package com.ruoyi.vqms.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.vqms.domain.VqmsArt26Curve;
import com.ruoyi.vqms.ingestion.Art26ReconcileService;
import com.ruoyi.vqms.ingestion.Art26ReconcileService.DayRow;
import com.ruoyi.vqms.ingestion.Art26ReconcileService.Reconcile;
import com.ruoyi.vqms.mapper.VqmsArt26CurveMapper;

/**
 * 第26条母线电压考核 Controller：季度曲线登记/导入 + 三桶对账 + 对账单导出。
 *
 * 权限统一 vqms:art26:*（查询/登记/导入/对账/导出）。
 */
@RestController
@RequestMapping("/vqms/art26")
public class VqmsArt26Controller extends BaseController {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private VqmsArt26CurveMapper curveMapper;

    @Autowired
    private Art26ReconcileService reconcileService;

    /** 曲线行列表（按母线+季度）。 */
    @PreAuthorize("@ss.hasPermi('vqms:art26:list')")
    @GetMapping("/list")
    public TableDataInfo list(VqmsArt26Curve q) {
        startPage();
        List<VqmsArt26Curve> rows = curveMapper.selectByBusbarQuarter(q);
        return getDataTable(rows);
    }

    @PreAuthorize("@ss.hasPermi('vqms:art26:list')")
    @GetMapping("/{curveId}")
    public AjaxResult getInfo(@PathVariable Long curveId) {
        return success(curveMapper.selectVqmsArt26CurveById(curveId));
    }

    @PreAuthorize("@ss.hasPermi('vqms:art26:add')")
    @Log(title = "第26条考核曲线", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody VqmsArt26Curve row) {
        validate(row);
        row.setCreateBy(SecurityUtils.getUsername());
        return toAjax(curveMapper.insertVqmsArt26Curve(row));
    }

    @PreAuthorize("@ss.hasPermi('vqms:art26:edit')")
    @Log(title = "第26条考核曲线", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody VqmsArt26Curve row) {
        validate(row);
        row.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(curveMapper.updateVqmsArt26Curve(row));
    }

    @PreAuthorize("@ss.hasPermi('vqms:art26:remove')")
    @Log(title = "第26条考核曲线", businessType = BusinessType.DELETE)
    @DeleteMapping("/{curveIds}")
    public AjaxResult remove(@PathVariable Long[] curveIds) {
        return toAjax(curveMapper.deleteVqmsArt26CurveByIds(curveIds));
    }

    /** CSV 批量导入曲线：每行 busbar_num,period_start,period_end,limit_up_kv,limit_down_kv
     *  （yyyy-MM-dd HH:mm:ss；quarter/source 由参数带入；首行可为表头自动跳过）。 */
    @PreAuthorize("@ss.hasPermi('vqms:art26:add')")
    @Log(title = "第26条考核曲线导入", businessType = BusinessType.IMPORT)
    @PostMapping("/import")
    public AjaxResult importCsv(@RequestParam String quarter, @RequestParam String source,
                                @RequestBody String csv) {
        if (csv == null || csv.isBlank()) {
            throw new ServiceException("导入内容为空");
        }
        List<VqmsArt26Curve> rows = new ArrayList<>();
        int line = 0;
        for (String ln : csv.split("\\r?\\n")) {
            line++;
            String s = ln.trim();
            if (s.isEmpty() || s.startsWith("#") || s.toLowerCase().startsWith("busbar_num")) {
                continue;
            }
            String[] p = s.split("[,，]");
            if (p.length < 5) {
                throw new ServiceException("第 " + line + " 行列数不足（需 5 列：busbar_num,起,止,上限,下限）: " + s);
            }
            try {
                VqmsArt26Curve c = new VqmsArt26Curve();
                c.setBusbarNum(Long.parseLong(p[0].trim()));
                c.setPeriodStart(java.sql.Timestamp.valueOf(LocalDateTime.parse(p[1].trim(), TS)));
                c.setPeriodEnd(java.sql.Timestamp.valueOf(LocalDateTime.parse(p[2].trim(), TS)));
                c.setLimitUpKv(new BigDecimal(p[3].trim()));
                c.setLimitDownKv(new BigDecimal(p[4].trim()));
                c.setQuarter(quarter);
                c.setSource(source);
                c.setCreateBy(SecurityUtils.getUsername());
                validate(c);
                rows.add(c);
            } catch (ServiceException e) {
                throw e;
            } catch (Exception e) {
                throw new ServiceException("第 " + line + " 行解析失败: " + s + "（" + e.getMessage() + "）");
            }
        }
        for (VqmsArt26Curve c : rows) {
            curveMapper.insertVqmsArt26Curve(c);
        }
        return success("导入 " + rows.size() + " 行");
    }

    /** 三桶对账：合格 / 不合格·AVC闭环(免考) / 不合格·AVC退出(待判) + 逐日明细。 */
    @PreAuthorize("@ss.hasPermi('vqms:art26:list')")
    @Log(title = "第26条对账查询", businessType = BusinessType.OTHER)
    @GetMapping("/reconcile")
    public AjaxResult reconcile(@RequestParam String quarter, @RequestParam long busbarNum) {
        return success(reconcileService.reconcile(quarter, busbarNum));
    }

    /** 对账单导出（Excel：汇总 + 逐日明细——对监管回复件）。 */
    @PreAuthorize("@ss.hasPermi('vqms:art26:export')")
    @Log(title = "第26条对账单导出", businessType = BusinessType.EXPORT)
    @PostMapping("/reconcile/export")
    public void reconcileExport(HttpServletResponse response, @RequestParam String quarter,
                                @RequestParam long busbarNum) {
        Reconcile r = reconcileService.reconcile(quarter, busbarNum);
        List<Art26DayVo> rows = new ArrayList<>();
        for (DayRow d : r.days()) {
            rows.add(new Art26DayVo(String.valueOf(d.date()), d.total(), d.qualified(),
                    d.exemptClosedLoop(), d.violationExitAvc(), d.noCurve(), d.noData()));
        }
        ExcelUtil<Art26DayVo> util = new ExcelUtil<>(Art26DayVo.class);
        util.exportExcel(response, rows, "第26条对账_" + quarter + "_母线" + busbarNum
                + "_(考核" + r.totalMinutes() + "分_合格" + r.qualified() + "_闭环免考" + r.exemptClosedLoop()
                + "_退出待判" + r.violationExitAvc() + ")");
    }

    private static void validate(VqmsArt26Curve c) {
        if (c.getBusbarNum() == null || c.getQuarter() == null || c.getPeriodStart() == null
                || c.getPeriodEnd() == null || c.getLimitUpKv() == null || c.getLimitDownKv() == null) {
            throw new ServiceException("母线/季度/时段/上下限均必填");
        }
        if (!c.getPeriodEnd().after(c.getPeriodStart())) {
            throw new ServiceException("时段止必须晚于时段起");
        }
        if (c.getLimitUpKv().compareTo(c.getLimitDownKv()) <= 0) {
            throw new ServiceException("考核上限必须大于下限");
        }
    }

    /** 对账逐日导出行。 */
    public static class Art26DayVo {
        @com.ruoyi.common.annotation.Excel(name = "日期")
        private String date;
        @com.ruoyi.common.annotation.Excel(name = "考核分钟")
        private Integer total;
        @com.ruoyi.common.annotation.Excel(name = "合格分钟")
        private Integer qualified;
        @com.ruoyi.common.annotation.Excel(name = "不合格·AVC闭环(免考)")
        private Integer exemptClosedLoop;
        @com.ruoyi.common.annotation.Excel(name = "不合格·AVC退出(待判)")
        private Integer violationExitAvc;
        @com.ruoyi.common.annotation.Excel(name = "无曲线覆盖")
        private Integer noCurve;
        @com.ruoyi.common.annotation.Excel(name = "无实测")
        private Integer noData;

        public Art26DayVo(String date, Integer total, Integer qualified, Integer exemptClosedLoop,
                          Integer violationExitAvc, Integer noCurve, Integer noData) {
            this.date = date;
            this.total = total;
            this.qualified = qualified;
            this.exemptClosedLoop = exemptClosedLoop;
            this.violationExitAvc = violationExitAvc;
            this.noCurve = noCurve;
            this.noData = noData;
        }

        public String getDate() { return date; }
        public Integer getTotal() { return total; }
        public Integer getQualified() { return qualified; }
        public Integer getExemptClosedLoop() { return exemptClosedLoop; }
        public Integer getViolationExitAvc() { return violationExitAvc; }
        public Integer getNoCurve() { return noCurve; }
        public Integer getNoData() { return noData; }
    }
}

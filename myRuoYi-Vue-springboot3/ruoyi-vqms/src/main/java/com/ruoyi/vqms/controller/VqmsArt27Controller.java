package com.ruoyi.vqms.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.ruoyi.vqms.domain.VqmsArt27Device;
import com.ruoyi.vqms.domain.VqmsArt27Month;
import com.ruoyi.vqms.mapper.VqmsArt27Mapper;
import com.ruoyi.vqms.statistics.Art27Calculator;
import com.ruoyi.vqms.statistics.Art27Calculator.MonthResult;

/**
 * 第27条动态无功补偿装置考核 Controller：装置台账 + 月度对账登记 + 可用率/罚分计算 + 对账单导出。
 *
 * 口径（辅助服务细则 p11）：可用率 = Σ自动可用小时/Σ升压变带电小时，99% 合格；
 * 缺额每 1pp 月考容量(万千瓦)×0.1 分；速率/铭牌每天 0.1 分；合计上限容量×5 分。
 * 风光储主体适用（火电不适用——清单2 §5 待确认）；过渡期 2025-04 起考核。
 * 数据现状：装置信号无源 → 月度数值人工登记（MANUAL），自动采集预留（AUTO + 台账点号列）。
 */
@RestController
@RequestMapping("/vqms/art27")
public class VqmsArt27Controller extends BaseController {

    @Autowired
    private VqmsArt27Mapper mapper;

    // ── 装置台账 ──

    @PreAuthorize("@ss.hasPermi('vqms:art27:list')")
    @GetMapping("/devices")
    public TableDataInfo devices(VqmsArt27Device q) {
        startPage();
        return getDataTable(mapper.selectDevices(q));
    }

    @PreAuthorize("@ss.hasPermi('vqms:art27:add')")
    @Log(title = "第27条装置台账", businessType = BusinessType.INSERT)
    @PostMapping("/device")
    public AjaxResult addDevice(@RequestBody VqmsArt27Device d) {
        validateDevice(d);
        d.setCreateBy(SecurityUtils.getUsername());
        return toAjax(mapper.insertDevice(d));
    }

    @PreAuthorize("@ss.hasPermi('vqms:art27:edit')")
    @Log(title = "第27条装置台账", businessType = BusinessType.UPDATE)
    @PutMapping("/device")
    public AjaxResult editDevice(@RequestBody VqmsArt27Device d) {
        validateDevice(d);
        d.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(mapper.updateDevice(d));
    }

    @PreAuthorize("@ss.hasPermi('vqms:art27:remove')")
    @Log(title = "第27条装置台账", businessType = BusinessType.DELETE)
    @DeleteMapping("/device/{deviceIds}")
    public AjaxResult delDevice(@PathVariable Long[] deviceIds) {
        return toAjax(mapper.deleteDeviceByIds(deviceIds));
    }

    // ── 月度登记 ──

    @PreAuthorize("@ss.hasPermi('vqms:art27:list')")
    @GetMapping("/months")
    public TableDataInfo months(@RequestParam String statMonth) {
        startPage();
        return getDataTable(mapper.selectMonths(statMonth));
    }

    @PreAuthorize("@ss.hasPermi('vqms:art27:add')")
    @Log(title = "第27条月度登记", businessType = BusinessType.INSERT)
    @PostMapping("/month")
    public AjaxResult addMonth(@RequestBody VqmsArt27Month m) {
        validateMonth(m);
        m.setCreateBy(SecurityUtils.getUsername());
        m.setSource("MANUAL");
        return toAjax(mapper.insertMonth(m));
    }

    @PreAuthorize("@ss.hasPermi('vqms:art27:edit')")
    @Log(title = "第27条月度登记", businessType = BusinessType.UPDATE)
    @PutMapping("/month")
    public AjaxResult editMonth(@RequestBody VqmsArt27Month m) {
        validateMonth(m);
        m.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(mapper.updateMonth(m));
    }

    @PreAuthorize("@ss.hasPermi('vqms:art27:remove')")
    @Log(title = "第27条月度登记", businessType = BusinessType.DELETE)
    @DeleteMapping("/month/{ids}")
    public AjaxResult delMonth(@PathVariable Long[] ids) {
        return toAjax(mapper.deleteMonthByIds(ids));
    }

    // ── 月度对账（计算 + 监管上报比对）──

    /** 对账视图：装置 × 月度登记 → 可用率/缺额/罚分/上限 + 监管通知单比对。 */
    @PreAuthorize("@ss.hasPermi('vqms:art27:list')")
    @Log(title = "第27条对账查询", businessType = BusinessType.OTHER)
    @GetMapping("/reconcile")
    public AjaxResult reconcile(@RequestParam String statMonth) {
        List<VqmsArt27Device> devices = mapper.selectDevices(new VqmsArt27Device());
        List<VqmsArt27Month> months = mapper.selectMonths(statMonth);
        Map<Long, VqmsArt27Month> byDevice = new HashMap<>();
        for (VqmsArt27Month m : months) {
            byDevice.put(m.getDeviceId(), m);
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        BigDecimal totalPenalty = BigDecimal.ZERO;
        BigDecimal totalRegulator = BigDecimal.ZERO;
        for (VqmsArt27Device d : devices) {
            if (!"0".equals(d.getStatus())) {
                continue;
            }
            Map<String, Object> row = new HashMap<>();
            row.put("deviceCode", d.getDeviceCode());
            row.put("deviceName", d.getDeviceName());
            row.put("ratedCapacityKw", d.getRatedCapacityKw());
            VqmsArt27Month m = byDevice.get(d.getDeviceId());
            if (m == null) {
                row.put("registered", false);
                rows.add(row);
                continue;
            }
            MonthResult r = Art27Calculator.month(m.getAutoHours(), m.getEnergizedHours(),
                    d.getRatedCapacityKw(),
                    m.getRatePenaltyDays() == null ? 0 : m.getRatePenaltyDays(),
                    m.getNameplateDays() == null ? 0 : m.getNameplateDays());
            row.put("registered", true);
            row.put("autoHours", m.getAutoHours());
            row.put("energizedHours", m.getEnergizedHours());
            row.put("ratePenaltyDays", m.getRatePenaltyDays());
            row.put("nameplateDays", m.getNameplateDays());
            row.put("availabilityPct", r.availabilityPct());
            row.put("shortfallPct", r.shortfallPct());
            row.put("availabilityPenalty", r.availabilityPenalty());
            row.put("daysPenalty", r.daysPenalty());
            row.put("totalPenalty", r.cappedPenalty());
            row.put("capped", r.capped());
            row.put("penaltyCny", r.cappedPenalty() == null ? null : r.cappedPenalty().multiply(BigDecimal.valueOf(1000)));
            row.put("regulatorRate", m.getRegulatorRate());
            row.put("regulatorPenalty", m.getRegulatorPenalty());
            if (r.cappedPenalty() != null) {
                totalPenalty = totalPenalty.add(r.cappedPenalty());
            }
            if (m.getRegulatorPenalty() != null) {
                totalRegulator = totalRegulator.add(m.getRegulatorPenalty());
            }
            rows.add(row);
        }
        Map<String, Object> out = new HashMap<>();
        out.put("statMonth", statMonth);
        out.put("rows", rows);
        out.put("totalPenalty", totalPenalty);
        out.put("totalRegulatorPenalty", totalRegulator);
        return success(out);
    }

    /** 对账单导出（Excel）。 */
    @PreAuthorize("@ss.hasPermi('vqms:art27:export')")
    @Log(title = "第27条对账单导出", businessType = BusinessType.EXPORT)
    @PostMapping("/reconcile/export")
    public void reconcileExport(HttpServletResponse response, @RequestParam String statMonth) {
        List<Art27Vo> rows = new ArrayList<>();
        for (Map<String, Object> r : resultMapOf(statMonth)) {
            rows.add(new Art27Vo(
                    str(r.get("deviceCode")), str(r.get("deviceName")),
                    dec(r.get("ratedCapacityKw")), dec(r.get("autoHours")), dec(r.get("energizedHours")),
                    intOf(r.get("ratePenaltyDays")), intOf(r.get("nameplateDays")),
                    dec(r.get("availabilityPct")), dec(r.get("shortfallPct")),
                    dec(r.get("availabilityPenalty")), dec(r.get("daysPenalty")),
                    dec(r.get("totalPenalty")), dec(r.get("penaltyCny")),
                    dec(r.get("regulatorRate")), dec(r.get("regulatorPenalty"))));
        }
        ExcelUtil<Art27Vo> util = new ExcelUtil<>(Art27Vo.class);
        util.exportExcel(response, rows, "第27条对账_" + statMonth);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> resultMapOf(String statMonth) {
        Object data = reconcile(statMonth).get("data");
        return data instanceof Map ? (List<Map<String, Object>>) ((Map<String, Object>) data).get("rows") : List.of();
    }

    private static String str(Object o) { return o == null ? "" : String.valueOf(o); }

    private static BigDecimal dec(Object o) {
        return o == null ? null : (o instanceof BigDecimal b ? b : new BigDecimal(String.valueOf(o)));
    }

    private static Integer intOf(Object o) { return o == null ? null : Integer.valueOf(String.valueOf(o)); }

    private static void validateDevice(VqmsArt27Device d) {
        if (d.getDeviceCode() == null || d.getDeviceName() == null || d.getDeviceType() == null
                || d.getRatedCapacityKw() == null) {
            throw new ServiceException("装置编号/名称/类型/额定容量均必填");
        }
        if (d.getRatedCapacityKw().signum() <= 0) {
            throw new ServiceException("额定容量必须大于 0");
        }
    }

    private static void validateMonth(VqmsArt27Month m) {
        if (m.getStatMonth() == null || !m.getStatMonth().matches("\\d{4}-\\d{2}")) {
            throw new ServiceException("统计月格式非法（yyyy-MM）");
        }
        if (m.getDeviceId() == null) {
            throw new ServiceException("装置必选");
        }
        if (m.getEnergizedHours() != null && m.getEnergizedHours().signum() < 0) {
            throw new ServiceException("带电小时不能为负");
        }
        if (m.getAutoHours() != null && m.getEnergizedHours() != null
                && m.getAutoHours().compareTo(m.getEnergizedHours()) > 0) {
            throw new ServiceException("可用小时不能大于带电小时");
        }
    }

    /** 对账导出行。 */
    public static class Art27Vo {
        @com.ruoyi.common.annotation.Excel(name = "装置编号")
        private String deviceCode;
        @com.ruoyi.common.annotation.Excel(name = "装置名称")
        private String deviceName;
        @com.ruoyi.common.annotation.Excel(name = "额定容量kW")
        private BigDecimal ratedCapacityKw;
        @com.ruoyi.common.annotation.Excel(name = "自动可用小时")
        private BigDecimal autoHours;
        @com.ruoyi.common.annotation.Excel(name = "升压变带电小时")
        private BigDecimal energizedHours;
        @com.ruoyi.common.annotation.Excel(name = "速率不符天数")
        private Integer ratePenaltyDays;
        @com.ruoyi.common.annotation.Excel(name = "铭牌不符天数")
        private Integer nameplateDays;
        @com.ruoyi.common.annotation.Excel(name = "可用率(%)")
        private BigDecimal availabilityPct;
        @com.ruoyi.common.annotation.Excel(name = "缺额(百分点)")
        private BigDecimal shortfallPct;
        @com.ruoyi.common.annotation.Excel(name = "可用率罚分")
        private BigDecimal availabilityPenalty;
        @com.ruoyi.common.annotation.Excel(name = "速率/铭牌罚分")
        private BigDecimal daysPenalty;
        @com.ruoyi.common.annotation.Excel(name = "合计罚分(封顶)")
        private BigDecimal totalPenalty;
        @com.ruoyi.common.annotation.Excel(name = "合计罚款(元)")
        private BigDecimal penaltyCny;
        @com.ruoyi.common.annotation.Excel(name = "监管上报可用率(%)")
        private BigDecimal regulatorRate;
        @com.ruoyi.common.annotation.Excel(name = "监管考核分")
        private BigDecimal regulatorPenalty;

        public Art27Vo(String deviceCode, String deviceName, BigDecimal ratedCapacityKw,
                       BigDecimal autoHours, BigDecimal energizedHours, Integer ratePenaltyDays,
                       Integer nameplateDays, BigDecimal availabilityPct, BigDecimal shortfallPct,
                       BigDecimal availabilityPenalty, BigDecimal daysPenalty, BigDecimal totalPenalty,
                       BigDecimal penaltyCny, BigDecimal regulatorRate, BigDecimal regulatorPenalty) {
            this.deviceCode = deviceCode;
            this.deviceName = deviceName;
            this.ratedCapacityKw = ratedCapacityKw;
            this.autoHours = autoHours;
            this.energizedHours = energizedHours;
            this.ratePenaltyDays = ratePenaltyDays;
            this.nameplateDays = nameplateDays;
            this.availabilityPct = availabilityPct;
            this.shortfallPct = shortfallPct;
            this.availabilityPenalty = availabilityPenalty;
            this.daysPenalty = daysPenalty;
            this.totalPenalty = totalPenalty;
            this.penaltyCny = penaltyCny;
            this.regulatorRate = regulatorRate;
            this.regulatorPenalty = regulatorPenalty;
        }

        public String getDeviceCode() { return deviceCode; }
        public String getDeviceName() { return deviceName; }
        public BigDecimal getRatedCapacityKw() { return ratedCapacityKw; }
        public BigDecimal getAutoHours() { return autoHours; }
        public BigDecimal getEnergizedHours() { return energizedHours; }
        public Integer getRatePenaltyDays() { return ratePenaltyDays; }
        public Integer getNameplateDays() { return nameplateDays; }
        public BigDecimal getAvailabilityPct() { return availabilityPct; }
        public BigDecimal getShortfallPct() { return shortfallPct; }
        public BigDecimal getAvailabilityPenalty() { return availabilityPenalty; }
        public BigDecimal getDaysPenalty() { return daysPenalty; }
        public BigDecimal getTotalPenalty() { return totalPenalty; }
        public BigDecimal getPenaltyCny() { return penaltyCny; }
        public BigDecimal getRegulatorRate() { return regulatorRate; }
        public BigDecimal getRegulatorPenalty() { return regulatorPenalty; }
    }
}

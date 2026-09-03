package com.ruoyi.vqms.controller;

import java.math.BigDecimal;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.vqms.domain.VqmsEntity;
import com.ruoyi.vqms.domain.VqmsRegulationStats;
import com.ruoyi.vqms.domain.VqmsRuntimeStats;
import com.ruoyi.vqms.domain.vo.VqmsRegulationReportVo;
import com.ruoyi.vqms.domain.vo.VqmsRuntimeReportVo;
import com.ruoyi.vqms.ingestion.RecomputeLock;
import com.ruoyi.vqms.ingestion.StatsRollupService;
import com.ruoyi.vqms.mapper.VqmsEntityMapper;
import com.ruoyi.vqms.mapper.VqmsRegulationCmdMapper;
import com.ruoyi.vqms.mapper.VqmsRegulationStatsMapper;
import com.ruoyi.vqms.mapper.VqmsRuntimeStatsMapper;
import com.ruoyi.vqms.statistics.RegulationStatsCalculator;
import com.ruoyi.vqms.statistics.RegulationStatsCalculator.RegulationRates;

/**
 * 考核统计查询 Controller：计数来自 rollup 表，率/罚款查询层按计数重算（铁律）。
 */
@RestController
@RequestMapping("/vqms/stats")
public class VqmsStatsController {

    @Autowired
    private StatsRollupService statsRollupService;

    @Autowired
    private RecomputeLock recomputeLock;

    @Autowired
    private VqmsRegulationStatsMapper regulationStatsMapper;

    @Autowired
    private VqmsRuntimeStatsMapper runtimeStatsMapper;

    @Autowired
    private VqmsRegulationCmdMapper regulationCmdMapper;

    @Autowired
    private VqmsEntityMapper entityMapper;

    /** 触发 rollup（日→月→年，幂等）。 */
    @PreAuthorize("@ss.hasPermi('vqms:judge:run')")
    @Log(title = "汇总重算", businessType = BusinessType.UPDATE)
    @PostMapping("/rollup")
    public AjaxResult rollup(@RequestParam("start") String start, @RequestParam("end") String end) {
        return AjaxResult.success(recomputeLock.guard(() -> statsRollupService.rollupByDateRange(
                LocalDate.parse(start), LocalDate.parse(end))));
    }

    /** 调节合格率报表（计数 + 率/缺额/罚分重算）。grain: D/M/Y。 */
    @PreAuthorize("@ss.hasPermi('vqms:judge:run')")
    @GetMapping("/regulation")
    public AjaxResult regulation(@RequestParam String grain,
                                 @RequestParam String start, @RequestParam String end) {
        List<VqmsRegulationStats> rows = regulationStatsMapper.selectByRange(
                grain, LocalDate.parse(start), LocalDate.parse(end));
        BigDecimal capacity = capacityOf();
        List<Map<String, Object>> out = new ArrayList<>(rows.size());
        for (VqmsRegulationStats r : rows) {
            Map<String, Object> m = new HashMap<>();
            m.put("statPeriod", r.getStatPeriod());
            m.put("totalCmds", r.getTotalCmds());
            m.put("counts", Map.of(
                    "qualifiedFast", r.getQualifiedFast(), "penalizedFast", r.getPenalizedFast(),
                    "exemptedFast", r.getExemptedFast(), "invalidFast", r.getInvalidFast(),
                    "qualifiedEcon", r.getQualifiedEcon(), "penalizedEcon", r.getPenalizedEcon(),
                    "exemptedEcon", r.getExemptedEcon(), "invalidEcon", r.getInvalidEcon(),
                    "undecodable", r.getUndecodableCount()));
            RegulationRates rates = RegulationStatsCalculator.compute(
                    r.getTotalCmds(), r.getQualifiedFast(), r.getExemptedFast(),
                    r.getQualifiedEcon(), r.getExemptedEcon(), capacity);
            m.put("fastRatePct", rates.fast().ratePct());
            m.put("fastPenaltyScore", rates.fast().penaltyScore());
            m.put("econRatePct", rates.econ().ratePct());
            m.put("econPenaltyScore", rates.econ().penaltyScore());
            m.put("exemptedTotal", rates.exemptedTotal());
            m.put("penaltyTotal", rates.penaltyTotal());
            // 罚款金额后端化（1 分 = 1000 元）：展示/导出统一由后端换算，前端不再 ×1000
            m.put("penaltyTotalCny", rates.penaltyTotal() == null ? null
                    : rates.penaltyTotal().multiply(BigDecimal.valueOf(1000)));
            out.add(m);
        }
        return AjaxResult.success(out);
    }

    /** 投运率报表（快照随行，月/年由 rollup 纯函数写回）。grain: D/M/Y。 */
    @PreAuthorize("@ss.hasPermi('vqms:judge:run')")
    @GetMapping("/runtime")
    public AjaxResult runtime(@RequestParam String grain,
                              @RequestParam String start, @RequestParam String end) {
        List<VqmsRuntimeStats> rows = runtimeStatsMapper.selectByRange(
                grain, LocalDate.parse(start), LocalDate.parse(end));
        return AjaxResult.success(rows);
    }

    /** 投运率报表导出（Excel；率/缺额/罚款为快照随行值，罚款元 = 分 × 1000）。 */
    @PreAuthorize("@ss.hasPermi('vqms:judge:run')")
    @Log(title = "投运率报表导出", businessType = BusinessType.EXPORT)
    @PostMapping("/runtime/export")
    public void runtimeExport(HttpServletResponse response, @RequestParam String grain,
                              @RequestParam String start, @RequestParam String end) {
        List<VqmsRuntimeStats> rows = runtimeStatsMapper.selectByRange(
                grain, LocalDate.parse(start), LocalDate.parse(end));
        List<VqmsRuntimeReportVo> out = new ArrayList<>(rows.size());
        for (VqmsRuntimeStats r : rows) {
            out.add(new VqmsRuntimeReportVo()
                    .statPeriod(String.valueOf(r.getStatPeriod()))
                    .entityId(r.getEntityId())
                    .gridMinutes(nvl(r.getInServiceMin()) + nvl(r.getExitGridMin()) + nvl(r.getExitNonGridMin()))
                    .inServiceMin(r.getInServiceMin())
                    .exitGridMin(r.getExitGridMin())
                    .exitNongridMin(r.getExitNonGridMin())
                    .ratePct(r.getRatePct())
                    .shortfallPct(r.getShortfallPct())
                    .penaltyScore(r.getPenaltyScore())
                    .penaltyCny(r.getPenaltyScore() == null ? null
                            : r.getPenaltyScore().multiply(BigDecimal.valueOf(1000))));
        }
        ExcelUtil<VqmsRuntimeReportVo> util = new ExcelUtil<>(VqmsRuntimeReportVo.class);
        util.exportExcel(response, out, "投运率报表_" + grain);
    }

    /** 调节合格率报表导出（Excel；率/罚款查询层重算，与 /regulation 接口同口径）。 */
    @PreAuthorize("@ss.hasPermi('vqms:judge:run')")
    @Log(title = "调节报表导出", businessType = BusinessType.EXPORT)
    @PostMapping("/regulation/export")
    public void regulationExport(HttpServletResponse response, @RequestParam String grain,
                                 @RequestParam String start, @RequestParam String end) {
        List<VqmsRegulationStats> rows = regulationStatsMapper.selectByRange(
                grain, LocalDate.parse(start), LocalDate.parse(end));
        BigDecimal capacity = capacityOf();
        List<VqmsRegulationReportVo> out = new ArrayList<>(rows.size());
        for (VqmsRegulationStats r : rows) {
            RegulationRates rates = RegulationStatsCalculator.compute(
                    r.getTotalCmds(), r.getQualifiedFast(), r.getExemptedFast(),
                    r.getQualifiedEcon(), r.getExemptedEcon(), capacity);
            out.add(new VqmsRegulationReportVo()
                    .statPeriod(String.valueOf(r.getStatPeriod()))
                    .entityId(r.getEntityId())
                    .totalCmds(r.getTotalCmds())
                    .qualifiedFast(r.getQualifiedFast())
                    .exemptedFast(r.getExemptedFast())
                    .invalidFast(r.getInvalidFast())
                    .fastRatePct(rates.fast().ratePct())
                    .fastPenaltyScore(rates.fast().penaltyScore())
                    .qualifiedEcon(r.getQualifiedEcon())
                    .exemptedEcon(r.getExemptedEcon())
                    .invalidEcon(r.getInvalidEcon())
                    .econRatePct(rates.econ().ratePct())
                    .econPenaltyScore(rates.econ().penaltyScore())
                    .penaltyTotal(rates.penaltyTotal())
                    .penaltyCny(rates.penaltyTotal() == null ? null
                            : rates.penaltyTotal().multiply(BigDecimal.valueOf(1000))));
        }
        ExcelUtil<VqmsRegulationReportVo> util = new ExcelUtil<>(VqmsRegulationReportVo.class);
        util.exportExcel(response, out, "调节合格率报表_" + grain);
    }

    private static int nvl(Integer v) {
        return v == null ? 0 : v;
    }

    /** 指令级明细（看板钻取，单日全量按 cmd_time 升序）。 */
    @PreAuthorize("@ss.hasPermi('vqms:judge:run')")
    @GetMapping("/commands")
    public AjaxResult commands(@RequestParam String start, @RequestParam String end) {
        return AjaxResult.success(regulationCmdMapper.selectByRange(
                LocalDate.parse(start).atStartOfDay(),
                LocalDate.parse(end).atTime(23, 59)));
    }

    private BigDecimal capacityOf() {
        VqmsEntity e = entityMapper.selectVqmsEntityById(1L);
        return e == null ? null : e.getRatedCapacityKw();
    }
}

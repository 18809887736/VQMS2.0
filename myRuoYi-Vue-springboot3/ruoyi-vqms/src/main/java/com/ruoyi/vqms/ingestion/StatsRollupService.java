package com.ruoyi.vqms.ingestion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vqms.domain.VqmsRuntimeStats;
import com.ruoyi.vqms.mapper.VqmsRegulationStatsMapper;
import com.ruoyi.vqms.mapper.VqmsRuntimeStatsMapper;
import com.ruoyi.vqms.statistics.RuntimeClassifier;
import com.ruoyi.vqms.statistics.RuntimeClassifier.DayStats;

/**
 * 统计 rollup 编排：调节 D→M→Y（纯 SQL 权威求和）+ 投运 M/Y（D 行求和→纯函数重算率写回）。
 *
 * 铁律：rollup 只对计数求和，绝不平均率列；率/罚款由查询层按计数重算
 * （投运侧快照列由 RuntimeClassifier.summarize 单一来源写回）。
 */
@Service
public class StatsRollupService {

    private static final Logger log = LoggerFactory.getLogger(StatsRollupService.class);

    @Autowired
    private VqmsRegulationStatsMapper regulationStatsMapper;

    @Autowired
    private VqmsRuntimeStatsMapper runtimeStatsMapper;

    /** 事务包裹：D→M→Y 全层原子落库，防半重算（写到一半失败时部分月/年行已落而日行残缺）。 */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> rollupByDateRange(LocalDate start, LocalDate end) {
        if (end.isBefore(start)) {
            throw new ServiceException("结束日不能早于起始日");
        }
        int days = regulationStatsMapper.rollupDay(start, end);

        Set<LocalDate> months = new HashSet<>();
        Set<LocalDate> years = new HashSet<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            months.add(d.withDayOfMonth(1));
            years.add(d.withDayOfYear(1));
        }
        int monthsRolled = 0;
        for (LocalDate m : months) {
            monthsRolled += regulationStatsMapper.rollupMonth(m);
            monthsRolled += rollupRuntimeMonth(m);
        }
        int yearsRolled = 0;
        for (LocalDate y : years) {
            yearsRolled += regulationStatsMapper.rollupYear(y);
            yearsRolled += rollupRuntimeYear(y);
        }
        log.info("rollup 完成 {}~{}: D={} M={} Y={}", start, end, days, monthsRolled, yearsRolled);
        return Map.of("start", start.toString(), "end", end.toString(),
                "regulationDayRows", days, "monthRows", monthsRolled, "yearRows", yearsRolled);
    }

    /** 投运月度：当月 D 行四桶求和 → 纯函数重算率/罚款 → 写 M 行（含快照）。 */
    private int rollupRuntimeMonth(LocalDate monthStart) {
        LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);
        List<VqmsRuntimeStats> dRows = runtimeStatsMapper.selectByRange("D", monthStart, monthEnd);
        if (dRows.isEmpty()) {
            return 0;
        }
        VqmsRuntimeStats m = aggregate(dRows, "M", monthStart);
        return runtimeStatsMapper.upsertBatch(List.of(m));
    }

    private int rollupRuntimeYear(LocalDate yearStart) {
        LocalDate yearEnd = yearStart.plusYears(1).minusDays(1);
        List<VqmsRuntimeStats> mRows = runtimeStatsMapper.selectByRange("M", yearStart, yearEnd);
        if (mRows.isEmpty()) {
            return 0;
        }
        VqmsRuntimeStats y = aggregate(mRows, "Y", yearStart);
        return runtimeStatsMapper.upsertBatch(List.of(y));
    }

    private VqmsRuntimeStats aggregate(List<VqmsRuntimeStats> rows, String grain, LocalDate period) {
        int inService = rows.stream().mapToInt(VqmsRuntimeStats::getInServiceMin).sum();
        int grid = rows.stream().mapToInt(VqmsRuntimeStats::getExitGridMin).sum();
        int nonGrid = rows.stream().mapToInt(VqmsRuntimeStats::getExitNonGridMin).sum();
        int offline = rows.stream().mapToInt(VqmsRuntimeStats::getOfflineMin).sum();
        BigDecimal capacity = rows.stream().map(VqmsRuntimeStats::getRatedCapacityKw)
                .filter(java.util.Objects::nonNull).findFirst().orElse(null);
        Long entityId = rows.get(0).getEntityId();
        DayStats stats = RuntimeClassifier.summarize(inService, grid, nonGrid, offline, capacity);

        // 多主体防护：当前 M/Y 汇总不按 entity 分组，混入多主体行会被静默合到首个主体——先告警暴露
        if (rows.stream().anyMatch(r -> !entityId.equals(r.getEntityId()))) {
            log.warn("汇总周期 {} 混入多个考核主体（{}）——当前 M/Y 汇总不分组，结果按主体 {} 记账，"
                    + "多主体上线前需按 entity_id 分维聚合", period,
                    rows.stream().map(VqmsRuntimeStats::getEntityId).distinct().toList(), entityId);
        }
        VqmsRuntimeStats out = new VqmsRuntimeStats();
        out.setStatGrain(grain);
        out.setStatPeriod(period);
        out.setEntityId(entityId);
        out.setInServiceMin(inService);
        out.setExitGridMin(grid);
        out.setExitNonGridMin(nonGrid);
        out.setOfflineMin(offline);
        out.setRatedCapacityKw(capacity);
        out.setRatePct(stats.ratePct());
        out.setShortfallPct(stats.shortfallPct());
        out.setPenaltyScore(stats.penaltyScore());
        return out;
    }
}

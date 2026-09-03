package com.ruoyi.vqms.ingestion;

import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ruoyi.vqms.mapper.VqmsRegulationCmdMapper;
import com.ruoyi.vqms.mapper.VqmsRuntimeStatsMapper;

/**
 * VQMS 统计定时任务（sys_job invokeTarget：vqmsStatsTask.recomputeYesterday()）。
 *
 * 全链 = 指令摄取 → 调节判定 → 投运记账 → 统计 rollup，全部幂等，可安全重跑。
 * 手动回补：调度 UI 调 vqmsStatsTask.recomputeRange('2026-02-27','2026-03-31')。
 *
 * 缺口补算（停机/misfire 自愈）：recomputeYesterday 不只算昨天——从两条链
 * （调节 cmd / 投运 D 行）最新已记账日的次日起补到昨天，漏跑几天补几天；
 * 单次补算跨度上限 {@value #MAX_CATCHUP_DAYS} 天，超限报错提示手动分批（防 Quartz 长任务失控）。
 */
@Component("vqmsStatsTask")
public class VqmsStatsTask {

    private static final Logger log = LoggerFactory.getLogger(VqmsStatsTask.class);

    /** 单次补算跨度上限（天）：停机超一个季度需手动分批回补。 */
    static final int MAX_CATCHUP_DAYS = 92;

    @Autowired
    private CommandIngestService commandIngestService;

    @Autowired
    private RegulationJudgeService regulationJudgeService;

    @Autowired
    private RuntimePipelineService runtimePipelineService;

    @Autowired
    private StatsRollupService statsRollupService;

    @Autowired
    private VqmsRegulationCmdMapper regulationCmdMapper;

    @Autowired
    private VqmsRuntimeStatsMapper runtimeStatsMapper;

    @Autowired
    private RecomputeLock recomputeLock;

    /** 每日无参入口（cron 03:00）：缺口补算到昨天。 */
    public void recomputeYesterday() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDate start = catchupStart(yesterday);
        if (start == null) {
            log.info("[VQMS] 昨日 {} 已记账，无需补算", yesterday);
            return;
        }
        recomputeRange(start.toString(), yesterday.toString());
    }

    /** 补算起点：两条链最新已记账日（取更早者）+1；无缺口返回 null。
     *  全新部署（无任何记账）从昨天起。 */
    private LocalDate catchupStart(LocalDate yesterday) {
        LocalDate maxCmd = regulationCmdMapper.selectMaxStatDate();
        LocalDate maxRuntime = runtimeStatsMapper.selectMaxStatPeriod();
        if (maxCmd == null && maxRuntime == null) {
            return yesterday;
        }
        LocalDate latest = minOrNull(maxCmd, maxRuntime);
        if (!latest.isBefore(yesterday)) {
            return null; // 两条链都已覆盖到昨天
        }
        LocalDate start = latest.plusDays(1);
        long gap = java.time.temporal.ChronoUnit.DAYS.between(latest, yesterday);
        if (gap > MAX_CATCHUP_DAYS) {
            log.error("[VQMS] 停机缺口 {} 天（{}~{}）超单次补算上限 {} 天：仅补最近一段，"
                    + "更早缺口请手动 vqmsStatsTask.recomputeRange 分批回补",
                    gap, start, yesterday, MAX_CATCHUP_DAYS);
            start = yesterday.minusDays(MAX_CATCHUP_DAYS - 1);
        } else if (gap > 1) {
            log.warn("[VQMS] 检测到停机缺口 {} 天：{}~{} 一次性补算", gap, start, yesterday);
        }
        return start;
    }

    private static LocalDate minOrNull(LocalDate a, LocalDate b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.isBefore(b) ? a : b;
    }

    /** 手动/回补入口：指定日期区间（含端点）。单飞互斥——与手动端点防撞车。 */
    public void recomputeRange(String start, String end) {
        LocalDate s = LocalDate.parse(start);
        LocalDate e = LocalDate.parse(end);
        recomputeLock.guard(() -> {
            log.info("[VQMS] 全链重算开始 {}~{}", s, e);
            commandIngestService.ingestByDateRange(s, e);
            regulationJudgeService.judgeByDateRange(s, e);
            runtimePipelineService.runtimeByDateRange(s, e);
            statsRollupService.rollupByDateRange(s, e);
            log.info("[VQMS] 全链重算完成 {}~{}", s, e);
            return null;
        });
    }
}

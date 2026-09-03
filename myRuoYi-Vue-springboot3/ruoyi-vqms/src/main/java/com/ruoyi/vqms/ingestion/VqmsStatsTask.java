package com.ruoyi.vqms.ingestion;

import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * VQMS 统计定时任务（sys_job invokeTarget：vqmsStatsTask.recomputeYesterday()）。
 *
 * 全链 = 指令摄取 → 调节判定 → 投运记账 → 统计 rollup，全部幂等，可安全重跑。
 * 手动回补：调度 UI 调 vqmsStatsTask.recomputeRange('2026-02-27','2026-03-31')。
 */
@Component("vqmsStatsTask")
public class VqmsStatsTask {

    private static final Logger log = LoggerFactory.getLogger(VqmsStatsTask.class);

    @Autowired
    private CommandIngestService commandIngestService;

    @Autowired
    private RegulationJudgeService regulationJudgeService;

    @Autowired
    private RuntimePipelineService runtimePipelineService;

    @Autowired
    private StatsRollupService statsRollupService;

    /** 每日无参入口：重算昨日全链（cron 03:00）。 */
    public void recomputeYesterday() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        recomputeRange(yesterday.toString(), yesterday.toString());
    }

    /** 手动/回补入口：指定日期区间（含端点）。 */
    public void recomputeRange(String start, String end) {
        LocalDate s = LocalDate.parse(start);
        LocalDate e = LocalDate.parse(end);
        log.info("[VQMS] 全链重算开始 {}~{}", s, e);
        commandIngestService.ingestByDateRange(s, e);
        regulationJudgeService.judgeByDateRange(s, e);
        runtimePipelineService.runtimeByDateRange(s, e);
        statsRollupService.rollupByDateRange(s, e);
        log.info("[VQMS] 全链重算完成 {}~{}", s, e);
    }
}

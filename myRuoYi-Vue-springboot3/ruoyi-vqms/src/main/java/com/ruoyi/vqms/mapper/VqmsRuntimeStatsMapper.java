package com.ruoyi.vqms.mapper;

import java.util.List;
import com.ruoyi.vqms.domain.VqmsRuntimeStats;

/**
 * 投运率记账 数据层（(stat_grain, stat_period, entity) uk 幂等 upsert）。
 */
public interface VqmsRuntimeStatsMapper {

    int upsertBatch(List<VqmsRuntimeStats> rows);

    List<VqmsRuntimeStats> selectByRange(String grain, java.time.LocalDate start, java.time.LocalDate end);

    /** 已记账的最新 D 粒度日期（缺口补算起点推断）。 */
    java.time.LocalDate selectMaxStatPeriod();
}

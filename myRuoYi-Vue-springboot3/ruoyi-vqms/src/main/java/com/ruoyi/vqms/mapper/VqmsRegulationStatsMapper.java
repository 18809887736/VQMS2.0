package com.ruoyi.vqms.mapper;

import java.time.LocalDate;
import java.util.List;
import com.ruoyi.vqms.domain.VqmsRegulationStats;

/**
 * 调节合格率汇总 数据层（rollup INSERT...SELECT...ON DUPLICATE 幂等；只对计数求和）。
 */
public interface VqmsRegulationStatsMapper {

    /** 日 rollup：vqms_regulation_cmd → 粒度 D。 */
    int rollupDay(LocalDate start, LocalDate end);

    /** 月 rollup：粒度 D → M（按日行求和）。 */
    int rollupMonth(LocalDate monthStart);

    /** 年 rollup：粒度 M → Y。 */
    int rollupYear(LocalDate yearStart);

    List<VqmsRegulationStats> selectByRange(String grain, LocalDate start, LocalDate end);
}

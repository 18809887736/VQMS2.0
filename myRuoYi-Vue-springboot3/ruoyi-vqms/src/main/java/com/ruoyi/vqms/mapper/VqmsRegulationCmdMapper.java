package com.ruoyi.vqms.mapper;

import java.util.List;
import com.ruoyi.vqms.domain.VqmsRegulationCmd;

/**
 * 调节合格率指令级明细 数据层（uk 幂等 upsert——重算覆盖）。
 */
public interface VqmsRegulationCmdMapper {

    /** 批量 upsert（ON DUPLICATE KEY UPDATE 判定列全量覆盖）；返回影响行数。 */
    int upsertBatch(List<VqmsRegulationCmd> rows);

    /** 时间范围内行数（验收）。 */
    long countByRange(java.time.LocalDateTime start, java.time.LocalDateTime end);

    /** 按指令原文时间取判定行（manifest 验收对账）。 */
    List<VqmsRegulationCmd> selectByRange(java.time.LocalDateTime start, java.time.LocalDateTime end);

    /** 已判定的最新指令日期（缺口补算起点推断）。 */
    java.time.LocalDate selectMaxStatDate();
}

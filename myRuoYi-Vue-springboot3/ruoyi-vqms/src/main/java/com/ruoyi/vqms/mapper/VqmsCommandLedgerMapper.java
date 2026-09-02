package com.ruoyi.vqms.mapper;

import java.util.List;
import com.ruoyi.vqms.domain.VqmsCommandLedger;

/**
 * 指令流水账 数据层（INSERT IGNORE 幂等，只增）。
 */
public interface VqmsCommandLedgerMapper {

    /** 批量幂等插入（uk 生成列归一拦截重复）；返回实际入库行数。 */
    int insertIgnoreBatch(List<VqmsCommandLedger> rows);

    /** 时间范围内行数（摄取校验）。 */
    long countByRange(java.time.LocalDateTime start, java.time.LocalDateTime end);
}

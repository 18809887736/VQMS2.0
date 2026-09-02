package com.ruoyi.vqms.mapper;

import com.ruoyi.vqms.domain.VqmsIngestLog;

/**
 * 摄取批次日志 数据层（uk (batch_no, source_table) INSERT IGNORE）。
 */
public interface VqmsIngestLogMapper {

    int insertIgnore(VqmsIngestLog log);
}

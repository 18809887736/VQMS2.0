package com.ruoyi.vqms.ingestion;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vqms.domain.VqmsCommandLedger;
import com.ruoyi.vqms.domain.VqmsIngestLog;
import com.ruoyi.vqms.mapper.VqmsCommandLedgerMapper;
import com.ruoyi.vqms.mapper.VqmsIngestLogMapper;
import com.ruoyi.vqms.source.SourceReader;
import com.ruoyi.vqms.source.SourceRows.WarnInfoRow;
import com.ruoyi.vqms.statistics.SaveTimeParser;
import com.ruoyi.vqms.statistics.VTargetDecoder;

/**
 * 指令摄取管线 v1：warn_info(type5) → 时间闸门 → ledger 幂等落库 → 摄取日志。
 *
 * 编排层只做装配：解析/解码全部走 statistics 纯函数；外部访问全部走 source Reader。
 * 增量形态指令（缺 t0 实时电压）落账原文但解码留空（判定阶段补）。
 */
@Service
public class CommandIngestService {

    private static final Logger log = LoggerFactory.getLogger(CommandIngestService.class);
    private static final DateTimeFormatter BATCH = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final int BATCH_SIZE = 500;

    @Autowired
    private SourceReader sourceReader;

    @Autowired
    private VqmsCommandLedgerMapper ledgerMapper;

    @Autowired
    private VqmsIngestLogMapper ingestLogMapper;

    /**
     * 按日期区间（含端点，整天）抓取指令入账。
     *
     * @return 摄取日志（含 rows_read / accepted / dup 统计）
     */
    public VqmsIngestLog ingestByDateRange(LocalDate start, LocalDate end) {
        if (end.isBefore(start)) {
            throw new ServiceException("结束日不能早于起始日");
        }
        LocalDateTime startDt = start.atStartOfDay();
        LocalDateTime endDt = end.atTime(23, 59);
        LocalDateTime beganAt = LocalDateTime.now();

        List<WarnInfoRow> rows = sourceReader.fetchCommands(startDt, endDt);

        int dirtyTime = 0;
        int decodedOk = 0;
        int undecodable = 0;
        List<VqmsCommandLedger> ledgerRows = new ArrayList<>(rows.size());
        for (WarnInfoRow r : rows) {
            // rows 已过时间闸门；此处二次校验防御
            LocalDateTime cmdTime = SaveTimeParser.parseToMinute(r.warnTimeRaw());
            if (cmdTime == null) {
                dirtyTime++;
                continue;
            }
            VqmsCommandLedger l = new VqmsCommandLedger();
            l.setWarnTimeRaw(r.warnTimeRaw());
            l.setCmdTime(cmdTime);
            l.setMillisecond(r.millisecond());
            l.setWarnType((int) r.warnType());
            l.setObjNum(r.objNum());
            l.setWarnContent(r.warnInfo());
            if (VTargetDecoder.decodeAny(r.warnInfo(), null) != null) {
                decodedOk++;
            } else {
                undecodable++;
            }
            ledgerRows.add(l);
        }

        int accepted = 0;
        for (int i = 0; i < ledgerRows.size(); i += BATCH_SIZE) {
            accepted += ledgerMapper.insertIgnoreBatch(ledgerRows.subList(i, Math.min(i + BATCH_SIZE, ledgerRows.size())));
        }
        int dup = ledgerRows.size() - accepted;

        VqmsIngestLog il = new VqmsIngestLog();
        il.setBatchNo(LocalDateTime.now().format(BATCH) + "-CMD");
        il.setSourceTable("warn_info");
        il.setRangeStart(startDt);
        il.setRangeEnd(endDt);
        il.setRowsRead(rows.size());
        il.setRowsAccepted(accepted);
        il.setRowsSkippedDirty(dirtyTime);
        il.setRowsSkippedDup(dup);
        il.setSkipDetail(String.format("解码成功=%d, 解码失败(增量缺t0或脏值)=%d, 时间非法=%d", decodedOk, undecodable, dirtyTime));
        il.setStatus("0");
        il.setStartedAt(beganAt);
        il.setFinishedAt(LocalDateTime.now());
        ingestLogMapper.insertIgnore(il);
        log.info("指令摄取完成: {}~{} read={} accepted={} dup={} dirty={}", start, end, rows.size(), accepted, dup, dirtyTime);
        return il;
    }
}

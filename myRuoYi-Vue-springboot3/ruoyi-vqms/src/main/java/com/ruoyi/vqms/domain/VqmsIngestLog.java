package com.ruoyi.vqms.domain;

import java.time.LocalDateTime;

/**
 * 摄取批次日志 vqms_ingest_log（数据质量闸门留痕）。
 */
public class VqmsIngestLog {

    private Long id;
    private String batchNo;
    private String sourceTable;
    private LocalDateTime rangeStart;
    private LocalDateTime rangeEnd;
    private Integer rowsRead;
    private Integer rowsAccepted;
    private Integer rowsSkippedDirty;
    private Integer rowsSkippedDup;
    private String skipDetail;
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private String remark;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public String getSourceTable() { return sourceTable; }
    public void setSourceTable(String sourceTable) { this.sourceTable = sourceTable; }
    public LocalDateTime getRangeStart() { return rangeStart; }
    public void setRangeStart(LocalDateTime rangeStart) { this.rangeStart = rangeStart; }
    public LocalDateTime getRangeEnd() { return rangeEnd; }
    public void setRangeEnd(LocalDateTime rangeEnd) { this.rangeEnd = rangeEnd; }
    public Integer getRowsRead() { return rowsRead; }
    public void setRowsRead(Integer rowsRead) { this.rowsRead = rowsRead; }
    public Integer getRowsAccepted() { return rowsAccepted; }
    public void setRowsAccepted(Integer rowsAccepted) { this.rowsAccepted = rowsAccepted; }
    public Integer getRowsSkippedDirty() { return rowsSkippedDirty; }
    public void setRowsSkippedDirty(Integer rowsSkippedDirty) { this.rowsSkippedDirty = rowsSkippedDirty; }
    public Integer getRowsSkippedDup() { return rowsSkippedDup; }
    public void setRowsSkippedDup(Integer rowsSkippedDup) { this.rowsSkippedDup = rowsSkippedDup; }
    public String getSkipDetail() { return skipDetail; }
    public void setSkipDetail(String skipDetail) { this.skipDetail = skipDetail; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}

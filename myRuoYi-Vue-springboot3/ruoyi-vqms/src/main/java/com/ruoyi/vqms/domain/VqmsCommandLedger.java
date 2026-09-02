package com.ruoyi.vqms.domain;

import java.time.LocalDateTime;
import java.math.BigDecimal;

/**
 * AVC 指令流水账 vqms_command_ledger（原始事实，只增）。
 * uk 生成列（millisecond_uk/obj_num_uk）应用不读写。
 */
public class VqmsCommandLedger {

    private Long id;
    private String warnTimeRaw;
    private LocalDateTime cmdTime;
    private String millisecond;
    private Integer warnType;
    private Long objNum;
    private String warnContent;
    private LocalDateTime fetchedAt;

    /** 解码审计（不入库——判定明细表落；此处供摄取日志统计） */
    private transient BigDecimal decodedTargetKv;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getWarnTimeRaw() { return warnTimeRaw; }
    public void setWarnTimeRaw(String warnTimeRaw) { this.warnTimeRaw = warnTimeRaw; }
    public LocalDateTime getCmdTime() { return cmdTime; }
    public void setCmdTime(LocalDateTime cmdTime) { this.cmdTime = cmdTime; }
    public String getMillisecond() { return millisecond; }
    public void setMillisecond(String millisecond) { this.millisecond = millisecond; }
    public Integer getWarnType() { return warnType; }
    public void setWarnType(Integer warnType) { this.warnType = warnType; }
    public Long getObjNum() { return objNum; }
    public void setObjNum(Long objNum) { this.objNum = objNum; }
    public String getWarnContent() { return warnContent; }
    public void setWarnContent(String warnContent) { this.warnContent = warnContent; }
    public LocalDateTime getFetchedAt() { return fetchedAt; }
    public void setFetchedAt(LocalDateTime fetchedAt) { this.fetchedAt = fetchedAt; }
    public BigDecimal getDecodedTargetKv() { return decodedTargetKv; }
    public void setDecodedTargetKv(BigDecimal decodedTargetKv) { this.decodedTargetKv = decodedTargetKv; }
}

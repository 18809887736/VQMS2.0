package com.ruoyi.vqms.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 调节合格率指令级明细 vqms_regulation_cmd（判定+免考+策略处置结果）。
 * uk 生成列（millisecond_uk/obj_num_uk）应用不读写。
 */
public class VqmsRegulationCmd {

    private Long id;
    private LocalDate statDate;
    private Long entityId;
    private Long groupNum;
    private String warnTimeRaw;
    private String millisecond;
    private Long objNum;
    private LocalDateTime cmdTime;
    private String algorithmId;
    private String decodeAlgorithm;
    private BigDecimal targetKv;
    private Integer responseMinutes;
    private Integer tFastSnapshot;
    private String fastState;
    private String econState;
    private BigDecimal completeness;
    private String invalidTiers;
    private String undecodableReason;
    private String exemptSource;
    private Long exemptRefId;
    private String disposition;
    private String hitRuleId;
    private LocalDateTime fetchedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDate getStatDate() { return statDate; }
    public void setStatDate(LocalDate statDate) { this.statDate = statDate; }
    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }
    public Long getGroupNum() { return groupNum; }
    public void setGroupNum(Long groupNum) { this.groupNum = groupNum; }
    public String getWarnTimeRaw() { return warnTimeRaw; }
    public void setWarnTimeRaw(String warnTimeRaw) { this.warnTimeRaw = warnTimeRaw; }
    public String getMillisecond() { return millisecond; }
    public void setMillisecond(String millisecond) { this.millisecond = millisecond; }
    public Long getObjNum() { return objNum; }
    public void setObjNum(Long objNum) { this.objNum = objNum; }
    public LocalDateTime getCmdTime() { return cmdTime; }
    public void setCmdTime(LocalDateTime cmdTime) { this.cmdTime = cmdTime; }
    public String getAlgorithmId() { return algorithmId; }
    public void setAlgorithmId(String algorithmId) { this.algorithmId = algorithmId; }
    public String getDecodeAlgorithm() { return decodeAlgorithm; }
    public void setDecodeAlgorithm(String decodeAlgorithm) { this.decodeAlgorithm = decodeAlgorithm; }
    public BigDecimal getTargetKv() { return targetKv; }
    public void setTargetKv(BigDecimal targetKv) { this.targetKv = targetKv; }
    public Integer getResponseMinutes() { return responseMinutes; }
    public void setResponseMinutes(Integer responseMinutes) { this.responseMinutes = responseMinutes; }
    public Integer getTFastSnapshot() { return tFastSnapshot; }
    public void setTFastSnapshot(Integer tFastSnapshot) { this.tFastSnapshot = tFastSnapshot; }
    public String getFastState() { return fastState; }
    public void setFastState(String fastState) { this.fastState = fastState; }
    public String getEconState() { return econState; }
    public void setEconState(String econState) { this.econState = econState; }
    public BigDecimal getCompleteness() { return completeness; }
    public void setCompleteness(BigDecimal completeness) { this.completeness = completeness; }
    public String getInvalidTiers() { return invalidTiers; }
    public void setInvalidTiers(String invalidTiers) { this.invalidTiers = invalidTiers; }
    public String getUndecodableReason() { return undecodableReason; }
    public void setUndecodableReason(String undecodableReason) { this.undecodableReason = undecodableReason; }
    public String getExemptSource() { return exemptSource; }
    public void setExemptSource(String exemptSource) { this.exemptSource = exemptSource; }
    public Long getExemptRefId() { return exemptRefId; }
    public void setExemptRefId(Long exemptRefId) { this.exemptRefId = exemptRefId; }
    public String getDisposition() { return disposition; }
    public void setDisposition(String disposition) { this.disposition = disposition; }
    public String getHitRuleId() { return hitRuleId; }
    public void setHitRuleId(String hitRuleId) { this.hitRuleId = hitRuleId; }
    public LocalDateTime getFetchedAt() { return fetchedAt; }
    public void setFetchedAt(LocalDateTime fetchedAt) { this.fetchedAt = fetchedAt; }
}

package com.ruoyi.vqms.domain;

import java.math.BigDecimal;
import com.ruoyi.common.core.domain.BaseEntity;

/** 第27条月度对账登记 vqms_art27_month。 */
public class VqmsArt27Month extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String statMonth;
    private Long deviceId;
    private BigDecimal autoHours;
    private BigDecimal energizedHours;
    private Integer ratePenaltyDays;
    private Integer nameplateDays;
    private BigDecimal regulatorRate;
    private BigDecimal regulatorPenalty;
    private String source;

    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
    public String getStatMonth() { return statMonth; }
    public void setStatMonth(String v) { this.statMonth = v; }
    public Long getDeviceId() { return deviceId; }
    public void setDeviceId(Long v) { this.deviceId = v; }
    public BigDecimal getAutoHours() { return autoHours; }
    public void setAutoHours(BigDecimal v) { this.autoHours = v; }
    public BigDecimal getEnergizedHours() { return energizedHours; }
    public void setEnergizedHours(BigDecimal v) { this.energizedHours = v; }
    public Integer getRatePenaltyDays() { return ratePenaltyDays; }
    public void setRatePenaltyDays(Integer v) { this.ratePenaltyDays = v; }
    public Integer getNameplateDays() { return nameplateDays; }
    public void setNameplateDays(Integer v) { this.nameplateDays = v; }
    public BigDecimal getRegulatorRate() { return regulatorRate; }
    public void setRegulatorRate(BigDecimal v) { this.regulatorRate = v; }
    public BigDecimal getRegulatorPenalty() { return regulatorPenalty; }
    public void setRegulatorPenalty(BigDecimal v) { this.regulatorPenalty = v; }
    public String getSource() { return source; }
    public void setSource(String v) { this.source = v; }
}

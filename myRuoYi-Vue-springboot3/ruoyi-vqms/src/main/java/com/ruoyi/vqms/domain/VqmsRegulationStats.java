package com.ruoyi.vqms.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 调节合格率汇总 vqms_regulation_stats（三粒度合一；rollup 只存计数，率/罚款查询层重算）。
 */
public class VqmsRegulationStats {

    private Long id;
    private String statGrain;
    private LocalDate statPeriod;
    private Long entityId;
    private String algorithmId;
    private Integer totalCmds;
    private Integer qualifiedFast;
    private Integer penalizedFast;
    private Integer exemptedFast;
    private Integer invalidFast;
    private Integer qualifiedEcon;
    private Integer penalizedEcon;
    private Integer exemptedEcon;
    private Integer invalidEcon;
    private Integer undecodableCount;
    private Integer pendedCount;
    private Integer excludedCount;
    private BigDecimal completenessSum;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getStatGrain() { return statGrain; }
    public void setStatGrain(String statGrain) { this.statGrain = statGrain; }
    public LocalDate getStatPeriod() { return statPeriod; }
    public void setStatPeriod(LocalDate statPeriod) { this.statPeriod = statPeriod; }
    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }
    public String getAlgorithmId() { return algorithmId; }
    public void setAlgorithmId(String algorithmId) { this.algorithmId = algorithmId; }
    public Integer getTotalCmds() { return totalCmds; }
    public void setTotalCmds(Integer totalCmds) { this.totalCmds = totalCmds; }
    public Integer getQualifiedFast() { return qualifiedFast; }
    public void setQualifiedFast(Integer qualifiedFast) { this.qualifiedFast = qualifiedFast; }
    public Integer getPenalizedFast() { return penalizedFast; }
    public void setPenalizedFast(Integer penalizedFast) { this.penalizedFast = penalizedFast; }
    public Integer getExemptedFast() { return exemptedFast; }
    public void setExemptedFast(Integer exemptedFast) { this.exemptedFast = exemptedFast; }
    public Integer getInvalidFast() { return invalidFast; }
    public void setInvalidFast(Integer invalidFast) { this.invalidFast = invalidFast; }
    public Integer getQualifiedEcon() { return qualifiedEcon; }
    public void setQualifiedEcon(Integer qualifiedEcon) { this.qualifiedEcon = qualifiedEcon; }
    public Integer getPenalizedEcon() { return penalizedEcon; }
    public void setPenalizedEcon(Integer penalizedEcon) { this.penalizedEcon = penalizedEcon; }
    public Integer getExemptedEcon() { return exemptedEcon; }
    public void setExemptedEcon(Integer exemptedEcon) { this.exemptedEcon = exemptedEcon; }
    public Integer getInvalidEcon() { return invalidEcon; }
    public void setInvalidEcon(Integer invalidEcon) { this.invalidEcon = invalidEcon; }
    public Integer getUndecodableCount() { return undecodableCount; }
    public void setUndecodableCount(Integer undecodableCount) { this.undecodableCount = undecodableCount; }
    public Integer getPendedCount() { return pendedCount; }
    public void setPendedCount(Integer pendedCount) { this.pendedCount = pendedCount; }
    public Integer getExcludedCount() { return excludedCount; }
    public void setExcludedCount(Integer excludedCount) { this.excludedCount = excludedCount; }
    public BigDecimal getCompletenessSum() { return completenessSum; }
    public void setCompletenessSum(BigDecimal completenessSum) { this.completenessSum = completenessSum; }
}

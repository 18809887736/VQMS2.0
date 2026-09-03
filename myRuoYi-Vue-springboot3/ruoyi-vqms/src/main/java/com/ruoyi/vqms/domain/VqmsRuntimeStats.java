package com.ruoyi.vqms.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 投运率记账 vqms_runtime_stats（四桶分钟计数；率/罚款为纯函数写回的快照）。
 */
public class VqmsRuntimeStats {

    private Long id;
    private String statGrain;
    private LocalDate statPeriod;
    private Long entityId;
    private Integer inServiceMin;
    private Integer exitGridMin;
    private Integer exitNonGridMin;
    private Integer offlineMin;
    private BigDecimal ratedCapacityKw;
    private BigDecimal ratePct;
    private BigDecimal shortfallPct;
    private BigDecimal penaltyScore;
    private LocalDateTime recomputeAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getStatGrain() { return statGrain; }
    public void setStatGrain(String statGrain) { this.statGrain = statGrain; }
    public LocalDate getStatPeriod() { return statPeriod; }
    public void setStatPeriod(LocalDate statPeriod) { this.statPeriod = statPeriod; }
    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }
    public Integer getInServiceMin() { return inServiceMin; }
    public void setInServiceMin(Integer inServiceMin) { this.inServiceMin = inServiceMin; }
    public Integer getExitGridMin() { return exitGridMin; }
    public void setExitGridMin(Integer exitGridMin) { this.exitGridMin = exitGridMin; }
    public Integer getExitNonGridMin() { return exitNonGridMin; }
    public void setExitNonGridMin(Integer exitNonGridMin) { this.exitNonGridMin = exitNonGridMin; }
    public Integer getOfflineMin() { return offlineMin; }
    public void setOfflineMin(Integer offlineMin) { this.offlineMin = offlineMin; }
    public BigDecimal getRatedCapacityKw() { return ratedCapacityKw; }
    public void setRatedCapacityKw(BigDecimal ratedCapacityKw) { this.ratedCapacityKw = ratedCapacityKw; }
    public BigDecimal getRatePct() { return ratePct; }
    public void setRatePct(BigDecimal ratePct) { this.ratePct = ratePct; }
    public BigDecimal getShortfallPct() { return shortfallPct; }
    public void setShortfallPct(BigDecimal shortfallPct) { this.shortfallPct = shortfallPct; }
    public BigDecimal getPenaltyScore() { return penaltyScore; }
    public void setPenaltyScore(BigDecimal penaltyScore) { this.penaltyScore = penaltyScore; }
    /** 罚款金额（元，1 分 = 1000 元）——查询层派生列，无 DB 存储。 */
    public BigDecimal getPenaltyScoreCny() {
        return penaltyScore == null ? null : penaltyScore.multiply(java.math.BigDecimal.valueOf(1000));
    }
    public LocalDateTime getRecomputeAt() { return recomputeAt; }
    public void setRecomputeAt(LocalDateTime recomputeAt) { this.recomputeAt = recomputeAt; }
}

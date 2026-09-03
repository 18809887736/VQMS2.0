package com.ruoyi.vqms.domain.vo;

import java.math.BigDecimal;

import com.ruoyi.common.annotation.Excel;

/**
 * 调节合格率报表导出行（Excel）。率/罚款为查询层重算（DB 只落计数），1 分 = 1000 元。
 */
public class VqmsRegulationReportVo {

    @Excel(name = "统计期间")
    private String statPeriod;

    @Excel(name = "考核主体")
    private Long entityId;

    @Excel(name = "发令次数")
    private Integer totalCmds;

    @Excel(name = "快速性·合格")
    private Integer qualifiedFast;

    @Excel(name = "快速性·免考")
    private Integer exemptedFast;

    @Excel(name = "快速性·无效")
    private Integer invalidFast;

    @Excel(name = "快速性合格率(%)")
    private BigDecimal fastRatePct;

    @Excel(name = "快速性罚款(分)")
    private BigDecimal fastPenaltyScore;

    @Excel(name = "经济性·合格")
    private Integer qualifiedEcon;

    @Excel(name = "经济性·免考")
    private Integer exemptedEcon;

    @Excel(name = "经济性·无效")
    private Integer invalidEcon;

    @Excel(name = "经济性合格率(%)")
    private BigDecimal econRatePct;

    @Excel(name = "经济性罚款(分)")
    private BigDecimal econPenaltyScore;

    @Excel(name = "总罚款(分)")
    private BigDecimal penaltyTotal;

    @Excel(name = "总罚款(元)")
    private BigDecimal penaltyCny;

    public VqmsRegulationReportVo statPeriod(String v) { this.statPeriod = v; return this; }
    public VqmsRegulationReportVo entityId(Long v) { this.entityId = v; return this; }
    public VqmsRegulationReportVo totalCmds(Integer v) { this.totalCmds = v; return this; }
    public VqmsRegulationReportVo qualifiedFast(Integer v) { this.qualifiedFast = v; return this; }
    public VqmsRegulationReportVo exemptedFast(Integer v) { this.exemptedFast = v; return this; }
    public VqmsRegulationReportVo invalidFast(Integer v) { this.invalidFast = v; return this; }
    public VqmsRegulationReportVo fastRatePct(BigDecimal v) { this.fastRatePct = v; return this; }
    public VqmsRegulationReportVo fastPenaltyScore(BigDecimal v) { this.fastPenaltyScore = v; return this; }
    public VqmsRegulationReportVo qualifiedEcon(Integer v) { this.qualifiedEcon = v; return this; }
    public VqmsRegulationReportVo exemptedEcon(Integer v) { this.exemptedEcon = v; return this; }
    public VqmsRegulationReportVo invalidEcon(Integer v) { this.invalidEcon = v; return this; }
    public VqmsRegulationReportVo econRatePct(BigDecimal v) { this.econRatePct = v; return this; }
    public VqmsRegulationReportVo econPenaltyScore(BigDecimal v) { this.econPenaltyScore = v; return this; }
    public VqmsRegulationReportVo penaltyTotal(BigDecimal v) { this.penaltyTotal = v; return this; }
    public VqmsRegulationReportVo penaltyCny(BigDecimal v) { this.penaltyCny = v; return this; }

    public String getStatPeriod() { return statPeriod; }
    public Long getEntityId() { return entityId; }
    public Integer getTotalCmds() { return totalCmds; }
    public Integer getQualifiedFast() { return qualifiedFast; }
    public Integer getExemptedFast() { return exemptedFast; }
    public Integer getInvalidFast() { return invalidFast; }
    public BigDecimal getFastRatePct() { return fastRatePct; }
    public BigDecimal getFastPenaltyScore() { return fastPenaltyScore; }
    public Integer getQualifiedEcon() { return qualifiedEcon; }
    public Integer getExemptedEcon() { return exemptedEcon; }
    public Integer getInvalidEcon() { return invalidEcon; }
    public BigDecimal getEconRatePct() { return econRatePct; }
    public BigDecimal getEconPenaltyScore() { return econPenaltyScore; }
    public BigDecimal getPenaltyTotal() { return penaltyTotal; }
    public BigDecimal getPenaltyCny() { return penaltyCny; }
}

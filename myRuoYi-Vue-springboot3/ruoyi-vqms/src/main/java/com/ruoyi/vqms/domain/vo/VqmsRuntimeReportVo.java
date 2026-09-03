package com.ruoyi.vqms.domain.vo;

import java.math.BigDecimal;

import com.ruoyi.common.annotation.Excel;

/**
 * 投运率报表导出行（Excel）。
 * 并网运行分钟 = 投运 + 电网退出 + 非电网退出（offline 不在分母——缺数≠离线口径在 D 级已处理）。
 */
public class VqmsRuntimeReportVo {

    @Excel(name = "统计期间")
    private String statPeriod;

    @Excel(name = "考核主体")
    private Long entityId;

    @Excel(name = "并网运行(分)")
    private Integer gridMinutes;

    @Excel(name = "投运(分)")
    private Integer inServiceMin;

    @Excel(name = "电网退出(分)")
    private Integer exitGridMin;

    @Excel(name = "非电网退出(分)")
    private Integer exitNongridMin;

    @Excel(name = "投运率(%)")
    private BigDecimal ratePct;

    @Excel(name = "缺额(百分点)")
    private BigDecimal shortfallPct;

    @Excel(name = "罚款(分)")
    private BigDecimal penaltyScore;

    @Excel(name = "罚款(元)")
    private BigDecimal penaltyCny;

    public VqmsRuntimeReportVo statPeriod(String v) { this.statPeriod = v; return this; }
    public VqmsRuntimeReportVo entityId(Long v) { this.entityId = v; return this; }
    public VqmsRuntimeReportVo gridMinutes(Integer v) { this.gridMinutes = v; return this; }
    public VqmsRuntimeReportVo inServiceMin(Integer v) { this.inServiceMin = v; return this; }
    public VqmsRuntimeReportVo exitGridMin(Integer v) { this.exitGridMin = v; return this; }
    public VqmsRuntimeReportVo exitNongridMin(Integer v) { this.exitNongridMin = v; return this; }
    public VqmsRuntimeReportVo ratePct(BigDecimal v) { this.ratePct = v; return this; }
    public VqmsRuntimeReportVo shortfallPct(BigDecimal v) { this.shortfallPct = v; return this; }
    public VqmsRuntimeReportVo penaltyScore(BigDecimal v) { this.penaltyScore = v; return this; }
    public VqmsRuntimeReportVo penaltyCny(BigDecimal v) { this.penaltyCny = v; return this; }

    public String getStatPeriod() { return statPeriod; }
    public Long getEntityId() { return entityId; }
    public Integer getGridMinutes() { return gridMinutes; }
    public Integer getInServiceMin() { return inServiceMin; }
    public Integer getExitGridMin() { return exitGridMin; }
    public Integer getExitNongridMin() { return exitNongridMin; }
    public BigDecimal getRatePct() { return ratePct; }
    public BigDecimal getShortfallPct() { return shortfallPct; }
    public BigDecimal getPenaltyScore() { return penaltyScore; }
    public BigDecimal getPenaltyCny() { return penaltyCny; }
}

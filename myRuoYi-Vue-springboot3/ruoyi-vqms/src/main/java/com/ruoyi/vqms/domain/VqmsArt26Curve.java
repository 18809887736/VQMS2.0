package com.ruoyi.vqms.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 第26条季度母线电压考核曲线对象 vqms_art26_curve
 *
 * @author ruoyi
 * @date 2026-09-04
 */
public class VqmsArt26Curve extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long curveId;

    /** 考核母线（对齐 his_curve_sv.busbar_num） */
    @Excel(name = "考核母线")
    private Long busbarNum;

    /** 季度标签（如 2026Q1） */
    @Excel(name = "季度标签")
    private String quarter;

    /** 时段起（含） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "时段起", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date periodStart;

    /** 时段止（含） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "时段止", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date periodEnd;

    /** 考核上限 kV */
    @Excel(name = "考核上限kV")
    private BigDecimal limitUpKv;

    /** 考核下限 kV */
    @Excel(name = "考核下限kV")
    private BigDecimal limitDownKv;

    /** 下发来源（文件名/通知单号） */
    private String source;

    public void setCurveId(Long curveId)
    {
        this.curveId = curveId;
    }

    public Long getCurveId()
    {
        return curveId;
    }

    public void setBusbarNum(Long busbarNum)
    {
        this.busbarNum = busbarNum;
    }

    public Long getBusbarNum()
    {
        return busbarNum;
    }

    public void setQuarter(String quarter)
    {
        this.quarter = quarter;
    }

    public String getQuarter()
    {
        return quarter;
    }

    public void setPeriodStart(Date periodStart)
    {
        this.periodStart = periodStart;
    }

    public Date getPeriodStart()
    {
        return periodStart;
    }

    public void setPeriodEnd(Date periodEnd)
    {
        this.periodEnd = periodEnd;
    }

    public Date getPeriodEnd()
    {
        return periodEnd;
    }

    public void setLimitUpKv(BigDecimal limitUpKv)
    {
        this.limitUpKv = limitUpKv;
    }

    public BigDecimal getLimitUpKv()
    {
        return limitUpKv;
    }

    public void setLimitDownKv(BigDecimal limitDownKv)
    {
        this.limitDownKv = limitDownKv;
    }

    public BigDecimal getLimitDownKv()
    {
        return limitDownKv;
    }

    public void setSource(String source)
    {
        this.source = source;
    }

    public String getSource()
    {
        return source;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("curveId", getCurveId())
            .append("busbarNum", getBusbarNum())
            .append("quarter", getQuarter())
            .append("periodStart", getPeriodStart())
            .append("periodEnd", getPeriodEnd())
            .append("limitUpKv", getLimitUpKv())
            .append("limitDownKv", getLimitDownKv())
            .append("source", getSource())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("remark", getRemark())
            .toString();
    }
}

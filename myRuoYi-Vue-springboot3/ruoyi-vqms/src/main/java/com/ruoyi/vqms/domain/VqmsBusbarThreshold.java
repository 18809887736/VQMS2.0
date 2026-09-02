package com.ruoyi.vqms.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 母线电压阈值对象 vqms_busbar_threshold
 * 
 * @author ruoyi
 * @date 2026-09-02
 */
public class VqmsBusbarThreshold extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long thresholdId;

    /** 母线编号（逻辑FK → vqms_busbar） */
    @Excel(name = "母线编号", readConverterExp = "逻=辑FK,→=,v=qms_busbar")
    private Long busbarNum;

    /** 口径：AVC=附件6 合格区间 / GB=国标；附件6：500kV±1.5kV、220kV±1kV、66kV及以下±1%额定 */
    @Excel(name = "口径：AVC=附件6 合格区间 / GB=国标；附件6：500kV±1.5kV、220kV±1kV、66kV及以下±1%额定")
    private String criterionType;

    /** AVC 容差 kV：220kV=1.000, 500kV=1.500；66kV及以下按 ±1% 在判定层由 nominal_kv 折算，本列可空 */
    @Excel(name = "AVC 容差 kV：220kV=1.000, 500kV=1.500；66kV及以下按 ±1% 在判定层由 nominal_kv 折算，本列可空")
    private BigDecimal toleranceV;

    /** 生效起始日（含） */
    @Excel(name = "生效起始日", readConverterExp = "含=")
    private Date effectiveFrom;

    /** 生效结束日（含），NULL=至今有效 */
    @Excel(name = "生效结束日", readConverterExp = "含=")
    private Date effectiveTo;

    public void setThresholdId(Long thresholdId) 
    {
        this.thresholdId = thresholdId;
    }

    public Long getThresholdId() 
    {
        return thresholdId;
    }

    public void setBusbarNum(Long busbarNum) 
    {
        this.busbarNum = busbarNum;
    }

    public Long getBusbarNum() 
    {
        return busbarNum;
    }

    public void setCriterionType(String criterionType) 
    {
        this.criterionType = criterionType;
    }

    public String getCriterionType() 
    {
        return criterionType;
    }

    public void setToleranceV(BigDecimal toleranceV) 
    {
        this.toleranceV = toleranceV;
    }

    public BigDecimal getToleranceV() 
    {
        return toleranceV;
    }

    public void setEffectiveFrom(Date effectiveFrom) 
    {
        this.effectiveFrom = effectiveFrom;
    }

    public Date getEffectiveFrom() 
    {
        return effectiveFrom;
    }

    public void setEffectiveTo(Date effectiveTo) 
    {
        this.effectiveTo = effectiveTo;
    }

    public Date getEffectiveTo() 
    {
        return effectiveTo;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("thresholdId", getThresholdId())
            .append("busbarNum", getBusbarNum())
            .append("criterionType", getCriterionType())
            .append("toleranceV", getToleranceV())
            .append("effectiveFrom", getEffectiveFrom())
            .append("effectiveTo", getEffectiveTo())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}

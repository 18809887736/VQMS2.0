package com.ruoyi.vqms.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 判定整定参数对象 vqms_judge_param
 * 
 * @author ruoyi
 * @date 2026-09-02
 */
public class VqmsJudgeParam extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long paramId;

    /** 参数键 */
    @Excel(name = "参数键")
    private String paramKey;

    /** 参数值（分钟数） */
    @Excel(name = "参数值", readConverterExp = "分=钟数")
    private Long paramValue;

    /** 参数名称 */
    @Excel(name = "参数名称")
    private String name;

    /** 说明 */
    @Excel(name = "说明")
    private String description;

    /** 值域下限（含） */
    @Excel(name = "值域下限", readConverterExp = "含=")
    private Long valueMin;

    /** 值域上限（含） */
    @Excel(name = "值域上限", readConverterExp = "含=")
    private Long valueMax;

    /** 状态：0=正常, 1=停用 */
    @Excel(name = "状态：0=正常, 1=停用")
    private String status;

    public void setParamId(Long paramId) 
    {
        this.paramId = paramId;
    }

    public Long getParamId() 
    {
        return paramId;
    }

    public void setParamKey(String paramKey) 
    {
        this.paramKey = paramKey;
    }

    public String getParamKey() 
    {
        return paramKey;
    }

    public void setParamValue(Long paramValue) 
    {
        this.paramValue = paramValue;
    }

    public Long getParamValue() 
    {
        return paramValue;
    }

    public void setName(String name) 
    {
        this.name = name;
    }

    public String getName() 
    {
        return name;
    }

    public void setDescription(String description) 
    {
        this.description = description;
    }

    public String getDescription() 
    {
        return description;
    }

    public void setValueMin(Long valueMin) 
    {
        this.valueMin = valueMin;
    }

    public Long getValueMin() 
    {
        return valueMin;
    }

    public void setValueMax(Long valueMax) 
    {
        this.valueMax = valueMax;
    }

    public Long getValueMax() 
    {
        return valueMax;
    }

    public void setStatus(String status) 
    {
        this.status = status;
    }

    public String getStatus() 
    {
        return status;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("paramId", getParamId())
            .append("paramKey", getParamKey())
            .append("paramValue", getParamValue())
            .append("name", getName())
            .append("description", getDescription())
            .append("valueMin", getValueMin())
            .append("valueMax", getValueMax())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}

package com.ruoyi.vqms.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 数据不可用策略参数对象 vqms_policy_param
 * 
 * @author ruoyi
 * @date 2026-09-02
 */
public class VqmsPolicyParam extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long paramId;

    /** 参数键：规则行 freeform_rule_001..N（按序号有序）/ freeform_threshold_pct（A4 阈值 τ，默认 50 可整定） */
    @Excel(name = "参数键：规则行 freeform_rule_001..N", readConverterExp = "按=序号有序")
    private String paramKey;

    /** 参数值（规则行=「表达式-&gt;动作」DSL 文本；τ=整数百分比；空表=策略未配置，管线只记不判） */
    @Excel(name = "参数值", readConverterExp = "规=则行=「表达式-&gt;动作」DSL,文=本；τ=整数百分比；空表=策略未配置，管线只记不判")
    private String paramValue;

    /** 参数名称 */
    @Excel(name = "参数名称")
    private String name;

    /** 说明 */
    @Excel(name = "说明")
    private String description;

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

    public void setParamValue(String paramValue) 
    {
        this.paramValue = paramValue;
    }

    public String getParamValue() 
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

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("paramId", getParamId())
            .append("paramKey", getParamKey())
            .append("paramValue", getParamValue())
            .append("name", getName())
            .append("description", getDescription())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}

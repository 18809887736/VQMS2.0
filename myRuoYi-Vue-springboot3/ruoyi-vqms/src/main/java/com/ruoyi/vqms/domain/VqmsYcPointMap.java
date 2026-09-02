package com.ruoyi.vqms.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 点号语义注册对象 vqms_yc_point_map
 * 
 * @author ruoyi
 * @date 2026-09-02
 */
public class VqmsYcPointMap extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 点号（yc/yx 统一注册，对齐外部 yc_history / yx_history） */
    private Long pointNum;

    /** C=遥测 yc / X=遥信 yx */
    @Excel(name = "C=遥测 yc / X=遥信 yx")
    private String pointKind;

    /** 语义名称 */
    @Excel(name = "语义名称")
    private String pointName;

    /** busbar_id=主母线号 / voltage=电压 / power=有功 / reactive=无功 / yx=开关量 / analog=编码量 */
    @Excel(name = "busbar_id=主母线号 / voltage=电压 / power=有功 / reactive=无功 / yx=开关量 / analog=编码量")
    private String pointType;

    /** 归属主体（逻辑FK） */
    @Excel(name = "归属主体", readConverterExp = "逻=辑FK")
    private Long entityId;

    /** 关联母线（逻辑FK） */
    @Excel(name = "关联母线", readConverterExp = "逻=辑FK")
    private Long busbarNum;

    /** 单位（模拟量） */
    @Excel(name = "单位", readConverterExp = "模=拟量")
    private String unit;

    /** yx 值=1 语义 */
    @Excel(name = "yx 值=1 语义")
    private String state1Label;

    /** yx 值=0 语义 */
    @Excel(name = "yx 值=0 语义")
    private String state0Label;

    /** 是否启用为考核门控：1=启用；真实环境默认0，现场核对后置1 */
    @Excel(name = "是否启用为考核门控：1=启用；真实环境默认0，现场核对后置1")
    private Integer gateEnabled;

    /** 状态：0=正常, 1=停用 */
    @Excel(name = "状态：0=正常, 1=停用")
    private String status;

    public void setPointNum(Long pointNum) 
    {
        this.pointNum = pointNum;
    }

    public Long getPointNum() 
    {
        return pointNum;
    }

    public void setPointKind(String pointKind) 
    {
        this.pointKind = pointKind;
    }

    public String getPointKind() 
    {
        return pointKind;
    }

    public void setPointName(String pointName) 
    {
        this.pointName = pointName;
    }

    public String getPointName() 
    {
        return pointName;
    }

    public void setPointType(String pointType) 
    {
        this.pointType = pointType;
    }

    public String getPointType() 
    {
        return pointType;
    }

    public void setEntityId(Long entityId) 
    {
        this.entityId = entityId;
    }

    public Long getEntityId() 
    {
        return entityId;
    }

    public void setBusbarNum(Long busbarNum) 
    {
        this.busbarNum = busbarNum;
    }

    public Long getBusbarNum() 
    {
        return busbarNum;
    }

    public void setUnit(String unit) 
    {
        this.unit = unit;
    }

    public String getUnit() 
    {
        return unit;
    }

    public void setState1Label(String state1Label) 
    {
        this.state1Label = state1Label;
    }

    public String getState1Label() 
    {
        return state1Label;
    }

    public void setState0Label(String state0Label) 
    {
        this.state0Label = state0Label;
    }

    public String getState0Label() 
    {
        return state0Label;
    }

    public void setGateEnabled(Integer gateEnabled) 
    {
        this.gateEnabled = gateEnabled;
    }

    public Integer getGateEnabled() 
    {
        return gateEnabled;
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
            .append("pointNum", getPointNum())
            .append("pointKind", getPointKind())
            .append("pointName", getPointName())
            .append("pointType", getPointType())
            .append("entityId", getEntityId())
            .append("busbarNum", getBusbarNum())
            .append("unit", getUnit())
            .append("state1Label", getState1Label())
            .append("state0Label", getState0Label())
            .append("gateEnabled", getGateEnabled())
            .append("status", getStatus())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}

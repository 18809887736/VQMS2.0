package com.ruoyi.vqms.domain;

import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 主母线台账对象 vqms_busbar
 * 
 * @author ruoyi
 * @date 2026-09-02
 */
public class VqmsBusbar extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主母线编号，对齐 his_curve_sv.busbar_num（值对齐直连，设计原则9） */
    private Long busbarNum;

    /** 母线名称（0/1=220kV 东/西母线，2=500kV——拍板①单档在运、按档登记） */
    @Excel(name = "母线名称", readConverterExp = "0=/1=220kV,东=/西母线，2=500kV——拍板①单档在运、按档登记")
    private String busbarName;

    /** 电压等级编码，对齐字典 vqms_v_grade：0=500kV, 1=220kV, 2=66kV及以下(预留) */
    @Excel(name = "电压等级编码，对齐字典 vqms_v_grade：0=500kV, 1=220kV, 2=66kV及以下(预留)")
    private Long vGrade;

    /** 所属母线组（逻辑FK → vqms_busbar_group） */
    @Excel(name = "所属母线组", readConverterExp = "逻=辑FK,→=,v=qms_busbar_group")
    private Long groupNum;

    /** 标称电压 kV */
    @Excel(name = "标称电压 kV")
    private BigDecimal nominalKv;

    /** 该母线 t0 实时电压 yc 点（增量指令算 V_target 用；候选 yc8 东母/yc14 西母） */
    @Excel(name = "该母线 t0 实时电压 yc 点", readConverterExp = "增=量指令算,V=_target,用=；候选,y=c8,东=母/yc14,西=母")
    private Long realtimeYcNum;

    /** 状态：0=正常, 1=停用 */
    @Excel(name = "状态：0=正常, 1=停用")
    private String status;

    public void setBusbarNum(Long busbarNum) 
    {
        this.busbarNum = busbarNum;
    }

    public Long getBusbarNum() 
    {
        return busbarNum;
    }

    public void setBusbarName(String busbarName) 
    {
        this.busbarName = busbarName;
    }

    public String getBusbarName() 
    {
        return busbarName;
    }

    public void setvGrade(Long vGrade) 
    {
        this.vGrade = vGrade;
    }

    public Long getvGrade() 
    {
        return vGrade;
    }

    public void setGroupNum(Long groupNum) 
    {
        this.groupNum = groupNum;
    }

    public Long getGroupNum() 
    {
        return groupNum;
    }

    public void setNominalKv(BigDecimal nominalKv) 
    {
        this.nominalKv = nominalKv;
    }

    public BigDecimal getNominalKv() 
    {
        return nominalKv;
    }

    public void setRealtimeYcNum(Long realtimeYcNum) 
    {
        this.realtimeYcNum = realtimeYcNum;
    }

    public Long getRealtimeYcNum() 
    {
        return realtimeYcNum;
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
            .append("busbarNum", getBusbarNum())
            .append("busbarName", getBusbarName())
            .append("vGrade", getvGrade())
            .append("groupNum", getGroupNum())
            .append("nominalKv", getNominalKv())
            .append("realtimeYcNum", getRealtimeYcNum())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}

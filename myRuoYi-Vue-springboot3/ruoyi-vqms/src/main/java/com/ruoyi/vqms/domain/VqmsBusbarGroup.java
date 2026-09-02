package com.ruoyi.vqms.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 母线组对象 vqms_busbar_group
 * 
 * @author ruoyi
 * @date 2026-09-02
 */
public class VqmsBusbarGroup extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 母线组编号 */
    private Long groupNum;

    /** 所属并网主体（逻辑FK → vqms_entity） */
    @Excel(name = "所属并网主体", readConverterExp = "逻=辑FK,→=,v=qms_entity")
    private Long entityId;

    /** 组名 */
    @Excel(name = "组名")
    private String groupName;

    /** 电压等级编码，对齐字典 vqms_v_grade：0=500kV, 1=220kV, 2=66kV及以下(预留) */
    @Excel(name = "电压等级编码，对齐字典 vqms_v_grade：0=500kV, 1=220kV, 2=66kV及以下(预留)")
    private Long vGrade;

    /** 该组当前主母线号指示点（对端 BUSBAR_GROUP.MainBarYcNum=3 候选）；未接入前为空 */
    @Excel(name = "该组当前主母线号指示点", readConverterExp = "对=端,B=USBAR_GROUP.MainBarYcNum=3,候=选")
    private Long mainIndicatorYcNum;

    /** 指示点不可用兜底主母线号；NULL=不兜底→该组该分钟无主母线 */
    @Excel(name = "指示点不可用兜底主母线号；NULL=不兜底→该组该分钟无主母线")
    private Long defaultMainBusbarNum;

    /** 指示点陈旧窗口(分钟) */
    @Excel(name = "指示点陈旧窗口(分钟)")
    private Long maxStalenessMinutes;

    /** 状态：0=正常, 1=停用 */
    @Excel(name = "状态：0=正常, 1=停用")
    private String status;

    public void setGroupNum(Long groupNum) 
    {
        this.groupNum = groupNum;
    }

    public Long getGroupNum() 
    {
        return groupNum;
    }

    public void setEntityId(Long entityId) 
    {
        this.entityId = entityId;
    }

    public Long getEntityId() 
    {
        return entityId;
    }

    public void setGroupName(String groupName) 
    {
        this.groupName = groupName;
    }

    public String getGroupName() 
    {
        return groupName;
    }

    public void setvGrade(Long vGrade) 
    {
        this.vGrade = vGrade;
    }

    public Long getvGrade() 
    {
        return vGrade;
    }

    public void setMainIndicatorYcNum(Long mainIndicatorYcNum) 
    {
        this.mainIndicatorYcNum = mainIndicatorYcNum;
    }

    public Long getMainIndicatorYcNum() 
    {
        return mainIndicatorYcNum;
    }

    public void setDefaultMainBusbarNum(Long defaultMainBusbarNum) 
    {
        this.defaultMainBusbarNum = defaultMainBusbarNum;
    }

    public Long getDefaultMainBusbarNum() 
    {
        return defaultMainBusbarNum;
    }

    public void setMaxStalenessMinutes(Long maxStalenessMinutes) 
    {
        this.maxStalenessMinutes = maxStalenessMinutes;
    }

    public Long getMaxStalenessMinutes() 
    {
        return maxStalenessMinutes;
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
            .append("groupNum", getGroupNum())
            .append("entityId", getEntityId())
            .append("groupName", getGroupName())
            .append("vGrade", getvGrade())
            .append("mainIndicatorYcNum", getMainIndicatorYcNum())
            .append("defaultMainBusbarNum", getDefaultMainBusbarNum())
            .append("maxStalenessMinutes", getMaxStalenessMinutes())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}

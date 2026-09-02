package com.ruoyi.vqms.domain;

import java.math.BigDecimal;
import java.util.Date;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 并网主体对象 vqms_entity（考核主体，罚款基数载体）
 *
 * @author vqms
 */
public class VqmsEntity extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 并网主体ID */
    @Excel(name = "主体ID")
    private Long entityId;

    /** 主体编号（调度口径） */
    @Excel(name = "主体编号")
    private String entityCode;

    /** 主体名称 */
    @Excel(name = "主体名称")
    private String entityName;

    /** 类型（字典 vqms_entity_type）：1=火电 2=水电 3=核电 4=风电 5=光伏 6=新型储能 7=光热 8=其他 */
    @Excel(name = "主体类型", readConverterExp = "1=火电,2=水电,3=核电,4=风电,5=光伏,6=新型储能,7=光热,8=其他")
    private String entityType;

    /** 额定容量 kW（考核基数：缺额pp×容量/10000×0.02分；NULL=待补录不产罚款数） */
    @Excel(name = "额定容量kW")
    private BigDecimal ratedCapacityKw;

    /** 是否 AVC 主站闭环控制主体（1=是 0=否，第26条电压考核豁免判定输入） */
    @Excel(name = "AVC闭环", readConverterExp = "1=是,0=否")
    private Integer avcClosedLoop;

    /** 并网生效日 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "并网生效日", dateFormat = "yyyy-MM-dd")
    private Date effectiveFrom;

    /** 解列日（NULL=在运） */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "解列日", dateFormat = "yyyy-MM-dd")
    private Date effectiveTo;

    /** 状态（0正常 1停用） */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    public Long getEntityId()
    {
        return entityId;
    }

    public void setEntityId(Long entityId)
    {
        this.entityId = entityId;
    }

    @NotBlank(message = "主体编号不能为空")
    @Size(min = 0, max = 64, message = "主体编号长度不能超过64个字符")
    public String getEntityCode()
    {
        return entityCode;
    }

    public void setEntityCode(String entityCode)
    {
        this.entityCode = entityCode;
    }

    @NotBlank(message = "主体名称不能为空")
    @Size(min = 0, max = 128, message = "主体名称长度不能超过128个字符")
    public String getEntityName()
    {
        return entityName;
    }

    public void setEntityName(String entityName)
    {
        this.entityName = entityName;
    }

    @NotBlank(message = "主体类型不能为空")
    public String getEntityType()
    {
        return entityType;
    }

    public void setEntityType(String entityType)
    {
        this.entityType = entityType;
    }

    public BigDecimal getRatedCapacityKw()
    {
        return ratedCapacityKw;
    }

    public void setRatedCapacityKw(BigDecimal ratedCapacityKw)
    {
        this.ratedCapacityKw = ratedCapacityKw;
    }

    public Integer getAvcClosedLoop()
    {
        return avcClosedLoop;
    }

    public void setAvcClosedLoop(Integer avcClosedLoop)
    {
        this.avcClosedLoop = avcClosedLoop;
    }

    public Date getEffectiveFrom()
    {
        return effectiveFrom;
    }

    public void setEffectiveFrom(Date effectiveFrom)
    {
        this.effectiveFrom = effectiveFrom;
    }

    public Date getEffectiveTo()
    {
        return effectiveTo;
    }

    public void setEffectiveTo(Date effectiveTo)
    {
        this.effectiveTo = effectiveTo;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("entityId", getEntityId())
            .append("entityCode", getEntityCode())
            .append("entityName", getEntityName())
            .append("entityType", getEntityType())
            .append("ratedCapacityKw", getRatedCapacityKw())
            .append("avcClosedLoop", getAvcClosedLoop())
            .append("effectiveFrom", getEffectiveFrom())
            .append("effectiveTo", getEffectiveTo())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}

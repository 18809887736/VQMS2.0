package com.ruoyi.vqms.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 设备P-Q极限曲线对象 vqms_device_pq_limit
 * 
 * @author ruoyi
 * @date 2026-09-02
 */
public class VqmsDevicePqLimit extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 设备（逻辑FK → vqms_reactive_device） */
    @Excel(name = "设备", readConverterExp = "逻=辑FK,→=,v=qms_reactive_device")
    private Long deviceId;

    /** 有功工况点 kW */
    @Excel(name = "有功工况点 kW")
    private BigDecimal pKw;

    /** 该 P 下发出上限 kvar */
    @Excel(name = "该 P 下发出上限 kvar")
    private BigDecimal qUpKvar;

    /** 该 P 下吸收下限 kvar（负值） */
    @Excel(name = "该 P 下吸收下限 kvar", readConverterExp = "负=值")
    private BigDecimal qDownKvar;

    /** 生效起始日（曲线换版不回溯） */
    @Excel(name = "生效起始日", readConverterExp = "曲=线换版不回溯")
    private Date effectiveFrom;

    /** 生效结束日；NULL=至今 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "生效结束日；NULL=至今", width = 30, dateFormat = "yyyy-MM-dd")
    private Date effectiveTo;

    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }

    public void setDeviceId(Long deviceId) 
    {
        this.deviceId = deviceId;
    }

    public Long getDeviceId() 
    {
        return deviceId;
    }

    public void setpKw(BigDecimal pKw) 
    {
        this.pKw = pKw;
    }

    public BigDecimal getpKw() 
    {
        return pKw;
    }

    public void setqUpKvar(BigDecimal qUpKvar) 
    {
        this.qUpKvar = qUpKvar;
    }

    public BigDecimal getqUpKvar() 
    {
        return qUpKvar;
    }

    public void setqDownKvar(BigDecimal qDownKvar) 
    {
        this.qDownKvar = qDownKvar;
    }

    public BigDecimal getqDownKvar() 
    {
        return qDownKvar;
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
            .append("id", getId())
            .append("deviceId", getDeviceId())
            .append("pKw", getpKw())
            .append("qUpKvar", getqUpKvar())
            .append("qDownKvar", getqDownKvar())
            .append("effectiveFrom", getEffectiveFrom())
            .append("effectiveTo", getEffectiveTo())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}

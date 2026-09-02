package com.ruoyi.vqms.domain;

import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 无功设备台账对象 vqms_reactive_device
 * 
 * @author ruoyi
 * @date 2026-09-02
 */
public class VqmsReactiveDevice extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long deviceId;

    /** 所属主体（逻辑FK） */
    @Excel(name = "所属主体", readConverterExp = "逻=辑FK")
    private Long entityId;

    /** 设备编号 */
    @Excel(name = "设备编号")
    private String deviceCode;

    /** 设备名称 */
    @Excel(name = "设备名称")
    private String deviceName;

    /** 1=同步发电机/调相机 2=逆变器型(风/光/储) 3=SVC/STATCOM 4=电容器组 5=电抗器（字典 vqms_device_type） */
    @Excel(name = "1=同步发电机/调相机 2=逆变器型(风/光/储) 3=SVC/STATCOM 4=电容器组 5=电抗器", readConverterExp = "字=典,v=qms_device_type")
    private Long deviceType;

    /** 是否纳入 AVC 闭环控制（免考判定只考察闭环设备） */
    @Excel(name = "是否纳入 AVC 闭环控制", readConverterExp = "免=考判定只考察闭环设备")
    private Integer inAvcLoop;

    /** 视在容量 kVA（逆变器型：Q=±√(S²−P²) 判定用） */
    @Excel(name = "视在容量 kVA", readConverterExp = "逆=变器型：Q=±√(S²−P²),判=定用")
    private BigDecimal ratedSKva;

    /** 发出上限 kvar（对称/单向设备额定；发电机类本列空、查 P-Q 曲线表） */
    @Excel(name = "发出上限 kvar", readConverterExp = "对=称/单向设备额定；发电机类本列空、查,P=-Q,曲=线表")
    private BigDecimal ratedQUpKvar;

    /** 吸收下限 kvar（负值；单向设备空） */
    @Excel(name = "吸收下限 kvar", readConverterExp = "负=值；单向设备空")
    private BigDecimal ratedQDownKvar;

    /** 无功遥测点号（逻辑FK → vqms_yc_point_map） */
    @Excel(name = "无功遥测点号", readConverterExp = "逻=辑FK,→=,v=qms_yc_point_map")
    private Long qYcNum;

    /** 有功遥测点号（P-Q 曲线插值用） */
    @Excel(name = "有功遥测点号", readConverterExp = "P=-Q,曲=线插值用")
    private Long pYcNum;

    /** 状态：0=正常, 1=停用 */
    @Excel(name = "状态：0=正常, 1=停用")
    private String status;

    public void setDeviceId(Long deviceId) 
    {
        this.deviceId = deviceId;
    }

    public Long getDeviceId() 
    {
        return deviceId;
    }

    public void setEntityId(Long entityId) 
    {
        this.entityId = entityId;
    }

    public Long getEntityId() 
    {
        return entityId;
    }

    public void setDeviceCode(String deviceCode) 
    {
        this.deviceCode = deviceCode;
    }

    public String getDeviceCode() 
    {
        return deviceCode;
    }

    public void setDeviceName(String deviceName) 
    {
        this.deviceName = deviceName;
    }

    public String getDeviceName() 
    {
        return deviceName;
    }

    public void setDeviceType(Long deviceType) 
    {
        this.deviceType = deviceType;
    }

    public Long getDeviceType() 
    {
        return deviceType;
    }

    public void setInAvcLoop(Integer inAvcLoop) 
    {
        this.inAvcLoop = inAvcLoop;
    }

    public Integer getInAvcLoop() 
    {
        return inAvcLoop;
    }

    public void setRatedSKva(BigDecimal ratedSKva) 
    {
        this.ratedSKva = ratedSKva;
    }

    public BigDecimal getRatedSKva() 
    {
        return ratedSKva;
    }

    public void setRatedQUpKvar(BigDecimal ratedQUpKvar) 
    {
        this.ratedQUpKvar = ratedQUpKvar;
    }

    public BigDecimal getRatedQUpKvar() 
    {
        return ratedQUpKvar;
    }

    public void setRatedQDownKvar(BigDecimal ratedQDownKvar) 
    {
        this.ratedQDownKvar = ratedQDownKvar;
    }

    public BigDecimal getRatedQDownKvar() 
    {
        return ratedQDownKvar;
    }

    public void setqYcNum(Long qYcNum) 
    {
        this.qYcNum = qYcNum;
    }

    public Long getqYcNum() 
    {
        return qYcNum;
    }

    public void setpYcNum(Long pYcNum) 
    {
        this.pYcNum = pYcNum;
    }

    public Long getpYcNum() 
    {
        return pYcNum;
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
            .append("deviceId", getDeviceId())
            .append("entityId", getEntityId())
            .append("deviceCode", getDeviceCode())
            .append("deviceName", getDeviceName())
            .append("deviceType", getDeviceType())
            .append("inAvcLoop", getInAvcLoop())
            .append("ratedSKva", getRatedSKva())
            .append("ratedQUpKvar", getRatedQUpKvar())
            .append("ratedQDownKvar", getRatedQDownKvar())
            .append("qYcNum", getqYcNum())
            .append("pYcNum", getpYcNum())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}

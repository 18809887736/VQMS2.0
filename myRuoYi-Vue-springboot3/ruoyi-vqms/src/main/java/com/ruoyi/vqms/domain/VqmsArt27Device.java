package com.ruoyi.vqms.domain;

import java.math.BigDecimal;
import com.ruoyi.common.core.domain.BaseEntity;

/** 第27条动态无功补偿装置台账 vqms_art27_device。 */
public class VqmsArt27Device extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long deviceId;
    private Long entityId;
    private String deviceCode;
    private String deviceName;
    /** 1=SVG 2=SVC 3=调相机 */
    private Integer deviceType;
    private BigDecimal ratedCapacityKw;
    private Long autoYxNum;
    private Long energizedYxNum;
    private String status;

    public Long getDeviceId() { return deviceId; }
    public void setDeviceId(Long v) { this.deviceId = v; }
    public Long getEntityId() { return entityId; }
    public void setEntityId(Long v) { this.entityId = v; }
    public String getDeviceCode() { return deviceCode; }
    public void setDeviceCode(String v) { this.deviceCode = v; }
    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String v) { this.deviceName = v; }
    public Integer getDeviceType() { return deviceType; }
    public void setDeviceType(Integer v) { this.deviceType = v; }
    public BigDecimal getRatedCapacityKw() { return ratedCapacityKw; }
    public void setRatedCapacityKw(BigDecimal v) { this.ratedCapacityKw = v; }
    public Long getAutoYxNum() { return autoYxNum; }
    public void setAutoYxNum(Long v) { this.autoYxNum = v; }
    public Long getEnergizedYxNum() { return energizedYxNum; }
    public void setEnergizedYxNum(Long v) { this.energizedYxNum = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
}

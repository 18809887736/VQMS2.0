package com.ruoyi.vqms.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * AVC退出原因标注对象 vqms_exit_annotation
 * 
 * @author ruoyi
 * @date 2026-09-02
 */
public class VqmsExitAnnotation extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long annotationId;

    /** 考核主体（逻辑FK） */
    @Excel(name = "考核主体", readConverterExp = "逻=辑FK")
    private Long entityId;

    /** 退出时段起（含，北京历法） */
    @Excel(name = "退出时段起", readConverterExp = "含=，北京历法")
    private Date periodStart;

    /** 退出时段止（含） */
    @Excel(name = "退出时段止", readConverterExp = "含=")
    private Date periodEnd;

    /** GRID=电网原因（免责，出分母）/ NON_GRID=非电网（扣罚，在分母）/ UNKNOWN=原因不明 */
    @Excel(name = "GRID=电网原因", readConverterExp = "免=责，出分母")
    private String exitReason;

    /** 来源：AUTO_YC=yc521/522 三态点自动 / MANUAL=人工标注 */
    @Excel(name = "来源：AUTO_YC=yc521/522 三态点自动 / MANUAL=人工标注")
    private String source;

    /** 依据（闭锁信号/检修票/调度记录） */
    @Excel(name = "依据", readConverterExp = "闭=锁信号/检修票/调度记录")
    private String evidence;

    /** 状态：0=有效, 1=撤销 */
    @Excel(name = "状态：0=有效, 1=撤销")
    private String status;

    public void setAnnotationId(Long annotationId) 
    {
        this.annotationId = annotationId;
    }

    public Long getAnnotationId() 
    {
        return annotationId;
    }

    public void setEntityId(Long entityId) 
    {
        this.entityId = entityId;
    }

    public Long getEntityId() 
    {
        return entityId;
    }

    public void setPeriodStart(Date periodStart) 
    {
        this.periodStart = periodStart;
    }

    public Date getPeriodStart() 
    {
        return periodStart;
    }

    public void setPeriodEnd(Date periodEnd) 
    {
        this.periodEnd = periodEnd;
    }

    public Date getPeriodEnd() 
    {
        return periodEnd;
    }

    public void setExitReason(String exitReason) 
    {
        this.exitReason = exitReason;
    }

    public String getExitReason() 
    {
        return exitReason;
    }

    public void setSource(String source) 
    {
        this.source = source;
    }

    public String getSource() 
    {
        return source;
    }

    public void setEvidence(String evidence) 
    {
        this.evidence = evidence;
    }

    public String getEvidence() 
    {
        return evidence;
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
            .append("annotationId", getAnnotationId())
            .append("entityId", getEntityId())
            .append("periodStart", getPeriodStart())
            .append("periodEnd", getPeriodEnd())
            .append("exitReason", getExitReason())
            .append("source", getSource())
            .append("evidence", getEvidence())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}

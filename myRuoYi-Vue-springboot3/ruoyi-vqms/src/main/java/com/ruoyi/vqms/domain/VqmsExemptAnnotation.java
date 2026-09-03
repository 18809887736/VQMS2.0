package com.ruoyi.vqms.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 调节免考标注对象 vqms_exempt_annotation
 * 
 * @author ruoyi
 * @date 2026-09-02
 */
public class VqmsExemptAnnotation extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long annotationId;

    /** 考核主体（逻辑FK） */
    @Excel(name = "考核主体", readConverterExp = "逻=辑FK")
    private Long entityId;

    /** 指向指令（与 millisecond/obj_num 组成溯源键） */
    @Excel(name = "指向指令", readConverterExp = "与=,m=illisecond/obj_num,组=成溯源键")
    private String warnTimeRaw;

    /** 毫秒原文（溯源键成分） */
    @Excel(name = "毫秒原文", readConverterExp = "溯=源键成分")
    private String millisecond;

    /** 对象编号（溯源键成分） */
    @Excel(name = "对象编号", readConverterExp = "溯=源键成分")
    private Long objNum;

    /** 免考档：FAST/ECON/BOTH */
    @Excel(name = "免考档：FAST/ECON/BOTH")
    private String tier;

    /** 免考依据（附件6§三：全部闭环无功设备正确方向顶满仍不达标 等） */
    @Excel(name = "免考依据", readConverterExp = "附=件6§三：全部闭环无功设备正确方向顶满仍不达标,等=")
    private String exemptReason;

    /** 佐证材料描述（设备Q曲线截图/调度电话记录等） */
    @Excel(name = "佐证材料描述", readConverterExp = "设=备Q曲线截图/调度电话记录等")
    private String evidence;

    /** 复核状态：PENDING=待复核 / APPROVED=已批准（生效） / REJECTED=已驳回 */
    @Excel(name = "复核状态：PENDING=待复核 / APPROVED=已批准", readConverterExp = "生=效")
    private String reviewStatus;

    /** 复核人（≠标注人，Service 层校验） */
    @Excel(name = "复核人", readConverterExp = "≠=标注人，Service,层=校验")
    private String reviewBy;

    /** 复核时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "复核时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date reviewTime;

    /** 复核意见（驳回原因等） */
    @Excel(name = "复核意见", readConverterExp = "驳=回原因等")
    private String reviewOpinion;

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

    public void setWarnTimeRaw(String warnTimeRaw) 
    {
        this.warnTimeRaw = warnTimeRaw;
    }

    public String getWarnTimeRaw() 
    {
        return warnTimeRaw;
    }

    public void setMillisecond(String millisecond) 
    {
        this.millisecond = millisecond;
    }

    public String getMillisecond() 
    {
        return millisecond;
    }

    public void setObjNum(Long objNum) 
    {
        this.objNum = objNum;
    }

    public Long getObjNum() 
    {
        return objNum;
    }

    public void setTier(String tier) 
    {
        this.tier = tier;
    }

    public String getTier() 
    {
        return tier;
    }

    public void setExemptReason(String exemptReason) 
    {
        this.exemptReason = exemptReason;
    }

    public String getExemptReason() 
    {
        return exemptReason;
    }

    public void setEvidence(String evidence) 
    {
        this.evidence = evidence;
    }

    public String getEvidence() 
    {
        return evidence;
    }

    public void setReviewStatus(String reviewStatus) 
    {
        this.reviewStatus = reviewStatus;
    }

    public String getReviewStatus() 
    {
        return reviewStatus;
    }

    public void setReviewBy(String reviewBy) 
    {
        this.reviewBy = reviewBy;
    }

    public String getReviewBy() 
    {
        return reviewBy;
    }

    public void setReviewTime(Date reviewTime) 
    {
        this.reviewTime = reviewTime;
    }

    public Date getReviewTime() 
    {
        return reviewTime;
    }

    public void setReviewOpinion(String reviewOpinion) 
    {
        this.reviewOpinion = reviewOpinion;
    }

    public String getReviewOpinion() 
    {
        return reviewOpinion;
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
            .append("warnTimeRaw", getWarnTimeRaw())
            .append("millisecond", getMillisecond())
            .append("objNum", getObjNum())
            .append("tier", getTier())
            .append("exemptReason", getExemptReason())
            .append("evidence", getEvidence())
            .append("reviewStatus", getReviewStatus())
            .append("reviewBy", getReviewBy())
            .append("reviewTime", getReviewTime())
            .append("reviewOpinion", getReviewOpinion())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}

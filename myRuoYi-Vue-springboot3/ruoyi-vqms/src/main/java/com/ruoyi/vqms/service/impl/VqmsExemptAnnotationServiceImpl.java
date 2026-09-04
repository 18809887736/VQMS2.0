package com.ruoyi.vqms.service.impl;

import java.util.List;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.vqms.mapper.VqmsExemptAnnotationMapper;
import com.ruoyi.vqms.domain.VqmsExemptAnnotation;
import com.ruoyi.vqms.service.IVqmsExemptAnnotationService;

/**
 * 调节免考标注Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-09-02
 */
@Service
public class VqmsExemptAnnotationServiceImpl implements IVqmsExemptAnnotationService 
{
    @Autowired
    private VqmsExemptAnnotationMapper vqmsExemptAnnotationMapper;

    @Autowired
    private com.ruoyi.vqms.mapper.VqmsJudgeParamMapper judgeParamMapper;

    private boolean isTwoLevelReview() {
        com.ruoyi.vqms.domain.VqmsJudgeParam q = new com.ruoyi.vqms.domain.VqmsJudgeParam();
        q.setParamKey("exempt_review_two_level");
        java.util.List<com.ruoyi.vqms.domain.VqmsJudgeParam> list = judgeParamMapper.selectVqmsJudgeParamList(q);
        return list != null && !list.isEmpty() && list.get(0).getParamValue() != null
                && list.get(0).getParamValue() == 1;
    }

    /**
     * 查询调节免考标注
     * 
     * @param annotationId 调节免考标注主键
     * @return 调节免考标注
     */
    @Override
    public VqmsExemptAnnotation selectVqmsExemptAnnotationByAnnotationId(Long annotationId)
    {
        return vqmsExemptAnnotationMapper.selectVqmsExemptAnnotationByAnnotationId(annotationId);
    }

    /**
     * 查询调节免考标注列表
     * 
     * @param vqmsExemptAnnotation 调节免考标注
     * @return 调节免考标注
     */
    @Override
    public List<VqmsExemptAnnotation> selectVqmsExemptAnnotationList(VqmsExemptAnnotation vqmsExemptAnnotation)
    {
        return vqmsExemptAnnotationMapper.selectVqmsExemptAnnotationList(vqmsExemptAnnotation);
    }

    /**
     * 新增调节免考标注
     * 
     * @param vqmsExemptAnnotation 调节免考标注
     * @return 结果
     */
    @Override
    public int insertVqmsExemptAnnotation(VqmsExemptAnnotation vqmsExemptAnnotation)
    {
        vqmsExemptAnnotation.setCreateBy(SecurityUtils.getUsername());
        vqmsExemptAnnotation.setCreateTime(DateUtils.getNowDate());
        return vqmsExemptAnnotationMapper.insertVqmsExemptAnnotation(vqmsExemptAnnotation);
    }

    /**
     * 修改调节免考标注
     * 
     * @param vqmsExemptAnnotation 调节免考标注
     * @return 结果
     */
    /**
     * 修改免考标注（内控约束）：
     *  - 仅 PENDING 可改——已批/已驳行锁定，改判走复核链（防绕过双人复核）
     *  - 复核字段（review_status/by/time/opinion）一律剥离，只经 reviewVqmsExemptAnnotation 生效
     */
    @Override
    public int updateVqmsExemptAnnotation(VqmsExemptAnnotation vqmsExemptAnnotation)
    {
        VqmsExemptAnnotation row = vqmsExemptAnnotationMapper
                .selectVqmsExemptAnnotationByAnnotationId(vqmsExemptAnnotation.getAnnotationId());
        if (row == null)
        {
            throw new ServiceException("标注不存在");
        }
        if (!"PENDING".equals(row.getReviewStatus()))
        {
            throw new ServiceException("已复核标注（" + row.getReviewStatus() + "）锁定不可修改；改判需撤销后重新标注复核");
        }
        vqmsExemptAnnotation.setReviewStatus(null);
        vqmsExemptAnnotation.setReviewBy(null);
        vqmsExemptAnnotation.setReviewTime(null);
        vqmsExemptAnnotation.setReviewOpinion(null);
        vqmsExemptAnnotation.setUpdateBy(SecurityUtils.getUsername());
        vqmsExemptAnnotation.setUpdateTime(DateUtils.getNowDate());
        return vqmsExemptAnnotationMapper.updateVqmsExemptAnnotation(vqmsExemptAnnotation);
    }

    /**
     * 批量删除调节免考标注
     * 
     * @param annotationIds 需要删除的调节免考标注主键
     * @return 结果
     */
    @Override
    public int deleteVqmsExemptAnnotationByAnnotationIds(Long[] annotationIds)
    {
        for (Long id : annotationIds)
        {
            assertDeletable(id);
        }
        return vqmsExemptAnnotationMapper.deleteVqmsExemptAnnotationByAnnotationIds(annotationIds);
    }

    /** 已复核标注不可物理删除（审计留痕；与 update 锁定口径一致）。 */
    private void assertDeletable(Long annotationId)
    {
        VqmsExemptAnnotation row = vqmsExemptAnnotationMapper
                .selectVqmsExemptAnnotationByAnnotationId(annotationId);
        if (row != null && !"PENDING".equals(row.getReviewStatus()))
        {
            throw new ServiceException("已复核标注（#" + annotationId + " " + row.getReviewStatus()
                    + "）不可删除（审计留痕）");
        }
    }

    /**
     * 删除调节免考标注信息
     * 
     * @param annotationId 调节免考标注主键
     * @return 结果
     */
    @Override
    public int deleteVqmsExemptAnnotationByAnnotationId(Long annotationId)
    {
        return vqmsExemptAnnotationMapper.deleteVqmsExemptAnnotationByAnnotationId(annotationId);
    }

    /**
     * 复核免考标注：仅 PENDING 可复核；复核模式界面可整定（原子化，核实单 §6）：
     * exempt_review_two_level=0 单账户自批（默认，Leo 2026-09-04 拍板）/ =1 两级复核（恢复标注人≠复核人校验）；
     * APPROVED 后由判定重算拾取（MANUAL 免考源，优先级最高）
     */
    @Override
    public int reviewVqmsExemptAnnotation(VqmsExemptAnnotation annotation)
    {
        String target = annotation.getReviewStatus();
        if (!"APPROVED".equals(target) && !"REJECTED".equals(target))
        {
            throw new ServiceException("复核结论只能是 APPROVED 或 REJECTED");
        }
        VqmsExemptAnnotation row = vqmsExemptAnnotationMapper
                .selectVqmsExemptAnnotationByAnnotationId(annotation.getAnnotationId());
        if (row == null)
        {
            throw new ServiceException("标注不存在");
        }
        if (!"PENDING".equals(row.getReviewStatus()))
        {
            throw new ServiceException("仅待复核(PENDING)标注可复核，当前状态: " + row.getReviewStatus());
        }
        String reviewer = SecurityUtils.getUsername();
        if (isTwoLevelReview() && reviewer.equals(row.getCreateBy()))
        {
            throw new ServiceException("复核人不能与标注人相同（当前整定：两级复核 exempt_review_two_level=1）");
        }
        VqmsExemptAnnotation upd = new VqmsExemptAnnotation();
        upd.setAnnotationId(annotation.getAnnotationId());
        upd.setReviewStatus(target);
        upd.setReviewBy(reviewer);
        upd.setReviewTime(DateUtils.getNowDate());
        upd.setReviewOpinion(annotation.getReviewOpinion());
        return vqmsExemptAnnotationMapper.updateVqmsExemptAnnotation(upd);
    }
}

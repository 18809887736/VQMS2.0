package com.ruoyi.vqms.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
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
        vqmsExemptAnnotation.setCreateTime(DateUtils.getNowDate());
        return vqmsExemptAnnotationMapper.insertVqmsExemptAnnotation(vqmsExemptAnnotation);
    }

    /**
     * 修改调节免考标注
     * 
     * @param vqmsExemptAnnotation 调节免考标注
     * @return 结果
     */
    @Override
    public int updateVqmsExemptAnnotation(VqmsExemptAnnotation vqmsExemptAnnotation)
    {
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
        return vqmsExemptAnnotationMapper.deleteVqmsExemptAnnotationByAnnotationIds(annotationIds);
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
}

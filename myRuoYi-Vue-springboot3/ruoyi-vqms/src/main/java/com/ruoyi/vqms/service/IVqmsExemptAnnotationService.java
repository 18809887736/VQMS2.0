package com.ruoyi.vqms.service;

import java.util.List;
import com.ruoyi.vqms.domain.VqmsExemptAnnotation;

/**
 * 调节免考标注Service接口
 * 
 * @author ruoyi
 * @date 2026-09-02
 */
public interface IVqmsExemptAnnotationService 
{
    /**
     * 查询调节免考标注
     * 
     * @param annotationId 调节免考标注主键
     * @return 调节免考标注
     */
    public VqmsExemptAnnotation selectVqmsExemptAnnotationByAnnotationId(Long annotationId);

    /**
     * 查询调节免考标注列表
     * 
     * @param vqmsExemptAnnotation 调节免考标注
     * @return 调节免考标注集合
     */
    public List<VqmsExemptAnnotation> selectVqmsExemptAnnotationList(VqmsExemptAnnotation vqmsExemptAnnotation);

    /**
     * 新增调节免考标注
     * 
     * @param vqmsExemptAnnotation 调节免考标注
     * @return 结果
     */
    public int insertVqmsExemptAnnotation(VqmsExemptAnnotation vqmsExemptAnnotation);

    /**
     * 修改调节免考标注
     * 
     * @param vqmsExemptAnnotation 调节免考标注
     * @return 结果
     */
    public int updateVqmsExemptAnnotation(VqmsExemptAnnotation vqmsExemptAnnotation);

    /**
     * 批量删除调节免考标注
     * 
     * @param annotationIds 需要删除的调节免考标注主键集合
     * @return 结果
     */
    public int deleteVqmsExemptAnnotationByAnnotationIds(Long[] annotationIds);

    /**
     * 删除调节免考标注信息
     * 
     * @param annotationId 调节免考标注主键
     * @return 结果
     */
    public int deleteVqmsExemptAnnotationByAnnotationId(Long annotationId);
}

package com.ruoyi.vqms.service;

import java.util.List;
import com.ruoyi.vqms.domain.VqmsExitAnnotation;

/**
 * AVC退出原因标注Service接口
 * 
 * @author ruoyi
 * @date 2026-09-02
 */
public interface IVqmsExitAnnotationService 
{
    /**
     * 查询AVC退出原因标注
     * 
     * @param annotationId AVC退出原因标注主键
     * @return AVC退出原因标注
     */
    public VqmsExitAnnotation selectVqmsExitAnnotationByAnnotationId(Long annotationId);

    /**
     * 查询AVC退出原因标注列表
     * 
     * @param vqmsExitAnnotation AVC退出原因标注
     * @return AVC退出原因标注集合
     */
    public List<VqmsExitAnnotation> selectVqmsExitAnnotationList(VqmsExitAnnotation vqmsExitAnnotation);

    /**
     * 新增AVC退出原因标注
     * 
     * @param vqmsExitAnnotation AVC退出原因标注
     * @return 结果
     */
    public int insertVqmsExitAnnotation(VqmsExitAnnotation vqmsExitAnnotation);

    /**
     * 修改AVC退出原因标注
     * 
     * @param vqmsExitAnnotation AVC退出原因标注
     * @return 结果
     */
    public int updateVqmsExitAnnotation(VqmsExitAnnotation vqmsExitAnnotation);

    /**
     * 批量删除AVC退出原因标注
     * 
     * @param annotationIds 需要删除的AVC退出原因标注主键集合
     * @return 结果
     */
    public int deleteVqmsExitAnnotationByAnnotationIds(Long[] annotationIds);

    /**
     * 删除AVC退出原因标注信息
     * 
     * @param annotationId AVC退出原因标注主键
     * @return 结果
     */
    public int deleteVqmsExitAnnotationByAnnotationId(Long annotationId);
}

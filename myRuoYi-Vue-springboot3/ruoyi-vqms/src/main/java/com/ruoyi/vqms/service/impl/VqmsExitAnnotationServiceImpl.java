package com.ruoyi.vqms.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.vqms.mapper.VqmsExitAnnotationMapper;
import com.ruoyi.vqms.domain.VqmsExitAnnotation;
import com.ruoyi.vqms.service.IVqmsExitAnnotationService;

/**
 * AVC退出原因标注Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-09-02
 */
@Service
public class VqmsExitAnnotationServiceImpl implements IVqmsExitAnnotationService 
{
    @Autowired
    private VqmsExitAnnotationMapper vqmsExitAnnotationMapper;

    /**
     * 查询AVC退出原因标注
     * 
     * @param annotationId AVC退出原因标注主键
     * @return AVC退出原因标注
     */
    @Override
    public VqmsExitAnnotation selectVqmsExitAnnotationByAnnotationId(Long annotationId)
    {
        return vqmsExitAnnotationMapper.selectVqmsExitAnnotationByAnnotationId(annotationId);
    }

    /**
     * 查询AVC退出原因标注列表
     * 
     * @param vqmsExitAnnotation AVC退出原因标注
     * @return AVC退出原因标注
     */
    @Override
    public List<VqmsExitAnnotation> selectVqmsExitAnnotationList(VqmsExitAnnotation vqmsExitAnnotation)
    {
        return vqmsExitAnnotationMapper.selectVqmsExitAnnotationList(vqmsExitAnnotation);
    }

    /**
     * 新增AVC退出原因标注
     * 
     * @param vqmsExitAnnotation AVC退出原因标注
     * @return 结果
     */
    @Override
    public int insertVqmsExitAnnotation(VqmsExitAnnotation vqmsExitAnnotation)
    {
        vqmsExitAnnotation.setCreateTime(DateUtils.getNowDate());
        return vqmsExitAnnotationMapper.insertVqmsExitAnnotation(vqmsExitAnnotation);
    }

    /**
     * 修改AVC退出原因标注
     * 
     * @param vqmsExitAnnotation AVC退出原因标注
     * @return 结果
     */
    @Override
    public int updateVqmsExitAnnotation(VqmsExitAnnotation vqmsExitAnnotation)
    {
        vqmsExitAnnotation.setUpdateTime(DateUtils.getNowDate());
        return vqmsExitAnnotationMapper.updateVqmsExitAnnotation(vqmsExitAnnotation);
    }

    /**
     * 批量删除AVC退出原因标注
     * 
     * @param annotationIds 需要删除的AVC退出原因标注主键
     * @return 结果
     */
    @Override
    public int deleteVqmsExitAnnotationByAnnotationIds(Long[] annotationIds)
    {
        return vqmsExitAnnotationMapper.deleteVqmsExitAnnotationByAnnotationIds(annotationIds);
    }

    /**
     * 删除AVC退出原因标注信息
     * 
     * @param annotationId AVC退出原因标注主键
     * @return 结果
     */
    @Override
    public int deleteVqmsExitAnnotationByAnnotationId(Long annotationId)
    {
        return vqmsExitAnnotationMapper.deleteVqmsExitAnnotationByAnnotationId(annotationId);
    }
}

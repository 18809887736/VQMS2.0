package com.ruoyi.vqms.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.vqms.mapper.VqmsBusbarThresholdMapper;
import com.ruoyi.vqms.domain.VqmsBusbarThreshold;
import com.ruoyi.vqms.service.IVqmsBusbarThresholdService;

/**
 * 母线电压阈值Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-09-02
 */
@Service
public class VqmsBusbarThresholdServiceImpl implements IVqmsBusbarThresholdService 
{
    @Autowired
    private VqmsBusbarThresholdMapper vqmsBusbarThresholdMapper;

    /**
     * 查询母线电压阈值
     * 
     * @param thresholdId 母线电压阈值主键
     * @return 母线电压阈值
     */
    @Override
    public VqmsBusbarThreshold selectVqmsBusbarThresholdByThresholdId(Long thresholdId)
    {
        return vqmsBusbarThresholdMapper.selectVqmsBusbarThresholdByThresholdId(thresholdId);
    }

    /**
     * 查询母线电压阈值列表
     * 
     * @param vqmsBusbarThreshold 母线电压阈值
     * @return 母线电压阈值
     */
    @Override
    public List<VqmsBusbarThreshold> selectVqmsBusbarThresholdList(VqmsBusbarThreshold vqmsBusbarThreshold)
    {
        return vqmsBusbarThresholdMapper.selectVqmsBusbarThresholdList(vqmsBusbarThreshold);
    }

    /**
     * 新增母线电压阈值
     * 
     * @param vqmsBusbarThreshold 母线电压阈值
     * @return 结果
     */
    @Override
    public int insertVqmsBusbarThreshold(VqmsBusbarThreshold vqmsBusbarThreshold)
    {
        vqmsBusbarThreshold.setCreateTime(DateUtils.getNowDate());
        return vqmsBusbarThresholdMapper.insertVqmsBusbarThreshold(vqmsBusbarThreshold);
    }

    /**
     * 修改母线电压阈值
     * 
     * @param vqmsBusbarThreshold 母线电压阈值
     * @return 结果
     */
    @Override
    public int updateVqmsBusbarThreshold(VqmsBusbarThreshold vqmsBusbarThreshold)
    {
        vqmsBusbarThreshold.setUpdateTime(DateUtils.getNowDate());
        return vqmsBusbarThresholdMapper.updateVqmsBusbarThreshold(vqmsBusbarThreshold);
    }

    /**
     * 批量删除母线电压阈值
     * 
     * @param thresholdIds 需要删除的母线电压阈值主键
     * @return 结果
     */
    @Override
    public int deleteVqmsBusbarThresholdByThresholdIds(Long[] thresholdIds)
    {
        return vqmsBusbarThresholdMapper.deleteVqmsBusbarThresholdByThresholdIds(thresholdIds);
    }

    /**
     * 删除母线电压阈值信息
     * 
     * @param thresholdId 母线电压阈值主键
     * @return 结果
     */
    @Override
    public int deleteVqmsBusbarThresholdByThresholdId(Long thresholdId)
    {
        return vqmsBusbarThresholdMapper.deleteVqmsBusbarThresholdByThresholdId(thresholdId);
    }
}

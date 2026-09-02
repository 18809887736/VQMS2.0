package com.ruoyi.vqms.service;

import java.util.List;
import com.ruoyi.vqms.domain.VqmsBusbarThreshold;

/**
 * 母线电压阈值Service接口
 * 
 * @author ruoyi
 * @date 2026-09-02
 */
public interface IVqmsBusbarThresholdService 
{
    /**
     * 查询母线电压阈值
     * 
     * @param thresholdId 母线电压阈值主键
     * @return 母线电压阈值
     */
    public VqmsBusbarThreshold selectVqmsBusbarThresholdByThresholdId(Long thresholdId);

    /**
     * 查询母线电压阈值列表
     * 
     * @param vqmsBusbarThreshold 母线电压阈值
     * @return 母线电压阈值集合
     */
    public List<VqmsBusbarThreshold> selectVqmsBusbarThresholdList(VqmsBusbarThreshold vqmsBusbarThreshold);

    /**
     * 新增母线电压阈值
     * 
     * @param vqmsBusbarThreshold 母线电压阈值
     * @return 结果
     */
    public int insertVqmsBusbarThreshold(VqmsBusbarThreshold vqmsBusbarThreshold);

    /**
     * 修改母线电压阈值
     * 
     * @param vqmsBusbarThreshold 母线电压阈值
     * @return 结果
     */
    public int updateVqmsBusbarThreshold(VqmsBusbarThreshold vqmsBusbarThreshold);

    /**
     * 批量删除母线电压阈值
     * 
     * @param thresholdIds 需要删除的母线电压阈值主键集合
     * @return 结果
     */
    public int deleteVqmsBusbarThresholdByThresholdIds(Long[] thresholdIds);

    /**
     * 删除母线电压阈值信息
     * 
     * @param thresholdId 母线电压阈值主键
     * @return 结果
     */
    public int deleteVqmsBusbarThresholdByThresholdId(Long thresholdId);
}

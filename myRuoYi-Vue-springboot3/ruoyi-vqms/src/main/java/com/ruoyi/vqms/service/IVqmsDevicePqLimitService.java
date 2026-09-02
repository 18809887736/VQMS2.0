package com.ruoyi.vqms.service;

import java.util.List;
import com.ruoyi.vqms.domain.VqmsDevicePqLimit;

/**
 * 设备P-Q极限曲线Service接口
 * 
 * @author ruoyi
 * @date 2026-09-02
 */
public interface IVqmsDevicePqLimitService 
{
    /**
     * 查询设备P-Q极限曲线
     * 
     * @param id 设备P-Q极限曲线主键
     * @return 设备P-Q极限曲线
     */
    public VqmsDevicePqLimit selectVqmsDevicePqLimitById(Long id);

    /**
     * 查询设备P-Q极限曲线列表
     * 
     * @param vqmsDevicePqLimit 设备P-Q极限曲线
     * @return 设备P-Q极限曲线集合
     */
    public List<VqmsDevicePqLimit> selectVqmsDevicePqLimitList(VqmsDevicePqLimit vqmsDevicePqLimit);

    /**
     * 新增设备P-Q极限曲线
     * 
     * @param vqmsDevicePqLimit 设备P-Q极限曲线
     * @return 结果
     */
    public int insertVqmsDevicePqLimit(VqmsDevicePqLimit vqmsDevicePqLimit);

    /**
     * 修改设备P-Q极限曲线
     * 
     * @param vqmsDevicePqLimit 设备P-Q极限曲线
     * @return 结果
     */
    public int updateVqmsDevicePqLimit(VqmsDevicePqLimit vqmsDevicePqLimit);

    /**
     * 批量删除设备P-Q极限曲线
     * 
     * @param ids 需要删除的设备P-Q极限曲线主键集合
     * @return 结果
     */
    public int deleteVqmsDevicePqLimitByIds(Long[] ids);

    /**
     * 删除设备P-Q极限曲线信息
     * 
     * @param id 设备P-Q极限曲线主键
     * @return 结果
     */
    public int deleteVqmsDevicePqLimitById(Long id);
}

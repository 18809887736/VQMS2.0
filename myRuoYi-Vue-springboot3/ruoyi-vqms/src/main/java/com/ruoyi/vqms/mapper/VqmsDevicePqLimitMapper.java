package com.ruoyi.vqms.mapper;

import java.util.List;
import com.ruoyi.vqms.domain.VqmsDevicePqLimit;

/**
 * 设备P-Q极限曲线Mapper接口
 * 
 * @author ruoyi
 * @date 2026-09-02
 */
public interface VqmsDevicePqLimitMapper 
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
     * 删除设备P-Q极限曲线
     * 
     * @param id 设备P-Q极限曲线主键
     * @return 结果
     */
    public int deleteVqmsDevicePqLimitById(Long id);

    /**
     * 批量删除设备P-Q极限曲线
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteVqmsDevicePqLimitByIds(Long[] ids);
}

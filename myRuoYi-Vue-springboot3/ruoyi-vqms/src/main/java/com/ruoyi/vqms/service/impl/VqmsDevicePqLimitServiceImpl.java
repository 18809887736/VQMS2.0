package com.ruoyi.vqms.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.vqms.mapper.VqmsDevicePqLimitMapper;
import com.ruoyi.vqms.domain.VqmsDevicePqLimit;
import com.ruoyi.vqms.service.IVqmsDevicePqLimitService;

/**
 * 设备P-Q极限曲线Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-09-02
 */
@Service
public class VqmsDevicePqLimitServiceImpl implements IVqmsDevicePqLimitService 
{
    @Autowired
    private VqmsDevicePqLimitMapper vqmsDevicePqLimitMapper;

    /**
     * 查询设备P-Q极限曲线
     * 
     * @param id 设备P-Q极限曲线主键
     * @return 设备P-Q极限曲线
     */
    @Override
    public VqmsDevicePqLimit selectVqmsDevicePqLimitById(Long id)
    {
        return vqmsDevicePqLimitMapper.selectVqmsDevicePqLimitById(id);
    }

    /**
     * 查询设备P-Q极限曲线列表
     * 
     * @param vqmsDevicePqLimit 设备P-Q极限曲线
     * @return 设备P-Q极限曲线
     */
    @Override
    public List<VqmsDevicePqLimit> selectVqmsDevicePqLimitList(VqmsDevicePqLimit vqmsDevicePqLimit)
    {
        return vqmsDevicePqLimitMapper.selectVqmsDevicePqLimitList(vqmsDevicePqLimit);
    }

    /**
     * 新增设备P-Q极限曲线
     * 
     * @param vqmsDevicePqLimit 设备P-Q极限曲线
     * @return 结果
     */
    @Override
    public int insertVqmsDevicePqLimit(VqmsDevicePqLimit vqmsDevicePqLimit)
    {
        vqmsDevicePqLimit.setCreateTime(DateUtils.getNowDate());
        return vqmsDevicePqLimitMapper.insertVqmsDevicePqLimit(vqmsDevicePqLimit);
    }

    /**
     * 修改设备P-Q极限曲线
     * 
     * @param vqmsDevicePqLimit 设备P-Q极限曲线
     * @return 结果
     */
    @Override
    public int updateVqmsDevicePqLimit(VqmsDevicePqLimit vqmsDevicePqLimit)
    {
        vqmsDevicePqLimit.setUpdateTime(DateUtils.getNowDate());
        return vqmsDevicePqLimitMapper.updateVqmsDevicePqLimit(vqmsDevicePqLimit);
    }

    /**
     * 批量删除设备P-Q极限曲线
     * 
     * @param ids 需要删除的设备P-Q极限曲线主键
     * @return 结果
     */
    @Override
    public int deleteVqmsDevicePqLimitByIds(Long[] ids)
    {
        return vqmsDevicePqLimitMapper.deleteVqmsDevicePqLimitByIds(ids);
    }

    /**
     * 删除设备P-Q极限曲线信息
     * 
     * @param id 设备P-Q极限曲线主键
     * @return 结果
     */
    @Override
    public int deleteVqmsDevicePqLimitById(Long id)
    {
        return vqmsDevicePqLimitMapper.deleteVqmsDevicePqLimitById(id);
    }
}

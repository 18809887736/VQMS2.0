package com.ruoyi.vqms.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.vqms.mapper.VqmsReactiveDeviceMapper;
import com.ruoyi.vqms.domain.VqmsReactiveDevice;
import com.ruoyi.vqms.service.IVqmsReactiveDeviceService;

/**
 * 无功设备台账Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-09-02
 */
@Service
public class VqmsReactiveDeviceServiceImpl implements IVqmsReactiveDeviceService 
{
    @Autowired
    private VqmsReactiveDeviceMapper vqmsReactiveDeviceMapper;

    /**
     * 查询无功设备台账
     * 
     * @param deviceId 无功设备台账主键
     * @return 无功设备台账
     */
    @Override
    public VqmsReactiveDevice selectVqmsReactiveDeviceByDeviceId(Long deviceId)
    {
        return vqmsReactiveDeviceMapper.selectVqmsReactiveDeviceByDeviceId(deviceId);
    }

    /**
     * 查询无功设备台账列表
     * 
     * @param vqmsReactiveDevice 无功设备台账
     * @return 无功设备台账
     */
    @Override
    public List<VqmsReactiveDevice> selectVqmsReactiveDeviceList(VqmsReactiveDevice vqmsReactiveDevice)
    {
        return vqmsReactiveDeviceMapper.selectVqmsReactiveDeviceList(vqmsReactiveDevice);
    }

    /**
     * 新增无功设备台账
     * 
     * @param vqmsReactiveDevice 无功设备台账
     * @return 结果
     */
    @Override
    public int insertVqmsReactiveDevice(VqmsReactiveDevice vqmsReactiveDevice)
    {
        vqmsReactiveDevice.setCreateTime(DateUtils.getNowDate());
        return vqmsReactiveDeviceMapper.insertVqmsReactiveDevice(vqmsReactiveDevice);
    }

    /**
     * 修改无功设备台账
     * 
     * @param vqmsReactiveDevice 无功设备台账
     * @return 结果
     */
    @Override
    public int updateVqmsReactiveDevice(VqmsReactiveDevice vqmsReactiveDevice)
    {
        vqmsReactiveDevice.setUpdateTime(DateUtils.getNowDate());
        return vqmsReactiveDeviceMapper.updateVqmsReactiveDevice(vqmsReactiveDevice);
    }

    /**
     * 批量删除无功设备台账
     * 
     * @param deviceIds 需要删除的无功设备台账主键
     * @return 结果
     */
    @Override
    public int deleteVqmsReactiveDeviceByDeviceIds(Long[] deviceIds)
    {
        return vqmsReactiveDeviceMapper.deleteVqmsReactiveDeviceByDeviceIds(deviceIds);
    }

    /**
     * 删除无功设备台账信息
     * 
     * @param deviceId 无功设备台账主键
     * @return 结果
     */
    @Override
    public int deleteVqmsReactiveDeviceByDeviceId(Long deviceId)
    {
        return vqmsReactiveDeviceMapper.deleteVqmsReactiveDeviceByDeviceId(deviceId);
    }
}

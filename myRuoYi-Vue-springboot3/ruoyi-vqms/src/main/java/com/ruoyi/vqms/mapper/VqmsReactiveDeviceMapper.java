package com.ruoyi.vqms.mapper;

import java.util.List;
import com.ruoyi.vqms.domain.VqmsReactiveDevice;

/**
 * 无功设备台账Mapper接口
 * 
 * @author ruoyi
 * @date 2026-09-02
 */
public interface VqmsReactiveDeviceMapper 
{
    /**
     * 查询无功设备台账
     * 
     * @param deviceId 无功设备台账主键
     * @return 无功设备台账
     */
    public VqmsReactiveDevice selectVqmsReactiveDeviceByDeviceId(Long deviceId);

    /**
     * 查询无功设备台账列表
     * 
     * @param vqmsReactiveDevice 无功设备台账
     * @return 无功设备台账集合
     */
    public List<VqmsReactiveDevice> selectVqmsReactiveDeviceList(VqmsReactiveDevice vqmsReactiveDevice);

    /**
     * 新增无功设备台账
     * 
     * @param vqmsReactiveDevice 无功设备台账
     * @return 结果
     */
    public int insertVqmsReactiveDevice(VqmsReactiveDevice vqmsReactiveDevice);

    /**
     * 修改无功设备台账
     * 
     * @param vqmsReactiveDevice 无功设备台账
     * @return 结果
     */
    public int updateVqmsReactiveDevice(VqmsReactiveDevice vqmsReactiveDevice);

    /**
     * 删除无功设备台账
     * 
     * @param deviceId 无功设备台账主键
     * @return 结果
     */
    public int deleteVqmsReactiveDeviceByDeviceId(Long deviceId);

    /**
     * 批量删除无功设备台账
     * 
     * @param deviceIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteVqmsReactiveDeviceByDeviceIds(Long[] deviceIds);
}

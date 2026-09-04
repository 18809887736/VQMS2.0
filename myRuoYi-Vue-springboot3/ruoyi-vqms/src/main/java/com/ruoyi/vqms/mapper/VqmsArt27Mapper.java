package com.ruoyi.vqms.mapper;

import java.util.List;
import com.ruoyi.vqms.domain.VqmsArt27Device;
import com.ruoyi.vqms.domain.VqmsArt27Month;

public interface VqmsArt27Mapper {

    List<VqmsArt27Device> selectDevices(VqmsArt27Device q);

    VqmsArt27Device selectDeviceById(Long deviceId);

    int insertDevice(VqmsArt27Device d);

    int updateDevice(VqmsArt27Device d);

    int deleteDeviceByIds(Long[] deviceIds);

    List<VqmsArt27Month> selectMonths(String statMonth);

    VqmsArt27Month selectMonthById(Long id);

    int insertMonth(VqmsArt27Month m);

    int updateMonth(VqmsArt27Month m);

    int deleteMonthByIds(Long[] ids);
}

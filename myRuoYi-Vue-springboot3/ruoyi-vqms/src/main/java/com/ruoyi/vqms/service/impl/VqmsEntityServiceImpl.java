package com.ruoyi.vqms.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.vqms.domain.VqmsEntity;
import com.ruoyi.vqms.mapper.VqmsEntityMapper;
import com.ruoyi.vqms.service.IVqmsEntityService;

/**
 * 并网主体 服务层实现
 *
 * @author vqms
 */
@Service
public class VqmsEntityServiceImpl implements IVqmsEntityService
{
    @Autowired
    private VqmsEntityMapper vqmsEntityMapper;

    @Override
    public List<VqmsEntity> selectVqmsEntityList(VqmsEntity vqmsEntity)
    {
        return vqmsEntityMapper.selectVqmsEntityList(vqmsEntity);
    }

    @Override
    public VqmsEntity selectVqmsEntityById(Long entityId)
    {
        return vqmsEntityMapper.selectVqmsEntityById(entityId);
    }

    @Override
    public int insertVqmsEntity(VqmsEntity vqmsEntity)
    {
        vqmsEntity.setCreateTime(DateUtils.getNowDate());
        return vqmsEntityMapper.insertVqmsEntity(vqmsEntity);
    }

    @Override
    public int updateVqmsEntity(VqmsEntity vqmsEntity)
    {
        vqmsEntity.setUpdateTime(DateUtils.getNowDate());
        return vqmsEntityMapper.updateVqmsEntity(vqmsEntity);
    }

    @Override
    public int deleteVqmsEntityByIds(Long[] entityIds)
    {
        return vqmsEntityMapper.deleteVqmsEntityByIds(entityIds);
    }
}

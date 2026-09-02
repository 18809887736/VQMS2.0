package com.ruoyi.vqms.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.vqms.mapper.VqmsYcPointMapMapper;
import com.ruoyi.vqms.domain.VqmsYcPointMap;
import com.ruoyi.vqms.service.IVqmsYcPointMapService;

/**
 * 点号语义注册Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-09-02
 */
@Service
public class VqmsYcPointMapServiceImpl implements IVqmsYcPointMapService 
{
    @Autowired
    private VqmsYcPointMapMapper vqmsYcPointMapMapper;

    /**
     * 查询点号语义注册
     * 
     * @param pointNum 点号语义注册主键
     * @return 点号语义注册
     */
    @Override
    public VqmsYcPointMap selectVqmsYcPointMapByPointNum(Long pointNum)
    {
        return vqmsYcPointMapMapper.selectVqmsYcPointMapByPointNum(pointNum);
    }

    /**
     * 查询点号语义注册列表
     * 
     * @param vqmsYcPointMap 点号语义注册
     * @return 点号语义注册
     */
    @Override
    public List<VqmsYcPointMap> selectVqmsYcPointMapList(VqmsYcPointMap vqmsYcPointMap)
    {
        return vqmsYcPointMapMapper.selectVqmsYcPointMapList(vqmsYcPointMap);
    }

    /**
     * 新增点号语义注册
     * 
     * @param vqmsYcPointMap 点号语义注册
     * @return 结果
     */
    @Override
    public int insertVqmsYcPointMap(VqmsYcPointMap vqmsYcPointMap)
    {
        vqmsYcPointMap.setCreateTime(DateUtils.getNowDate());
        return vqmsYcPointMapMapper.insertVqmsYcPointMap(vqmsYcPointMap);
    }

    /**
     * 修改点号语义注册
     * 
     * @param vqmsYcPointMap 点号语义注册
     * @return 结果
     */
    @Override
    public int updateVqmsYcPointMap(VqmsYcPointMap vqmsYcPointMap)
    {
        vqmsYcPointMap.setUpdateTime(DateUtils.getNowDate());
        return vqmsYcPointMapMapper.updateVqmsYcPointMap(vqmsYcPointMap);
    }

    /**
     * 批量删除点号语义注册
     * 
     * @param pointNums 需要删除的点号语义注册主键
     * @return 结果
     */
    @Override
    public int deleteVqmsYcPointMapByPointNums(Long[] pointNums)
    {
        return vqmsYcPointMapMapper.deleteVqmsYcPointMapByPointNums(pointNums);
    }

    /**
     * 删除点号语义注册信息
     * 
     * @param pointNum 点号语义注册主键
     * @return 结果
     */
    @Override
    public int deleteVqmsYcPointMapByPointNum(Long pointNum)
    {
        return vqmsYcPointMapMapper.deleteVqmsYcPointMapByPointNum(pointNum);
    }
}

package com.ruoyi.vqms.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.vqms.mapper.VqmsPolicyParamMapper;
import com.ruoyi.vqms.domain.VqmsPolicyParam;
import com.ruoyi.vqms.service.IVqmsPolicyParamService;

/**
 * 数据不可用策略参数Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-09-02
 */
@Service
public class VqmsPolicyParamServiceImpl implements IVqmsPolicyParamService 
{
    @Autowired
    private VqmsPolicyParamMapper vqmsPolicyParamMapper;

    /**
     * 查询数据不可用策略参数
     * 
     * @param paramId 数据不可用策略参数主键
     * @return 数据不可用策略参数
     */
    @Override
    public VqmsPolicyParam selectVqmsPolicyParamByParamId(Long paramId)
    {
        return vqmsPolicyParamMapper.selectVqmsPolicyParamByParamId(paramId);
    }

    /**
     * 查询数据不可用策略参数列表
     * 
     * @param vqmsPolicyParam 数据不可用策略参数
     * @return 数据不可用策略参数
     */
    @Override
    public List<VqmsPolicyParam> selectVqmsPolicyParamList(VqmsPolicyParam vqmsPolicyParam)
    {
        return vqmsPolicyParamMapper.selectVqmsPolicyParamList(vqmsPolicyParam);
    }

    /**
     * 新增数据不可用策略参数
     * 
     * @param vqmsPolicyParam 数据不可用策略参数
     * @return 结果
     */
    @Override
    public int insertVqmsPolicyParam(VqmsPolicyParam vqmsPolicyParam)
    {
        vqmsPolicyParam.setCreateTime(DateUtils.getNowDate());
        return vqmsPolicyParamMapper.insertVqmsPolicyParam(vqmsPolicyParam);
    }

    /**
     * 修改数据不可用策略参数
     * 
     * @param vqmsPolicyParam 数据不可用策略参数
     * @return 结果
     */
    @Override
    public int updateVqmsPolicyParam(VqmsPolicyParam vqmsPolicyParam)
    {
        vqmsPolicyParam.setUpdateTime(DateUtils.getNowDate());
        return vqmsPolicyParamMapper.updateVqmsPolicyParam(vqmsPolicyParam);
    }

    /**
     * 批量删除数据不可用策略参数
     * 
     * @param paramIds 需要删除的数据不可用策略参数主键
     * @return 结果
     */
    @Override
    public int deleteVqmsPolicyParamByParamIds(Long[] paramIds)
    {
        return vqmsPolicyParamMapper.deleteVqmsPolicyParamByParamIds(paramIds);
    }

    /**
     * 删除数据不可用策略参数信息
     * 
     * @param paramId 数据不可用策略参数主键
     * @return 结果
     */
    @Override
    public int deleteVqmsPolicyParamByParamId(Long paramId)
    {
        return vqmsPolicyParamMapper.deleteVqmsPolicyParamByParamId(paramId);
    }
}

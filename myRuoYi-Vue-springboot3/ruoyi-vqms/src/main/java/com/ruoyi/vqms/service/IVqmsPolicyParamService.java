package com.ruoyi.vqms.service;

import java.util.List;
import com.ruoyi.vqms.domain.VqmsPolicyParam;

/**
 * 数据不可用策略参数Service接口
 * 
 * @author ruoyi
 * @date 2026-09-02
 */
public interface IVqmsPolicyParamService 
{
    /**
     * 查询数据不可用策略参数
     * 
     * @param paramId 数据不可用策略参数主键
     * @return 数据不可用策略参数
     */
    public VqmsPolicyParam selectVqmsPolicyParamByParamId(Long paramId);

    /**
     * 查询数据不可用策略参数列表
     * 
     * @param vqmsPolicyParam 数据不可用策略参数
     * @return 数据不可用策略参数集合
     */
    public List<VqmsPolicyParam> selectVqmsPolicyParamList(VqmsPolicyParam vqmsPolicyParam);

    /**
     * 新增数据不可用策略参数
     * 
     * @param vqmsPolicyParam 数据不可用策略参数
     * @return 结果
     */
    public int insertVqmsPolicyParam(VqmsPolicyParam vqmsPolicyParam);

    /**
     * 修改数据不可用策略参数
     * 
     * @param vqmsPolicyParam 数据不可用策略参数
     * @return 结果
     */
    public int updateVqmsPolicyParam(VqmsPolicyParam vqmsPolicyParam);

    /**
     * 批量删除数据不可用策略参数
     * 
     * @param paramIds 需要删除的数据不可用策略参数主键集合
     * @return 结果
     */
    public int deleteVqmsPolicyParamByParamIds(Long[] paramIds);

    /**
     * 删除数据不可用策略参数信息
     * 
     * @param paramId 数据不可用策略参数主键
     * @return 结果
     */
    public int deleteVqmsPolicyParamByParamId(Long paramId);
}

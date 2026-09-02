package com.ruoyi.vqms.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.vqms.mapper.VqmsBusbarGroupMapper;
import com.ruoyi.vqms.domain.VqmsBusbarGroup;
import com.ruoyi.vqms.service.IVqmsBusbarGroupService;

/**
 * 母线组Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-09-02
 */
@Service
public class VqmsBusbarGroupServiceImpl implements IVqmsBusbarGroupService 
{
    @Autowired
    private VqmsBusbarGroupMapper vqmsBusbarGroupMapper;

    /**
     * 查询母线组
     * 
     * @param groupNum 母线组主键
     * @return 母线组
     */
    @Override
    public VqmsBusbarGroup selectVqmsBusbarGroupByGroupNum(Long groupNum)
    {
        return vqmsBusbarGroupMapper.selectVqmsBusbarGroupByGroupNum(groupNum);
    }

    /**
     * 查询母线组列表
     * 
     * @param vqmsBusbarGroup 母线组
     * @return 母线组
     */
    @Override
    public List<VqmsBusbarGroup> selectVqmsBusbarGroupList(VqmsBusbarGroup vqmsBusbarGroup)
    {
        return vqmsBusbarGroupMapper.selectVqmsBusbarGroupList(vqmsBusbarGroup);
    }

    /**
     * 新增母线组
     * 
     * @param vqmsBusbarGroup 母线组
     * @return 结果
     */
    @Override
    public int insertVqmsBusbarGroup(VqmsBusbarGroup vqmsBusbarGroup)
    {
        vqmsBusbarGroup.setCreateTime(DateUtils.getNowDate());
        return vqmsBusbarGroupMapper.insertVqmsBusbarGroup(vqmsBusbarGroup);
    }

    /**
     * 修改母线组
     * 
     * @param vqmsBusbarGroup 母线组
     * @return 结果
     */
    @Override
    public int updateVqmsBusbarGroup(VqmsBusbarGroup vqmsBusbarGroup)
    {
        vqmsBusbarGroup.setUpdateTime(DateUtils.getNowDate());
        return vqmsBusbarGroupMapper.updateVqmsBusbarGroup(vqmsBusbarGroup);
    }

    /**
     * 批量删除母线组
     * 
     * @param groupNums 需要删除的母线组主键
     * @return 结果
     */
    @Override
    public int deleteVqmsBusbarGroupByGroupNums(Long[] groupNums)
    {
        return vqmsBusbarGroupMapper.deleteVqmsBusbarGroupByGroupNums(groupNums);
    }

    /**
     * 删除母线组信息
     * 
     * @param groupNum 母线组主键
     * @return 结果
     */
    @Override
    public int deleteVqmsBusbarGroupByGroupNum(Long groupNum)
    {
        return vqmsBusbarGroupMapper.deleteVqmsBusbarGroupByGroupNum(groupNum);
    }
}

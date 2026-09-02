package com.ruoyi.vqms.mapper;

import java.util.List;
import com.ruoyi.vqms.domain.VqmsBusbarGroup;

/**
 * 母线组Mapper接口
 * 
 * @author ruoyi
 * @date 2026-09-02
 */
public interface VqmsBusbarGroupMapper 
{
    /**
     * 查询母线组
     * 
     * @param groupNum 母线组主键
     * @return 母线组
     */
    public VqmsBusbarGroup selectVqmsBusbarGroupByGroupNum(Long groupNum);

    /**
     * 查询母线组列表
     * 
     * @param vqmsBusbarGroup 母线组
     * @return 母线组集合
     */
    public List<VqmsBusbarGroup> selectVqmsBusbarGroupList(VqmsBusbarGroup vqmsBusbarGroup);

    /**
     * 新增母线组
     * 
     * @param vqmsBusbarGroup 母线组
     * @return 结果
     */
    public int insertVqmsBusbarGroup(VqmsBusbarGroup vqmsBusbarGroup);

    /**
     * 修改母线组
     * 
     * @param vqmsBusbarGroup 母线组
     * @return 结果
     */
    public int updateVqmsBusbarGroup(VqmsBusbarGroup vqmsBusbarGroup);

    /**
     * 删除母线组
     * 
     * @param groupNum 母线组主键
     * @return 结果
     */
    public int deleteVqmsBusbarGroupByGroupNum(Long groupNum);

    /**
     * 批量删除母线组
     * 
     * @param groupNums 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteVqmsBusbarGroupByGroupNums(Long[] groupNums);
}

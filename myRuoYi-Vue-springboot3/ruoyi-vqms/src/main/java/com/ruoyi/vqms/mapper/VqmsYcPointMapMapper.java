package com.ruoyi.vqms.mapper;

import java.util.List;
import com.ruoyi.vqms.domain.VqmsYcPointMap;

/**
 * 点号语义注册Mapper接口
 * 
 * @author ruoyi
 * @date 2026-09-02
 */
public interface VqmsYcPointMapMapper 
{
    /**
     * 查询点号语义注册
     * 
     * @param pointNum 点号语义注册主键
     * @return 点号语义注册
     */
    public VqmsYcPointMap selectVqmsYcPointMapByPointNum(Long pointNum);

    /**
     * 查询点号语义注册列表
     * 
     * @param vqmsYcPointMap 点号语义注册
     * @return 点号语义注册集合
     */
    public List<VqmsYcPointMap> selectVqmsYcPointMapList(VqmsYcPointMap vqmsYcPointMap);

    /**
     * 新增点号语义注册
     * 
     * @param vqmsYcPointMap 点号语义注册
     * @return 结果
     */
    public int insertVqmsYcPointMap(VqmsYcPointMap vqmsYcPointMap);

    /**
     * 修改点号语义注册
     * 
     * @param vqmsYcPointMap 点号语义注册
     * @return 结果
     */
    public int updateVqmsYcPointMap(VqmsYcPointMap vqmsYcPointMap);

    /**
     * 删除点号语义注册
     * 
     * @param pointNum 点号语义注册主键
     * @return 结果
     */
    public int deleteVqmsYcPointMapByPointNum(Long pointNum);

    /**
     * 批量删除点号语义注册
     * 
     * @param pointNums 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteVqmsYcPointMapByPointNums(Long[] pointNums);
}

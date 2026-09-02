package com.ruoyi.vqms.mapper;

import java.util.List;
import com.ruoyi.vqms.domain.VqmsEntity;

/**
 * 并网主体 数据层
 *
 * @author vqms
 */
public interface VqmsEntityMapper
{
    /**
     * 查询并网主体列表
     *
     * @param vqmsEntity 并网主体
     * @return 并网主体集合
     */
    public List<VqmsEntity> selectVqmsEntityList(VqmsEntity vqmsEntity);

    /**
     * 根据主体ID查询并网主体
     *
     * @param entityId 并网主体ID
     * @return 并网主体
     */
    public VqmsEntity selectVqmsEntityById(Long entityId);

    /**
     * 新增并网主体
     *
     * @param vqmsEntity 并网主体
     * @return 结果
     */
    public int insertVqmsEntity(VqmsEntity vqmsEntity);

    /**
     * 修改并网主体
     *
     * @param vqmsEntity 并网主体
     * @return 结果
     */
    public int updateVqmsEntity(VqmsEntity vqmsEntity);

    /**
     * 删除并网主体
     *
     * @param entityId 并网主体ID
     * @return 结果
     */
    public int deleteVqmsEntityById(Long entityId);

    /**
     * 批量删除并网主体
     *
     * @param entityIds 需要删除的数据ID
     * @return 结果
     */
    public int deleteVqmsEntityByIds(Long[] entityIds);
}

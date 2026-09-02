package com.ruoyi.vqms.service;

import java.util.List;
import com.ruoyi.vqms.domain.VqmsBusbar;

/**
 * 主母线台账Service接口
 * 
 * @author ruoyi
 * @date 2026-09-02
 */
public interface IVqmsBusbarService 
{
    /**
     * 查询主母线台账
     * 
     * @param busbarNum 主母线台账主键
     * @return 主母线台账
     */
    public VqmsBusbar selectVqmsBusbarByBusbarNum(Long busbarNum);

    /**
     * 查询主母线台账列表
     * 
     * @param vqmsBusbar 主母线台账
     * @return 主母线台账集合
     */
    public List<VqmsBusbar> selectVqmsBusbarList(VqmsBusbar vqmsBusbar);

    /**
     * 新增主母线台账
     * 
     * @param vqmsBusbar 主母线台账
     * @return 结果
     */
    public int insertVqmsBusbar(VqmsBusbar vqmsBusbar);

    /**
     * 修改主母线台账
     * 
     * @param vqmsBusbar 主母线台账
     * @return 结果
     */
    public int updateVqmsBusbar(VqmsBusbar vqmsBusbar);

    /**
     * 批量删除主母线台账
     * 
     * @param busbarNums 需要删除的主母线台账主键集合
     * @return 结果
     */
    public int deleteVqmsBusbarByBusbarNums(Long[] busbarNums);

    /**
     * 删除主母线台账信息
     * 
     * @param busbarNum 主母线台账主键
     * @return 结果
     */
    public int deleteVqmsBusbarByBusbarNum(Long busbarNum);
}

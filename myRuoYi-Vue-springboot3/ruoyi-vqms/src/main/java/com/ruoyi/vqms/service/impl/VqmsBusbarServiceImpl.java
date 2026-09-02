package com.ruoyi.vqms.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.vqms.mapper.VqmsBusbarMapper;
import com.ruoyi.vqms.domain.VqmsBusbar;
import com.ruoyi.vqms.service.IVqmsBusbarService;

/**
 * 主母线台账Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-09-02
 */
@Service
public class VqmsBusbarServiceImpl implements IVqmsBusbarService 
{
    @Autowired
    private VqmsBusbarMapper vqmsBusbarMapper;

    /**
     * 查询主母线台账
     * 
     * @param busbarNum 主母线台账主键
     * @return 主母线台账
     */
    @Override
    public VqmsBusbar selectVqmsBusbarByBusbarNum(Long busbarNum)
    {
        return vqmsBusbarMapper.selectVqmsBusbarByBusbarNum(busbarNum);
    }

    /**
     * 查询主母线台账列表
     * 
     * @param vqmsBusbar 主母线台账
     * @return 主母线台账
     */
    @Override
    public List<VqmsBusbar> selectVqmsBusbarList(VqmsBusbar vqmsBusbar)
    {
        return vqmsBusbarMapper.selectVqmsBusbarList(vqmsBusbar);
    }

    /**
     * 新增主母线台账
     * 
     * @param vqmsBusbar 主母线台账
     * @return 结果
     */
    @Override
    public int insertVqmsBusbar(VqmsBusbar vqmsBusbar)
    {
        vqmsBusbar.setCreateTime(DateUtils.getNowDate());
        return vqmsBusbarMapper.insertVqmsBusbar(vqmsBusbar);
    }

    /**
     * 修改主母线台账
     * 
     * @param vqmsBusbar 主母线台账
     * @return 结果
     */
    @Override
    public int updateVqmsBusbar(VqmsBusbar vqmsBusbar)
    {
        vqmsBusbar.setUpdateTime(DateUtils.getNowDate());
        return vqmsBusbarMapper.updateVqmsBusbar(vqmsBusbar);
    }

    /**
     * 批量删除主母线台账
     * 
     * @param busbarNums 需要删除的主母线台账主键
     * @return 结果
     */
    @Override
    public int deleteVqmsBusbarByBusbarNums(Long[] busbarNums)
    {
        return vqmsBusbarMapper.deleteVqmsBusbarByBusbarNums(busbarNums);
    }

    /**
     * 删除主母线台账信息
     * 
     * @param busbarNum 主母线台账主键
     * @return 结果
     */
    @Override
    public int deleteVqmsBusbarByBusbarNum(Long busbarNum)
    {
        return vqmsBusbarMapper.deleteVqmsBusbarByBusbarNum(busbarNum);
    }
}

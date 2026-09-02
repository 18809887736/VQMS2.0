package com.ruoyi.vqms.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.vqms.mapper.VqmsJudgeParamMapper;
import com.ruoyi.vqms.domain.VqmsJudgeParam;
import com.ruoyi.vqms.service.IVqmsJudgeParamService;

/**
 * 判定整定参数Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-09-02
 */
@Service
public class VqmsJudgeParamServiceImpl implements IVqmsJudgeParamService 
{
    @Autowired
    private VqmsJudgeParamMapper vqmsJudgeParamMapper;

    /**
     * 查询判定整定参数
     * 
     * @param paramId 判定整定参数主键
     * @return 判定整定参数
     */
    @Override
    public VqmsJudgeParam selectVqmsJudgeParamByParamId(Long paramId)
    {
        return vqmsJudgeParamMapper.selectVqmsJudgeParamByParamId(paramId);
    }

    /**
     * 查询判定整定参数列表
     * 
     * @param vqmsJudgeParam 判定整定参数
     * @return 判定整定参数
     */
    @Override
    public List<VqmsJudgeParam> selectVqmsJudgeParamList(VqmsJudgeParam vqmsJudgeParam)
    {
        return vqmsJudgeParamMapper.selectVqmsJudgeParamList(vqmsJudgeParam);
    }

    /**
     * 新增判定整定参数
     * 
     * @param vqmsJudgeParam 判定整定参数
     * @return 结果
     */
    @Override
    public int insertVqmsJudgeParam(VqmsJudgeParam vqmsJudgeParam)
    {
        vqmsJudgeParam.setCreateTime(DateUtils.getNowDate());
        return vqmsJudgeParamMapper.insertVqmsJudgeParam(vqmsJudgeParam);
    }

    /**
     * 修改判定整定参数
     * 
     * @param vqmsJudgeParam 判定整定参数
     * @return 结果
     */
    @Override
    public int updateVqmsJudgeParam(VqmsJudgeParam vqmsJudgeParam)
    {
        vqmsJudgeParam.setUpdateTime(DateUtils.getNowDate());
        return vqmsJudgeParamMapper.updateVqmsJudgeParam(vqmsJudgeParam);
    }

    /**
     * 批量删除判定整定参数
     * 
     * @param paramIds 需要删除的判定整定参数主键
     * @return 结果
     */
    @Override
    public int deleteVqmsJudgeParamByParamIds(Long[] paramIds)
    {
        return vqmsJudgeParamMapper.deleteVqmsJudgeParamByParamIds(paramIds);
    }

    /**
     * 删除判定整定参数信息
     * 
     * @param paramId 判定整定参数主键
     * @return 结果
     */
    @Override
    public int deleteVqmsJudgeParamByParamId(Long paramId)
    {
        return vqmsJudgeParamMapper.deleteVqmsJudgeParamByParamId(paramId);
    }
}

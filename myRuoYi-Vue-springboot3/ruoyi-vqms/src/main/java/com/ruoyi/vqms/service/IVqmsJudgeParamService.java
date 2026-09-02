package com.ruoyi.vqms.service;

import java.util.List;
import com.ruoyi.vqms.domain.VqmsJudgeParam;

/**
 * 判定整定参数Service接口
 * 
 * @author ruoyi
 * @date 2026-09-02
 */
public interface IVqmsJudgeParamService 
{
    /**
     * 查询判定整定参数
     * 
     * @param paramId 判定整定参数主键
     * @return 判定整定参数
     */
    public VqmsJudgeParam selectVqmsJudgeParamByParamId(Long paramId);

    /**
     * 查询判定整定参数列表
     * 
     * @param vqmsJudgeParam 判定整定参数
     * @return 判定整定参数集合
     */
    public List<VqmsJudgeParam> selectVqmsJudgeParamList(VqmsJudgeParam vqmsJudgeParam);

    /**
     * 新增判定整定参数
     * 
     * @param vqmsJudgeParam 判定整定参数
     * @return 结果
     */
    public int insertVqmsJudgeParam(VqmsJudgeParam vqmsJudgeParam);

    /**
     * 修改判定整定参数
     * 
     * @param vqmsJudgeParam 判定整定参数
     * @return 结果
     */
    public int updateVqmsJudgeParam(VqmsJudgeParam vqmsJudgeParam);

    /**
     * 批量删除判定整定参数
     * 
     * @param paramIds 需要删除的判定整定参数主键集合
     * @return 结果
     */
    public int deleteVqmsJudgeParamByParamIds(Long[] paramIds);

    /**
     * 删除判定整定参数信息
     * 
     * @param paramId 判定整定参数主键
     * @return 结果
     */
    public int deleteVqmsJudgeParamByParamId(Long paramId);
}

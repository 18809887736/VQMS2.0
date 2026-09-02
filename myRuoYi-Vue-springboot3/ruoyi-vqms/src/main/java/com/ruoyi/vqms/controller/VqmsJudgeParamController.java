package com.ruoyi.vqms.controller;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.vqms.domain.VqmsJudgeParam;
import com.ruoyi.vqms.service.IVqmsJudgeParamService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 判定整定参数Controller
 * 
 * @author ruoyi
 * @date 2026-09-02
 */
@RestController
@RequestMapping("/vqms/judgeParam")
public class VqmsJudgeParamController extends BaseController
{
    @Autowired
    private IVqmsJudgeParamService vqmsJudgeParamService;

    /**
     * 查询判定整定参数列表
     */
    @PreAuthorize("@ss.hasPermi('vqms:judgeParam:list')")
    @GetMapping("/list")
    public TableDataInfo list(VqmsJudgeParam vqmsJudgeParam)
    {
        startPage();
        List<VqmsJudgeParam> list = vqmsJudgeParamService.selectVqmsJudgeParamList(vqmsJudgeParam);
        return getDataTable(list);
    }

    /**
     * 导出判定整定参数列表
     */
    @PreAuthorize("@ss.hasPermi('vqms:judgeParam:export')")
    @Log(title = "判定整定参数", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, VqmsJudgeParam vqmsJudgeParam)
    {
        List<VqmsJudgeParam> list = vqmsJudgeParamService.selectVqmsJudgeParamList(vqmsJudgeParam);
        ExcelUtil<VqmsJudgeParam> util = new ExcelUtil<VqmsJudgeParam>(VqmsJudgeParam.class);
        util.exportExcel(response, list, "判定整定参数数据");
    }

    /**
     * 获取判定整定参数详细信息
     */
    @PreAuthorize("@ss.hasPermi('vqms:judgeParam:query')")
    @GetMapping(value = "/{paramId}")
    public AjaxResult getInfo(@PathVariable("paramId") Long paramId)
    {
        return success(vqmsJudgeParamService.selectVqmsJudgeParamByParamId(paramId));
    }

    /**
     * 新增判定整定参数
     */
    @PreAuthorize("@ss.hasPermi('vqms:judgeParam:add')")
    @Log(title = "判定整定参数", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody VqmsJudgeParam vqmsJudgeParam)
    {
        return toAjax(vqmsJudgeParamService.insertVqmsJudgeParam(vqmsJudgeParam));
    }

    /**
     * 修改判定整定参数
     */
    @PreAuthorize("@ss.hasPermi('vqms:judgeParam:edit')")
    @Log(title = "判定整定参数", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody VqmsJudgeParam vqmsJudgeParam)
    {
        return toAjax(vqmsJudgeParamService.updateVqmsJudgeParam(vqmsJudgeParam));
    }

    /**
     * 删除判定整定参数
     */
    @PreAuthorize("@ss.hasPermi('vqms:judgeParam:remove')")
    @Log(title = "判定整定参数", businessType = BusinessType.DELETE)
	@DeleteMapping("/{paramIds}")
    public AjaxResult remove(@PathVariable Long[] paramIds)
    {
        return toAjax(vqmsJudgeParamService.deleteVqmsJudgeParamByParamIds(paramIds));
    }
}

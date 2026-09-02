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
import com.ruoyi.vqms.domain.VqmsPolicyParam;
import com.ruoyi.vqms.service.IVqmsPolicyParamService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 数据不可用策略参数Controller
 * 
 * @author ruoyi
 * @date 2026-09-02
 */
@RestController
@RequestMapping("/vqms/policyParam")
public class VqmsPolicyParamController extends BaseController
{
    @Autowired
    private IVqmsPolicyParamService vqmsPolicyParamService;

    /**
     * 查询数据不可用策略参数列表
     */
    @PreAuthorize("@ss.hasPermi('vqms:policyParam:list')")
    @GetMapping("/list")
    public TableDataInfo list(VqmsPolicyParam vqmsPolicyParam)
    {
        startPage();
        List<VqmsPolicyParam> list = vqmsPolicyParamService.selectVqmsPolicyParamList(vqmsPolicyParam);
        return getDataTable(list);
    }

    /**
     * 导出数据不可用策略参数列表
     */
    @PreAuthorize("@ss.hasPermi('vqms:policyParam:export')")
    @Log(title = "数据不可用策略参数", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, VqmsPolicyParam vqmsPolicyParam)
    {
        List<VqmsPolicyParam> list = vqmsPolicyParamService.selectVqmsPolicyParamList(vqmsPolicyParam);
        ExcelUtil<VqmsPolicyParam> util = new ExcelUtil<VqmsPolicyParam>(VqmsPolicyParam.class);
        util.exportExcel(response, list, "数据不可用策略参数数据");
    }

    /**
     * 获取数据不可用策略参数详细信息
     */
    @PreAuthorize("@ss.hasPermi('vqms:policyParam:query')")
    @GetMapping(value = "/{paramId}")
    public AjaxResult getInfo(@PathVariable("paramId") Long paramId)
    {
        return success(vqmsPolicyParamService.selectVqmsPolicyParamByParamId(paramId));
    }

    /**
     * 新增数据不可用策略参数
     */
    @PreAuthorize("@ss.hasPermi('vqms:policyParam:add')")
    @Log(title = "数据不可用策略参数", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody VqmsPolicyParam vqmsPolicyParam)
    {
        return toAjax(vqmsPolicyParamService.insertVqmsPolicyParam(vqmsPolicyParam));
    }

    /**
     * 修改数据不可用策略参数
     */
    @PreAuthorize("@ss.hasPermi('vqms:policyParam:edit')")
    @Log(title = "数据不可用策略参数", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody VqmsPolicyParam vqmsPolicyParam)
    {
        return toAjax(vqmsPolicyParamService.updateVqmsPolicyParam(vqmsPolicyParam));
    }

    /**
     * 删除数据不可用策略参数
     */
    @PreAuthorize("@ss.hasPermi('vqms:policyParam:remove')")
    @Log(title = "数据不可用策略参数", businessType = BusinessType.DELETE)
	@DeleteMapping("/{paramIds}")
    public AjaxResult remove(@PathVariable Long[] paramIds)
    {
        return toAjax(vqmsPolicyParamService.deleteVqmsPolicyParamByParamIds(paramIds));
    }
}

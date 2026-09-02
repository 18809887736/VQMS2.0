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
import com.ruoyi.vqms.domain.VqmsBusbarThreshold;
import com.ruoyi.vqms.service.IVqmsBusbarThresholdService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 母线电压阈值Controller
 * 
 * @author ruoyi
 * @date 2026-09-02
 */
@RestController
@RequestMapping("/vqms/threshold")
public class VqmsBusbarThresholdController extends BaseController
{
    @Autowired
    private IVqmsBusbarThresholdService vqmsBusbarThresholdService;

    /**
     * 查询母线电压阈值列表
     */
    @PreAuthorize("@ss.hasPermi('vqms:threshold:list')")
    @GetMapping("/list")
    public TableDataInfo list(VqmsBusbarThreshold vqmsBusbarThreshold)
    {
        startPage();
        List<VqmsBusbarThreshold> list = vqmsBusbarThresholdService.selectVqmsBusbarThresholdList(vqmsBusbarThreshold);
        return getDataTable(list);
    }

    /**
     * 导出母线电压阈值列表
     */
    @PreAuthorize("@ss.hasPermi('vqms:threshold:export')")
    @Log(title = "母线电压阈值", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, VqmsBusbarThreshold vqmsBusbarThreshold)
    {
        List<VqmsBusbarThreshold> list = vqmsBusbarThresholdService.selectVqmsBusbarThresholdList(vqmsBusbarThreshold);
        ExcelUtil<VqmsBusbarThreshold> util = new ExcelUtil<VqmsBusbarThreshold>(VqmsBusbarThreshold.class);
        util.exportExcel(response, list, "母线电压阈值数据");
    }

    /**
     * 获取母线电压阈值详细信息
     */
    @PreAuthorize("@ss.hasPermi('vqms:threshold:query')")
    @GetMapping(value = "/{thresholdId}")
    public AjaxResult getInfo(@PathVariable("thresholdId") Long thresholdId)
    {
        return success(vqmsBusbarThresholdService.selectVqmsBusbarThresholdByThresholdId(thresholdId));
    }

    /**
     * 新增母线电压阈值
     */
    @PreAuthorize("@ss.hasPermi('vqms:threshold:add')")
    @Log(title = "母线电压阈值", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody VqmsBusbarThreshold vqmsBusbarThreshold)
    {
        return toAjax(vqmsBusbarThresholdService.insertVqmsBusbarThreshold(vqmsBusbarThreshold));
    }

    /**
     * 修改母线电压阈值
     */
    @PreAuthorize("@ss.hasPermi('vqms:threshold:edit')")
    @Log(title = "母线电压阈值", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody VqmsBusbarThreshold vqmsBusbarThreshold)
    {
        return toAjax(vqmsBusbarThresholdService.updateVqmsBusbarThreshold(vqmsBusbarThreshold));
    }

    /**
     * 删除母线电压阈值
     */
    @PreAuthorize("@ss.hasPermi('vqms:threshold:remove')")
    @Log(title = "母线电压阈值", businessType = BusinessType.DELETE)
	@DeleteMapping("/{thresholdIds}")
    public AjaxResult remove(@PathVariable Long[] thresholdIds)
    {
        return toAjax(vqmsBusbarThresholdService.deleteVqmsBusbarThresholdByThresholdIds(thresholdIds));
    }
}

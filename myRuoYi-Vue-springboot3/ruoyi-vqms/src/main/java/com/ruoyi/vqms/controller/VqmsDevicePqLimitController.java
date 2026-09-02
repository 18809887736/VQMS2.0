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
import com.ruoyi.vqms.domain.VqmsDevicePqLimit;
import com.ruoyi.vqms.service.IVqmsDevicePqLimitService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 设备P-Q极限曲线Controller
 * 
 * @author ruoyi
 * @date 2026-09-02
 */
@RestController
@RequestMapping("/vqms/devicePqLimit")
public class VqmsDevicePqLimitController extends BaseController
{
    @Autowired
    private IVqmsDevicePqLimitService vqmsDevicePqLimitService;

    /**
     * 查询设备P-Q极限曲线列表
     */
    @PreAuthorize("@ss.hasPermi('vqms:devicePqLimit:list')")
    @GetMapping("/list")
    public TableDataInfo list(VqmsDevicePqLimit vqmsDevicePqLimit)
    {
        startPage();
        List<VqmsDevicePqLimit> list = vqmsDevicePqLimitService.selectVqmsDevicePqLimitList(vqmsDevicePqLimit);
        return getDataTable(list);
    }

    /**
     * 导出设备P-Q极限曲线列表
     */
    @PreAuthorize("@ss.hasPermi('vqms:devicePqLimit:export')")
    @Log(title = "设备P-Q极限曲线", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, VqmsDevicePqLimit vqmsDevicePqLimit)
    {
        List<VqmsDevicePqLimit> list = vqmsDevicePqLimitService.selectVqmsDevicePqLimitList(vqmsDevicePqLimit);
        ExcelUtil<VqmsDevicePqLimit> util = new ExcelUtil<VqmsDevicePqLimit>(VqmsDevicePqLimit.class);
        util.exportExcel(response, list, "设备P-Q极限曲线数据");
    }

    /**
     * 获取设备P-Q极限曲线详细信息
     */
    @PreAuthorize("@ss.hasPermi('vqms:devicePqLimit:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(vqmsDevicePqLimitService.selectVqmsDevicePqLimitById(id));
    }

    /**
     * 新增设备P-Q极限曲线
     */
    @PreAuthorize("@ss.hasPermi('vqms:devicePqLimit:add')")
    @Log(title = "设备P-Q极限曲线", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody VqmsDevicePqLimit vqmsDevicePqLimit)
    {
        return toAjax(vqmsDevicePqLimitService.insertVqmsDevicePqLimit(vqmsDevicePqLimit));
    }

    /**
     * 修改设备P-Q极限曲线
     */
    @PreAuthorize("@ss.hasPermi('vqms:devicePqLimit:edit')")
    @Log(title = "设备P-Q极限曲线", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody VqmsDevicePqLimit vqmsDevicePqLimit)
    {
        return toAjax(vqmsDevicePqLimitService.updateVqmsDevicePqLimit(vqmsDevicePqLimit));
    }

    /**
     * 删除设备P-Q极限曲线
     */
    @PreAuthorize("@ss.hasPermi('vqms:devicePqLimit:remove')")
    @Log(title = "设备P-Q极限曲线", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(vqmsDevicePqLimitService.deleteVqmsDevicePqLimitByIds(ids));
    }
}

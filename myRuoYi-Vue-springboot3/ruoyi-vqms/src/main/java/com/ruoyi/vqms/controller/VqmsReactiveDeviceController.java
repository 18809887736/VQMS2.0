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
import com.ruoyi.vqms.domain.VqmsReactiveDevice;
import com.ruoyi.vqms.service.IVqmsReactiveDeviceService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 无功设备台账Controller
 * 
 * @author ruoyi
 * @date 2026-09-02
 */
@RestController
@RequestMapping("/vqms/device")
public class VqmsReactiveDeviceController extends BaseController
{
    @Autowired
    private IVqmsReactiveDeviceService vqmsReactiveDeviceService;

    /**
     * 查询无功设备台账列表
     */
    @PreAuthorize("@ss.hasPermi('vqms:device:list')")
    @GetMapping("/list")
    public TableDataInfo list(VqmsReactiveDevice vqmsReactiveDevice)
    {
        startPage();
        List<VqmsReactiveDevice> list = vqmsReactiveDeviceService.selectVqmsReactiveDeviceList(vqmsReactiveDevice);
        return getDataTable(list);
    }

    /**
     * 导出无功设备台账列表
     */
    @PreAuthorize("@ss.hasPermi('vqms:device:export')")
    @Log(title = "无功设备台账", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, VqmsReactiveDevice vqmsReactiveDevice)
    {
        List<VqmsReactiveDevice> list = vqmsReactiveDeviceService.selectVqmsReactiveDeviceList(vqmsReactiveDevice);
        ExcelUtil<VqmsReactiveDevice> util = new ExcelUtil<VqmsReactiveDevice>(VqmsReactiveDevice.class);
        util.exportExcel(response, list, "无功设备台账数据");
    }

    /**
     * 获取无功设备台账详细信息
     */
    @PreAuthorize("@ss.hasPermi('vqms:device:query')")
    @GetMapping(value = "/{deviceId}")
    public AjaxResult getInfo(@PathVariable("deviceId") Long deviceId)
    {
        return success(vqmsReactiveDeviceService.selectVqmsReactiveDeviceByDeviceId(deviceId));
    }

    /**
     * 新增无功设备台账
     */
    @PreAuthorize("@ss.hasPermi('vqms:device:add')")
    @Log(title = "无功设备台账", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody VqmsReactiveDevice vqmsReactiveDevice)
    {
        return toAjax(vqmsReactiveDeviceService.insertVqmsReactiveDevice(vqmsReactiveDevice));
    }

    /**
     * 修改无功设备台账
     */
    @PreAuthorize("@ss.hasPermi('vqms:device:edit')")
    @Log(title = "无功设备台账", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody VqmsReactiveDevice vqmsReactiveDevice)
    {
        return toAjax(vqmsReactiveDeviceService.updateVqmsReactiveDevice(vqmsReactiveDevice));
    }

    /**
     * 删除无功设备台账
     */
    @PreAuthorize("@ss.hasPermi('vqms:device:remove')")
    @Log(title = "无功设备台账", businessType = BusinessType.DELETE)
	@DeleteMapping("/{deviceIds}")
    public AjaxResult remove(@PathVariable Long[] deviceIds)
    {
        return toAjax(vqmsReactiveDeviceService.deleteVqmsReactiveDeviceByDeviceIds(deviceIds));
    }
}

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
import com.ruoyi.vqms.domain.VqmsBusbarGroup;
import com.ruoyi.vqms.service.IVqmsBusbarGroupService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 母线组Controller
 * 
 * @author ruoyi
 * @date 2026-09-02
 */
@RestController
@RequestMapping("/vqms/busbarGroup")
public class VqmsBusbarGroupController extends BaseController
{
    @Autowired
    private IVqmsBusbarGroupService vqmsBusbarGroupService;

    /**
     * 查询母线组列表
     */
    @PreAuthorize("@ss.hasPermi('vqms:busbarGroup:list')")
    @GetMapping("/list")
    public TableDataInfo list(VqmsBusbarGroup vqmsBusbarGroup)
    {
        startPage();
        List<VqmsBusbarGroup> list = vqmsBusbarGroupService.selectVqmsBusbarGroupList(vqmsBusbarGroup);
        return getDataTable(list);
    }

    /**
     * 导出母线组列表
     */
    @PreAuthorize("@ss.hasPermi('vqms:busbarGroup:export')")
    @Log(title = "母线组", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, VqmsBusbarGroup vqmsBusbarGroup)
    {
        List<VqmsBusbarGroup> list = vqmsBusbarGroupService.selectVqmsBusbarGroupList(vqmsBusbarGroup);
        ExcelUtil<VqmsBusbarGroup> util = new ExcelUtil<VqmsBusbarGroup>(VqmsBusbarGroup.class);
        util.exportExcel(response, list, "母线组数据");
    }

    /**
     * 获取母线组详细信息
     */
    @PreAuthorize("@ss.hasPermi('vqms:busbarGroup:query')")
    @GetMapping(value = "/{groupNum}")
    public AjaxResult getInfo(@PathVariable("groupNum") Long groupNum)
    {
        return success(vqmsBusbarGroupService.selectVqmsBusbarGroupByGroupNum(groupNum));
    }

    /**
     * 新增母线组
     */
    @PreAuthorize("@ss.hasPermi('vqms:busbarGroup:add')")
    @Log(title = "母线组", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody VqmsBusbarGroup vqmsBusbarGroup)
    {
        return toAjax(vqmsBusbarGroupService.insertVqmsBusbarGroup(vqmsBusbarGroup));
    }

    /**
     * 修改母线组
     */
    @PreAuthorize("@ss.hasPermi('vqms:busbarGroup:edit')")
    @Log(title = "母线组", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody VqmsBusbarGroup vqmsBusbarGroup)
    {
        return toAjax(vqmsBusbarGroupService.updateVqmsBusbarGroup(vqmsBusbarGroup));
    }

    /**
     * 删除母线组
     */
    @PreAuthorize("@ss.hasPermi('vqms:busbarGroup:remove')")
    @Log(title = "母线组", businessType = BusinessType.DELETE)
	@DeleteMapping("/{groupNums}")
    public AjaxResult remove(@PathVariable Long[] groupNums)
    {
        return toAjax(vqmsBusbarGroupService.deleteVqmsBusbarGroupByGroupNums(groupNums));
    }
}

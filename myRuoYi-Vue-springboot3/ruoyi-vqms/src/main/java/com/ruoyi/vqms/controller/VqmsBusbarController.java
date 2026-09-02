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
import com.ruoyi.vqms.domain.VqmsBusbar;
import com.ruoyi.vqms.service.IVqmsBusbarService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 主母线台账Controller
 * 
 * @author ruoyi
 * @date 2026-09-02
 */
@RestController
@RequestMapping("/vqms/busbar")
public class VqmsBusbarController extends BaseController
{
    @Autowired
    private IVqmsBusbarService vqmsBusbarService;

    /**
     * 查询主母线台账列表
     */
    @PreAuthorize("@ss.hasPermi('vqms:busbar:list')")
    @GetMapping("/list")
    public TableDataInfo list(VqmsBusbar vqmsBusbar)
    {
        startPage();
        List<VqmsBusbar> list = vqmsBusbarService.selectVqmsBusbarList(vqmsBusbar);
        return getDataTable(list);
    }

    /**
     * 导出主母线台账列表
     */
    @PreAuthorize("@ss.hasPermi('vqms:busbar:export')")
    @Log(title = "主母线台账", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, VqmsBusbar vqmsBusbar)
    {
        List<VqmsBusbar> list = vqmsBusbarService.selectVqmsBusbarList(vqmsBusbar);
        ExcelUtil<VqmsBusbar> util = new ExcelUtil<VqmsBusbar>(VqmsBusbar.class);
        util.exportExcel(response, list, "主母线台账数据");
    }

    /**
     * 获取主母线台账详细信息
     */
    @PreAuthorize("@ss.hasPermi('vqms:busbar:query')")
    @GetMapping(value = "/{busbarNum}")
    public AjaxResult getInfo(@PathVariable("busbarNum") Long busbarNum)
    {
        return success(vqmsBusbarService.selectVqmsBusbarByBusbarNum(busbarNum));
    }

    /**
     * 新增主母线台账
     */
    @PreAuthorize("@ss.hasPermi('vqms:busbar:add')")
    @Log(title = "主母线台账", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody VqmsBusbar vqmsBusbar)
    {
        return toAjax(vqmsBusbarService.insertVqmsBusbar(vqmsBusbar));
    }

    /**
     * 修改主母线台账
     */
    @PreAuthorize("@ss.hasPermi('vqms:busbar:edit')")
    @Log(title = "主母线台账", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody VqmsBusbar vqmsBusbar)
    {
        return toAjax(vqmsBusbarService.updateVqmsBusbar(vqmsBusbar));
    }

    /**
     * 删除主母线台账
     */
    @PreAuthorize("@ss.hasPermi('vqms:busbar:remove')")
    @Log(title = "主母线台账", businessType = BusinessType.DELETE)
	@DeleteMapping("/{busbarNums}")
    public AjaxResult remove(@PathVariable Long[] busbarNums)
    {
        return toAjax(vqmsBusbarService.deleteVqmsBusbarByBusbarNums(busbarNums));
    }
}

package com.ruoyi.vqms.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.vqms.domain.VqmsEntity;
import com.ruoyi.vqms.service.IVqmsEntityService;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 并网主体（考核主体）Controller
 *
 * @author vqms
 */
@RestController
@RequestMapping("/vqms/entity")
public class VqmsEntityController extends BaseController
{
    @Autowired
    private IVqmsEntityService vqmsEntityService;

    /**
     * 查询并网主体列表
     */
    @PreAuthorize("@ss.hasPermi('vqms:entity:list')")
    @GetMapping("/list")
    public TableDataInfo list(VqmsEntity vqmsEntity)
    {
        startPage();
        List<VqmsEntity> list = vqmsEntityService.selectVqmsEntityList(vqmsEntity);
        return getDataTable(list);
    }

    /**
     * 导出并网主体列表
     */
    @PreAuthorize("@ss.hasPermi('vqms:entity:export')")
    @Log(title = "并网主体", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, VqmsEntity vqmsEntity)
    {
        List<VqmsEntity> list = vqmsEntityService.selectVqmsEntityList(vqmsEntity);
        ExcelUtil<VqmsEntity> util = new ExcelUtil<>(VqmsEntity.class);
        util.exportExcel(response, list, "并网主体数据");
    }

    /**
     * 根据主体ID获取详细信息
     */
    @PreAuthorize("@ss.hasPermi('vqms:entity:query')")
    @GetMapping(value = "/{entityId}")
    public AjaxResult getInfo(@PathVariable("entityId") Long entityId)
    {
        return success(vqmsEntityService.selectVqmsEntityById(entityId));
    }

    /**
     * 新增并网主体
     */
    @PreAuthorize("@ss.hasPermi('vqms:entity:add')")
    @Log(title = "并网主体", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody VqmsEntity vqmsEntity)
    {
        return toAjax(vqmsEntityService.insertVqmsEntity(vqmsEntity));
    }

    /**
     * 修改并网主体
     */
    @PreAuthorize("@ss.hasPermi('vqms:entity:edit')")
    @Log(title = "并网主体", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody VqmsEntity vqmsEntity)
    {
        return toAjax(vqmsEntityService.updateVqmsEntity(vqmsEntity));
    }

    /**
     * 删除并网主体
     */
    @PreAuthorize("@ss.hasPermi('vqms:entity:remove')")
    @Log(title = "并网主体", businessType = BusinessType.DELETE)
    @DeleteMapping("/{entityIds}")
    public AjaxResult remove(@PathVariable Long[] entityIds)
    {
        return toAjax(vqmsEntityService.deleteVqmsEntityByIds(entityIds));
    }
}

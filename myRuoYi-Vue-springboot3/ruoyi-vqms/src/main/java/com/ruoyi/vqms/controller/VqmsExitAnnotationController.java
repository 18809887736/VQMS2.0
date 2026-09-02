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
import com.ruoyi.vqms.domain.VqmsExitAnnotation;
import com.ruoyi.vqms.service.IVqmsExitAnnotationService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * AVC退出原因标注Controller
 * 
 * @author ruoyi
 * @date 2026-09-02
 */
@RestController
@RequestMapping("/vqms/exitAnnotation")
public class VqmsExitAnnotationController extends BaseController
{
    @Autowired
    private IVqmsExitAnnotationService vqmsExitAnnotationService;

    /**
     * 查询AVC退出原因标注列表
     */
    @PreAuthorize("@ss.hasPermi('vqms:exitAnnotation:list')")
    @GetMapping("/list")
    public TableDataInfo list(VqmsExitAnnotation vqmsExitAnnotation)
    {
        startPage();
        List<VqmsExitAnnotation> list = vqmsExitAnnotationService.selectVqmsExitAnnotationList(vqmsExitAnnotation);
        return getDataTable(list);
    }

    /**
     * 导出AVC退出原因标注列表
     */
    @PreAuthorize("@ss.hasPermi('vqms:exitAnnotation:export')")
    @Log(title = "AVC退出原因标注", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, VqmsExitAnnotation vqmsExitAnnotation)
    {
        List<VqmsExitAnnotation> list = vqmsExitAnnotationService.selectVqmsExitAnnotationList(vqmsExitAnnotation);
        ExcelUtil<VqmsExitAnnotation> util = new ExcelUtil<VqmsExitAnnotation>(VqmsExitAnnotation.class);
        util.exportExcel(response, list, "AVC退出原因标注数据");
    }

    /**
     * 获取AVC退出原因标注详细信息
     */
    @PreAuthorize("@ss.hasPermi('vqms:exitAnnotation:query')")
    @GetMapping(value = "/{annotationId}")
    public AjaxResult getInfo(@PathVariable("annotationId") Long annotationId)
    {
        return success(vqmsExitAnnotationService.selectVqmsExitAnnotationByAnnotationId(annotationId));
    }

    /**
     * 新增AVC退出原因标注
     */
    @PreAuthorize("@ss.hasPermi('vqms:exitAnnotation:add')")
    @Log(title = "AVC退出原因标注", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody VqmsExitAnnotation vqmsExitAnnotation)
    {
        return toAjax(vqmsExitAnnotationService.insertVqmsExitAnnotation(vqmsExitAnnotation));
    }

    /**
     * 修改AVC退出原因标注
     */
    @PreAuthorize("@ss.hasPermi('vqms:exitAnnotation:edit')")
    @Log(title = "AVC退出原因标注", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody VqmsExitAnnotation vqmsExitAnnotation)
    {
        return toAjax(vqmsExitAnnotationService.updateVqmsExitAnnotation(vqmsExitAnnotation));
    }

    /**
     * 删除AVC退出原因标注
     */
    @PreAuthorize("@ss.hasPermi('vqms:exitAnnotation:remove')")
    @Log(title = "AVC退出原因标注", businessType = BusinessType.DELETE)
	@DeleteMapping("/{annotationIds}")
    public AjaxResult remove(@PathVariable Long[] annotationIds)
    {
        return toAjax(vqmsExitAnnotationService.deleteVqmsExitAnnotationByAnnotationIds(annotationIds));
    }
}

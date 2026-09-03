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
import com.ruoyi.vqms.domain.VqmsExemptAnnotation;
import com.ruoyi.vqms.service.IVqmsExemptAnnotationService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 调节免考标注Controller
 * 
 * @author ruoyi
 * @date 2026-09-02
 */
@RestController
@RequestMapping("/vqms/exemptAnnotation")
public class VqmsExemptAnnotationController extends BaseController
{
    @Autowired
    private IVqmsExemptAnnotationService vqmsExemptAnnotationService;

    /**
     * 查询调节免考标注列表
     */
    @PreAuthorize("@ss.hasPermi('vqms:exemptAnnotation:list')")
    @GetMapping("/list")
    public TableDataInfo list(VqmsExemptAnnotation vqmsExemptAnnotation)
    {
        startPage();
        List<VqmsExemptAnnotation> list = vqmsExemptAnnotationService.selectVqmsExemptAnnotationList(vqmsExemptAnnotation);
        return getDataTable(list);
    }

    /**
     * 导出调节免考标注列表
     */
    @PreAuthorize("@ss.hasPermi('vqms:exemptAnnotation:export')")
    @Log(title = "调节免考标注", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, VqmsExemptAnnotation vqmsExemptAnnotation)
    {
        List<VqmsExemptAnnotation> list = vqmsExemptAnnotationService.selectVqmsExemptAnnotationList(vqmsExemptAnnotation);
        ExcelUtil<VqmsExemptAnnotation> util = new ExcelUtil<VqmsExemptAnnotation>(VqmsExemptAnnotation.class);
        util.exportExcel(response, list, "调节免考标注数据");
    }

    /**
     * 获取调节免考标注详细信息
     */
    @PreAuthorize("@ss.hasPermi('vqms:exemptAnnotation:query')")
    @GetMapping(value = "/{annotationId}")
    public AjaxResult getInfo(@PathVariable("annotationId") Long annotationId)
    {
        return success(vqmsExemptAnnotationService.selectVqmsExemptAnnotationByAnnotationId(annotationId));
    }

    /**
     * 新增调节免考标注
     */
    @PreAuthorize("@ss.hasPermi('vqms:exemptAnnotation:add')")
    @Log(title = "调节免考标注", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody VqmsExemptAnnotation vqmsExemptAnnotation)
    {
        return toAjax(vqmsExemptAnnotationService.insertVqmsExemptAnnotation(vqmsExemptAnnotation));
    }

    /**
     * 修改调节免考标注
     */
    @PreAuthorize("@ss.hasPermi('vqms:exemptAnnotation:edit')")
    @Log(title = "调节免考标注", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody VqmsExemptAnnotation vqmsExemptAnnotation)
    {
        return toAjax(vqmsExemptAnnotationService.updateVqmsExemptAnnotation(vqmsExemptAnnotation));
    }

    /**
     * 复核免考标注（批准/驳回；复核人≠标注人，仅 PENDING 可复核；APPROVED 后重算判定生效）
     * body: { annotationId, reviewStatus: APPROVED|REJECTED, reviewOpinion }
     */
    @PreAuthorize("@ss.hasPermi('vqms:exemptAnnotation:edit')")
    @Log(title = "调节免考标注-复核", businessType = BusinessType.UPDATE)
    @PutMapping("/review")
    public AjaxResult review(@RequestBody VqmsExemptAnnotation vqmsExemptAnnotation)
    {
        return toAjax(vqmsExemptAnnotationService.reviewVqmsExemptAnnotation(vqmsExemptAnnotation));
    }

    /**
     * 删除调节免考标注
     */
    @PreAuthorize("@ss.hasPermi('vqms:exemptAnnotation:remove')")
    @Log(title = "调节免考标注", businessType = BusinessType.DELETE)
	@DeleteMapping("/{annotationIds}")
    public AjaxResult remove(@PathVariable Long[] annotationIds)
    {
        return toAjax(vqmsExemptAnnotationService.deleteVqmsExemptAnnotationByAnnotationIds(annotationIds));
    }
}

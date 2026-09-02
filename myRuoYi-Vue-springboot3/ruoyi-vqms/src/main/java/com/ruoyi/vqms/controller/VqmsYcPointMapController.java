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
import com.ruoyi.vqms.domain.VqmsYcPointMap;
import com.ruoyi.vqms.service.IVqmsYcPointMapService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 点号语义注册Controller
 * 
 * @author ruoyi
 * @date 2026-09-02
 */
@RestController
@RequestMapping("/vqms/pointMap")
public class VqmsYcPointMapController extends BaseController
{
    @Autowired
    private IVqmsYcPointMapService vqmsYcPointMapService;

    /**
     * 查询点号语义注册列表
     */
    @PreAuthorize("@ss.hasPermi('vqms:pointMap:list')")
    @GetMapping("/list")
    public TableDataInfo list(VqmsYcPointMap vqmsYcPointMap)
    {
        startPage();
        List<VqmsYcPointMap> list = vqmsYcPointMapService.selectVqmsYcPointMapList(vqmsYcPointMap);
        return getDataTable(list);
    }

    /**
     * 导出点号语义注册列表
     */
    @PreAuthorize("@ss.hasPermi('vqms:pointMap:export')")
    @Log(title = "点号语义注册", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, VqmsYcPointMap vqmsYcPointMap)
    {
        List<VqmsYcPointMap> list = vqmsYcPointMapService.selectVqmsYcPointMapList(vqmsYcPointMap);
        ExcelUtil<VqmsYcPointMap> util = new ExcelUtil<VqmsYcPointMap>(VqmsYcPointMap.class);
        util.exportExcel(response, list, "点号语义注册数据");
    }

    /**
     * 获取点号语义注册详细信息
     */
    @PreAuthorize("@ss.hasPermi('vqms:pointMap:query')")
    @GetMapping(value = "/{pointNum}")
    public AjaxResult getInfo(@PathVariable("pointNum") Long pointNum)
    {
        return success(vqmsYcPointMapService.selectVqmsYcPointMapByPointNum(pointNum));
    }

    /**
     * 新增点号语义注册
     */
    @PreAuthorize("@ss.hasPermi('vqms:pointMap:add')")
    @Log(title = "点号语义注册", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody VqmsYcPointMap vqmsYcPointMap)
    {
        return toAjax(vqmsYcPointMapService.insertVqmsYcPointMap(vqmsYcPointMap));
    }

    /**
     * 修改点号语义注册
     */
    @PreAuthorize("@ss.hasPermi('vqms:pointMap:edit')")
    @Log(title = "点号语义注册", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody VqmsYcPointMap vqmsYcPointMap)
    {
        return toAjax(vqmsYcPointMapService.updateVqmsYcPointMap(vqmsYcPointMap));
    }

    /**
     * 删除点号语义注册
     */
    @PreAuthorize("@ss.hasPermi('vqms:pointMap:remove')")
    @Log(title = "点号语义注册", businessType = BusinessType.DELETE)
	@DeleteMapping("/{pointNums}")
    public AjaxResult remove(@PathVariable Long[] pointNums)
    {
        return toAjax(vqmsYcPointMapService.deleteVqmsYcPointMapByPointNums(pointNums));
    }
}

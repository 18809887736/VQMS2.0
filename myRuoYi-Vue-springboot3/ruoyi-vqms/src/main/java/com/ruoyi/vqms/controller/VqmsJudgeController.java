package com.ruoyi.vqms.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.vqms.ingestion.RegulationJudgeService;
import com.ruoyi.vqms.ingestion.RuntimePipelineService;

/**
 * 考核判定 Controller（手动触发；Quartz 随统计上线再接）。
 */
@RestController
@RequestMapping("/vqms/judge")
public class VqmsJudgeController {

    @Autowired
    private RegulationJudgeService regulationJudgeService;

    @Autowired
    private RuntimePipelineService runtimePipelineService;

    /**
     * 调节合格率判定：按日期区间（含端点）重算指令级明细（幂等 upsert）。
     */
    @PreAuthorize("@ss.hasPermi('vqms:judge:run')")
    @PostMapping("/regulation")
    public AjaxResult judgeRegulation(@RequestParam("start") String start, @RequestParam("end") String end) {
        return AjaxResult.success(regulationJudgeService.judgeByDateRange(
                java.time.LocalDate.parse(start), java.time.LocalDate.parse(end)));
    }

    /**
     * 投运率记账：按日期区间（含端点）重算四桶分钟计数与率/罚款快照（幂等 upsert）。
     */
    @PreAuthorize("@ss.hasPermi('vqms:judge:run')")
    @PostMapping("/runtime")
    public AjaxResult judgeRuntime(@RequestParam("start") String start, @RequestParam("end") String end) {
        return AjaxResult.success(runtimePipelineService.runtimeByDateRange(
                java.time.LocalDate.parse(start), java.time.LocalDate.parse(end)));
    }
}

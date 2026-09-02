package com.ruoyi.vqms.controller;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.vqms.ingestion.CommandIngestService;

/**
 * 数据摄取 Controller（手动触发；Quartz 定时随统计上线再接）。
 */
@RestController
@RequestMapping("/vqms/ingest")
public class VqmsIngestController {

    @Autowired
    private CommandIngestService commandIngestService;

    /**
     * 指令摄取：抓取外部库 warn_info(type5) 入指令流水账。
     * 参数 yyyy-MM-dd，含端点。
     */
    @PreAuthorize("@ss.hasPermi('vqms:ingest:run')")
    @PostMapping("/commands")
    public AjaxResult ingestCommands(@RequestParam("start") String start, @RequestParam("end") String end) {
        return AjaxResult.success(commandIngestService.ingestByDateRange(
                LocalDate.parse(start), LocalDate.parse(end)));
    }
}

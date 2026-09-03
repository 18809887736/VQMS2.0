package com.ruoyi.vqms.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.vqms.source.SourceReader;
import com.ruoyi.vqms.source.SourceRows.HisCurveSvRow;
import com.ruoyi.vqms.statistics.SaveTimeParser;

/**
 * 电压曲线查询（免考复核支撑）：母线 + 时间范围 → 逐分钟 high/low，取自外部源（his_curve_sv）。
 *
 * 限量（承 1.0 §10.2）：时间范围上限 31 天、单页上限 500 分钟、busbarNum 必填。
 * 外部表无索引不可加（只读）：限量护物化与返回规模，不护扫描成本——复核场景按指令窗口查（小时级），
 * 大范围排查走数据导出通道。
 */
@RestController
@RequestMapping("/vqms/curve")
public class VqmsCurveController extends BaseController {

    private static final int MAX_RANGE_DAYS = 31;
    private static final int MAX_PAGE_SIZE = 500;
    private static final DateTimeFormatter MINUTE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Autowired
    private SourceReader sourceReader;

    @PreAuthorize("@ss.hasPermi('vqms:curve:list')")
    @GetMapping("/list")
    public TableDataInfo list(String startTime, String endTime, Long busbarNum,
                              Integer pageNum, Integer pageSize) {
        if (StringUtils.isEmpty(startTime) || StringUtils.isEmpty(endTime)) {
            throw new ServiceException("startTime / endTime 必填");
        }
        if (busbarNum == null) {
            throw new ServiceException("busbarNum 必填（曲线查询按单母线）");
        }
        LocalDateTime start = parseOrThrow(startTime);
        LocalDateTime end = parseOrThrow(endTime);
        if (end.isBefore(start)) {
            throw new ServiceException("查询区间倒置");
        }
        if (start.plusDays(MAX_RANGE_DAYS).isBefore(end)) {
            throw new ServiceException("曲线查询时间范围上限 " + MAX_RANGE_DAYS + " 天");
        }
        List<HisCurveSvRow> rows = sourceReader.fetchCurve(List.of(busbarNum), start, end);
        // 分钟对齐 + 每分钟一行（同分钟 jitter 多行取首行）+ 时间升序
        Map<String, Map<String, Object>> byMinute = new HashMap<>();
        for (HisCurveSvRow r : rows) {
            if (r.busbarNum() != busbarNum) {
                continue;
            }
            LocalDateTime t = SaveTimeParser.parseToMinute(r.saveTimeRaw());
            if (t == null || t.isBefore(start) || t.isAfter(end)) {
                continue;
            }
            String key = t.format(MINUTE);
            byMinute.putIfAbsent(key, rowOf(key, r));
        }
        List<Map<String, Object>> sorted = new ArrayList<>(byMinute.values());
        sorted.sort((a, b) -> String.valueOf(a.get("saveTime")).compareTo(String.valueOf(b.get("saveTime"))));

        int size = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, MAX_PAGE_SIZE);
        int from = (pageNum == null || pageNum < 1 ? 1 : pageNum) - 1;
        int total = sorted.size();
        int to = Math.min(from + size, total);
        TableDataInfo data = new TableDataInfo();
        data.setCode(200);
        data.setRows(from >= total ? List.of() : sorted.subList(from, to));
        data.setTotal(total);
        data.setMsg("查询成功");
        return data;
    }

    private static Map<String, Object> rowOf(String minute, HisCurveSvRow r) {
        Map<String, Object> m = new HashMap<>(6);
        m.put("saveTime", minute);
        m.put("busbarNum", r.busbarNum());
        m.put("highSV", r.highSv());
        m.put("lowSV", r.lowSv());
        m.put("averageSV", r.averageSv());
        return m;
    }

    private static LocalDateTime parseOrThrow(String s) {
        try {
            return LocalDateTime.parse((s.length() == 16 ? s + ":00" : s).replace(' ', 'T'));
        } catch (Exception e) {
            throw new ServiceException("时间格式非法（yyyy-MM-dd HH:mm:ss）: " + s);
        }
    }
}

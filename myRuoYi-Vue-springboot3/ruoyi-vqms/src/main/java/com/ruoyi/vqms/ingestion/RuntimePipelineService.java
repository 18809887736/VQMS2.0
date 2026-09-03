package com.ruoyi.vqms.ingestion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vqms.domain.VqmsEntity;
import com.ruoyi.vqms.domain.VqmsRuntimeStats;
import com.ruoyi.vqms.mapper.VqmsEntityMapper;
import com.ruoyi.vqms.mapper.VqmsRuntimeStatsMapper;
import com.ruoyi.vqms.source.SourceReader;
import com.ruoyi.vqms.source.SourceRows.YcHistoryRow;
import com.ruoyi.vqms.statistics.RuntimeClassifier;
import com.ruoyi.vqms.statistics.RuntimeClassifier.DayStats;
import com.ruoyi.vqms.statistics.SaveTimeParser;

/**
 * 投运率管线（编排层）：yc 五信号 → 逐分钟四桶分类 → vqms_runtime_stats 日记账（幂等 upsert）。
 *
 * 信号（points.yaml 口径，全部阶跃保持读法）：
 *  - 并网 yc511/512（≥10=并网；场景逐分钟写、背景 15 分钟写，保持读法统一）
 *  - AVC 投退 yc3009
 *  - 退出原因 yc521/522（1=电网免责；其余非电网，从严）
 * 分类逻辑全部在 statistics 纯函数；本类只做装配与落库。
 */
@Service
public class RuntimePipelineService {

    private static final Logger log = LoggerFactory.getLogger(RuntimePipelineService.class);
    private static final DateTimeFormatter BATCH = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private static final long GRID_MAIN = 511L;
    private static final long GRID_AUX = 512L;
    private static final long AVC_ONOFF = 3009L;
    private static final long EXIT_MAIN = 521L;
    private static final long EXIT_AUX = 522L;
    private static final long ENTITY_ID = 1L;

    @Autowired
    private SourceReader sourceReader;

    @Autowired
    private VqmsRuntimeStatsMapper runtimeStatsMapper;

    @Autowired
    private VqmsEntityMapper entityMapper;

    public Map<String, Object> runtimeByDateRange(LocalDate start, LocalDate end) {
        if (end.isBefore(start)) {
            throw new ServiceException("结束日不能早于起始日");
        }
        BigDecimal capacityKw = resolveCapacity();

        List<VqmsRuntimeStats> rows = new ArrayList<>();
        List<Map<String, Object>> daySummaries = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            LocalDateTime dayStart = d.atStartOfDay();
            LocalDateTime dayEnd = d.atTime(23, 59);
            TreeMap<LocalDateTime, Double> gridMain = loadYc(GRID_MAIN, dayStart, dayEnd);
            TreeMap<LocalDateTime, Double> gridAux = loadYc(GRID_AUX, dayStart, dayEnd);
            TreeMap<LocalDateTime, Double> onoff = loadYc(AVC_ONOFF, dayStart, dayEnd);
            TreeMap<LocalDateTime, Double> exitMain = loadYc(EXIT_MAIN, dayStart, dayEnd);
            TreeMap<LocalDateTime, Double> exitAux = loadYc(EXIT_AUX, dayStart, dayEnd);

            int inService = 0;
            int exitGrid = 0;
            int exitNonGrid = 0;
            int offline = 0;
            for (int m = 0; m < 1440; m++) {
                LocalDateTime t = dayStart.plusMinutes(m);
                boolean grid = RuntimeClassifier.isGrid(stepHold(gridMain, t))
                        || RuntimeClassifier.isGrid(stepHold(gridAux, t));
                BigDecimal onoffV = stepHold(onoff, t);
                boolean avcIn = onoffV != null && onoffV.compareTo(BigDecimal.ONE) >= 0;
                boolean exitGridFlag = isGridReason(stepHold(exitMain, t)) || isGridReason(stepHold(exitAux, t));
                switch (RuntimeClassifier.classify(grid, avcIn, exitGridFlag)) {
                    case IN_SERVICE -> inService++;
                    case EXIT_GRID -> exitGrid++;
                    case EXIT_NON_GRID -> exitNonGrid++;
                    case OFFLINE -> offline++;
                }
            }
            DayStats stats = RuntimeClassifier.summarize(inService, exitGrid, exitNonGrid, offline, capacityKw);

            VqmsRuntimeStats r = new VqmsRuntimeStats();
            r.setStatGrain("D");
            r.setStatPeriod(d);
            r.setEntityId(ENTITY_ID);
            r.setInServiceMin(inService);
            r.setExitGridMin(exitGrid);
            r.setExitNonGridMin(exitNonGrid);
            r.setOfflineMin(offline);
            r.setRatedCapacityKw(capacityKw);
            r.setRatePct(stats.ratePct());
            r.setShortfallPct(stats.shortfallPct());
            r.setPenaltyScore(stats.penaltyScore());
            rows.add(r);
            daySummaries.add(Map.of("date", d.toString(),
                    "inService", inService, "exitGrid", exitGrid, "exitNonGrid", exitNonGrid, "offline", offline,
                    "ratePct", stats.ratePct() == null ? "null" : stats.ratePct().toPlainString(),
                    "penaltyScore", stats.penaltyScore() == null ? "null" : stats.penaltyScore().toPlainString()));
        }
        runtimeStatsMapper.upsertBatch(rows);
        Map<String, Object> result = Map.of(
                "start", start.toString(), "end", end.toString(),
                "batch", LocalDateTime.now().format(BATCH) + "-RUNTIME",
                "capacityKw", capacityKw == null ? "null" : capacityKw.toPlainString(),
                "days", daySummaries);
        log.info("投运率记账完成: {}~{} {}天", start, end, rows.size());
        return result;
    }

    private static boolean isGridReason(BigDecimal v) {
        return v != null && v.compareTo(BigDecimal.ONE) == 0;
    }

    private static BigDecimal stepHold(TreeMap<LocalDateTime, Double> m, LocalDateTime at) {
        Map.Entry<LocalDateTime, Double> e = m.floorEntry(at);
        return e == null ? null : BigDecimal.valueOf(e.getValue());
    }

    private TreeMap<LocalDateTime, Double> loadYc(long point, LocalDateTime dayStart, LocalDateTime dayEnd) {
        List<YcHistoryRow> rows = sourceReader.fetchYc(List.of(point), dayStart.minusMinutes(30), dayEnd);
        TreeMap<LocalDateTime, Double> m = new TreeMap<>();
        for (YcHistoryRow r : rows) {
            if (r.ycNum() != point) {
                continue;
            }
            LocalDateTime minute = SaveTimeParser.parseToMinute(r.ycTimeRaw());
            if (minute != null) {
                m.put(minute, r.ycData());
            }
        }
        return m;
    }

    private BigDecimal resolveCapacity() {
        VqmsEntity e = entityMapper.selectVqmsEntityById(ENTITY_ID);
        return e == null ? null : e.getRatedCapacityKw();
    }
}

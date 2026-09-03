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
import com.ruoyi.vqms.domain.VqmsExitAnnotation;
import com.ruoyi.vqms.domain.VqmsRuntimeStats;
import com.ruoyi.vqms.mapper.VqmsEntityMapper;
import com.ruoyi.vqms.mapper.VqmsExitAnnotationMapper;
import com.ruoyi.vqms.mapper.VqmsRuntimeStatsMapper;
import com.ruoyi.vqms.source.SourceReader;
import com.ruoyi.vqms.source.SourceRows.YcHistoryRow;
import com.ruoyi.vqms.statistics.RuntimeClassifier;
import com.ruoyi.vqms.statistics.RuntimeClassifier.DayStats;
import com.ruoyi.vqms.statistics.SaveTimeParser;

/**
 * 投运率管线（编排层）：yc 五信号 → 逐分钟四桶分类 → vqms_runtime_stats 日记账（幂等 upsert）。
 *
 * 信号全部按语义键消费 vqms_yc_point_map 注册表（现场接线配置化，换号改库不改代码）：
 *  - grid_signal_main / grid_signal_aux 并网（≥10=并网；场景逐分钟写、背景 15 分钟写，保持读法统一）
 *  - avc_onoff AVC 投退
 *  - exit_reason_main / exit_reason_aux 退出原因（1=电网免责；其余非电网，从严）
 *
 * 退出原因两级来源（对齐调节免考三源：MANUAL > AUTO_YC）：yc521/522 对端落盘前现场无源，
 * 退出免责实际全部走人工标注（vqms_exit_annotation，status=0 时段覆盖即生效——值班标注意义为人已核实）：
 *  - 标注 GRID 覆盖该分钟 → 电网原因（免责，出分母）
 *  - 标注 NON_GRID 覆盖 → 非电网（扣罚）
 *  - 无标注覆盖 → 遥测 yc521/522（无值时从严按非电网）
 * 分类逻辑全部在 statistics 纯函数；本类只做装配与落库。
 */
@Service
public class RuntimePipelineService {

    private static final Logger log = LoggerFactory.getLogger(RuntimePipelineService.class);
    private static final DateTimeFormatter BATCH = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    @Autowired
    private SourceReader sourceReader;

    @Autowired
    private VqmsRuntimeStatsMapper runtimeStatsMapper;

    @Autowired
    private VqmsEntityMapper entityMapper;

    @Autowired
    private VqmsExitAnnotationMapper exitAnnotationMapper;

    @Autowired
    private VqmsPointConfig pointConfig;

    public Map<String, Object> runtimeByDateRange(LocalDate start, LocalDate end) {
        if (end.isBefore(start)) {
            throw new ServiceException("结束日不能早于起始日");
        }
        long entityId = pointConfig.resolveEntityId();
        Map<String, Long> pts = pointConfig.loadGatePoints();
        long gridMainPt = pointConfig.require(pts, VqmsPointConfig.GRID_SIGNAL_MAIN);
        long gridAuxPt = pointConfig.require(pts, VqmsPointConfig.GRID_SIGNAL_AUX);
        long onoffPt = pointConfig.require(pts, VqmsPointConfig.AVC_ONOFF);
        long exitMainPt = pointConfig.require(pts, VqmsPointConfig.EXIT_REASON_MAIN);
        long exitAuxPt = pointConfig.require(pts, VqmsPointConfig.EXIT_REASON_AUX);
        BigDecimal capacityKw = resolveCapacity(entityId);
        log.info("投运率管线 {}~{} 主体={} 容量={} 点号: 并网主={} 副={} 投退={} 退因主={} 副={}",
                start, end, entityId, capacityKw == null ? "null" : capacityKw.toPlainString(),
                gridMainPt, gridAuxPt, onoffPt, exitMainPt, exitAuxPt);

        List<VqmsRuntimeStats> rows = new ArrayList<>();
        List<Map<String, Object>> daySummaries = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            LocalDateTime dayStart = d.atStartOfDay();
            LocalDateTime dayEnd = d.atTime(23, 59);
            TreeMap<LocalDateTime, Double> gridMain = loadYc(gridMainPt, dayStart, dayEnd);
            TreeMap<LocalDateTime, Double> gridAux = loadYc(gridAuxPt, dayStart, dayEnd);
            TreeMap<LocalDateTime, Double> onoff = loadYc(onoffPt, dayStart, dayEnd);
            TreeMap<LocalDateTime, Double> exitMain = loadYc(exitMainPt, dayStart, dayEnd);
            TreeMap<LocalDateTime, Double> exitAux = loadYc(exitAuxPt, dayStart, dayEnd);
            List<VqmsExitAnnotation> manualExits = loadManualExits(dayStart, dayEnd);
            if (!manualExits.isEmpty()) {
                log.info("{}: 人工退出标注 {} 条参与免责判定（MANUAL 优先于遥测）", d, manualExits.size());
            }

            // 缺数≠离线：五信号当日零遥测行 = 源库无数据，跳过不记账
            // （真实离线日信号行仍在，值显示未并网；幻影 offline=1440 行会污染月/年汇总）
            if (gridMain.isEmpty() && gridAux.isEmpty() && onoff.isEmpty()
                    && exitMain.isEmpty() && exitAux.isEmpty()) {
                log.error("跳过 {}: 源库当日无任何投运相关遥测行（五信号 {} 个点全空），不记账——"
                        + "若非计划内缺数日请排查对端落盘", d, 5);
                daySummaries.add(Map.of("date", d.toString(), "skipped", "no-source-data"));
                continue;
            }

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
                boolean exitGridFlag = exitReasonOf(manualExits, t,
                        isGridReason(stepHold(exitMain, t)) || isGridReason(stepHold(exitAux, t)));
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
            r.setEntityId(entityId);
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
        if (!rows.isEmpty()) {
            runtimeStatsMapper.upsertBatch(rows);
        }
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

    /** 当日有效人工退出标注（status=0；与当日时段相交即载入）。 */
    private List<VqmsExitAnnotation> loadManualExits(LocalDateTime dayStart, LocalDateTime dayEnd) {
        VqmsExitAnnotation q = new VqmsExitAnnotation();
        q.setStatus("0");
        List<VqmsExitAnnotation> all = exitAnnotationMapper.selectVqmsExitAnnotationList(q);
        List<VqmsExitAnnotation> out = new ArrayList<>();
        if (all == null) {
            return out;
        }
        for (VqmsExitAnnotation a : all) {
            LocalDateTime s = toLocalDateTime(a.getPeriodStart());
            LocalDateTime e = toLocalDateTime(a.getPeriodEnd());
            if (s != null && e != null && !s.isAfter(dayEnd) && !e.isBefore(dayStart)) {
                out.add(a);
            }
        }
        return out;
    }

    /** 退出原因合成（MANUAL > AUTO_YC）：标注 GRID/NON_GRID 覆盖分钟 → 标注原因生效
     *  （多条覆盖取 id 最大即最新标注；GRID 免责 / NON_GRID 扣罚）；无覆盖 → 遥测判定。
     *  UNKNOWN 标注不参与（原因不明不作为人核实依据，不覆盖遥测）。 */
    private static boolean exitReasonOf(List<VqmsExitAnnotation> manualExits, LocalDateTime t, boolean telemetric) {
        VqmsExitAnnotation hit = null;
        for (VqmsExitAnnotation a : manualExits) {
            LocalDateTime s = toLocalDateTime(a.getPeriodStart());
            LocalDateTime e = toLocalDateTime(a.getPeriodEnd());
            if (s == null || e == null || t.isBefore(s) || t.isAfter(e)) {
                continue;
            }
            if ("GRID".equals(a.getExitReason()) || "NON_GRID".equals(a.getExitReason())) {
                hit = a; // 列表按 id 升序时后写覆盖 → 最新标注生效
            }
        }
        return hit == null ? telemetric : "GRID".equals(hit.getExitReason());
    }

    private static LocalDateTime toLocalDateTime(java.util.Date d) {
        return d == null ? null : LocalDateTime.ofInstant(d.toInstant(), java.time.ZoneId.systemDefault());
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

    private BigDecimal resolveCapacity(long entityId) {
        VqmsEntity e = entityMapper.selectVqmsEntityById(entityId);
        return e == null ? null : e.getRatedCapacityKw();
    }
}

package com.ruoyi.vqms.ingestion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vqms.domain.VqmsArt26Curve;
import com.ruoyi.vqms.mapper.VqmsArt26CurveMapper;
import com.ruoyi.vqms.source.SourceReader;
import com.ruoyi.vqms.source.SourceRows.HisCurveSvRow;
import com.ruoyi.vqms.source.SourceRows.YcHistoryRow;
import com.ruoyi.vqms.statistics.SaveTimeParser;

/**
 * 第26条对账计算（编排层）：季度考核曲线 × 实测电压 × AVC 投退 → 三桶对账。
 *
 * 口径（2024 版细则 p10-11）：
 *  - 调度按季下发母线电压曲线作为考核依据——曲线行登记于 vqms_art26_curve
 *  - 母线电压不合格判定：实测电压超出考核 [limit_down, limit_up]；实测取 average_SV，
 *    缺则 (low+high)/2；high/low 任一 ≤0 坏点行剔除等同缺数（发现④）
 *  - 免考核（第 3 款）：AVC 主站闭环调节控制期间的不合格时段免考 → 桶：
 *      QUALIFIED           合格分钟
 *      EXEMPT_CLOSED_LOOP  不合格·AVC 闭环（第3款免考核，对账出示）
 *      VIOLATION_EXIT_AVC  不合格·AVC 退出（待判：26 条未细分退出免责，展示事实供人工判定）
 *      NO_CURVE / NO_DATA  无曲线覆盖 / 无实测——不参与考核分钟（对账留痕）
 */
@Service
public class Art26ReconcileService {

    private static final Logger log = LoggerFactory.getLogger(Art26ReconcileService.class);

    @Autowired
    private VqmsArt26CurveMapper curveMapper;

    @Autowired
    private SourceReader sourceReader;

    @Autowired
    private VqmsPointConfig pointConfig;

    @Autowired
    private com.ruoyi.vqms.mapper.VqmsJudgeParamMapper judgeParamMapper;

    /** 逐日对账行（分钟数）。 */
    public record DayRow(LocalDate date, int total, int qualified, int exemptClosedLoop,
                         int violationExitAvc, int noCurve, int noData) {
    }

    public record Reconcile(String quarter, long busbarNum, LocalDate start, LocalDate end,
                            long totalMinutes, long qualified, long exemptClosedLoop,
                            long violationExitAvc, long noCurve, long noData,
                            List<VqmsArt26Curve> curves, List<DayRow> days) {
    }

    public Reconcile reconcile(String quarter, long busbarNum) {
        LocalDate[] bounds = quarterBounds(quarter);
        LocalDate start = bounds[0];
        LocalDate end = bounds[1];
        LocalDateTime ts = start.atStartOfDay();
        LocalDateTime te = end.atTime(23, 59);

        VqmsArt26Curve q = new VqmsArt26Curve();
        q.setBusbarNum(busbarNum);
        q.setQuarter(quarter);
        List<VqmsArt26Curve> curves = curveMapper.selectByBusbarQuarter(q);
        if (curves == null || curves.isEmpty()) {
            throw new ServiceException("季度 " + quarter + " 母线 " + busbarNum + " 无考核曲线——先登记/导入下发曲线");
        }

        boolean zeroBlock = resolveFlag("zero_badpoint_block_enabled", 1);
        Map<LocalDateTime, BigDecimal> measured = loadMeasured(busbarNum, ts, te, zeroBlock);
        long avcPt = pointConfig.require(pointConfig.loadGatePoints(), VqmsPointConfig.AVC_ONOFF);
        TreeMap<LocalDateTime, Double> onoff = loadYc(avcPt, ts, te);

        long qualified = 0, exempt = 0, violation = 0, noCurve = 0, noData = 0, total = 0;
        List<DayRow> days = new ArrayList<>();
        LocalDate curDay = null;
        int dt = 0, dqual = 0, dex = 0, dvi = 0, dnc = 0, dnd = 0;
        for (LocalDateTime t = ts; !t.isAfter(te); t = t.plusMinutes(1)) {
            if (!t.toLocalDate().equals(curDay)) {
                if (curDay != null) {
                    days.add(new DayRow(curDay, dt, dqual, dex, dvi, dnc, dnd));
                }
                curDay = t.toLocalDate();
                dt = dqual = dex = dvi = dnc = dnd = 0;
            }
            total++;
            dt++;
            VqmsArt26Curve hit = coverOf(curves, t);
            if (hit == null) {
                noCurve++;
                dnc++;
                continue;
            }
            BigDecimal v = measured.get(t);
            if (v == null) {
                noData++;
                dnd++;
                continue;
            }
            boolean bad = v.compareTo(hit.getLimitDownKv()) < 0 || v.compareTo(hit.getLimitUpKv()) > 0;
            if (!bad) {
                qualified++;
                dqual++;
            } else if (isAvcClosedLoop(onoff, t)) {
                exempt++;
                dex++;
            } else {
                violation++;
                dvi++;
            }
        }
        days.add(new DayRow(curDay, dt, dqual, dex, dvi, dnc, dnd));

        log.info("第26条对账 {} 母线{}: 考核分钟 {} 合格 {} 免考(闭环) {} 待判(退出) {} 无曲线 {} 无实测 {}",
                quarter, busbarNum, total, qualified, exempt, violation, noCurve, noData);
        return new Reconcile(quarter, busbarNum, start, end, total, qualified, exempt, violation,
                noCurve, noData, curves, days);
    }

    private boolean resolveFlag(String key, int defaultValue) {
        com.ruoyi.vqms.domain.VqmsJudgeParam q = new com.ruoyi.vqms.domain.VqmsJudgeParam();
        q.setParamKey(key);
        java.util.List<com.ruoyi.vqms.domain.VqmsJudgeParam> list = judgeParamMapper.selectVqmsJudgeParamList(q);
        if (list != null && !list.isEmpty() && list.get(0).getParamValue() != null) {
            return list.get(0).getParamValue() != 0;
        }
        return defaultValue != 0;
    }

    /** 实测电压：average_SV 优先，缺则 (low+high)/2；0 值坏点拦截按整定开关（默认拦截，发现④）。 */
    private Map<LocalDateTime, BigDecimal> loadMeasured(long busbarNum, LocalDateTime ts, LocalDateTime te, boolean zeroBlock) {
        List<HisCurveSvRow> rows = sourceReader.fetchCurve(List.of(busbarNum), ts.minusMinutes(2), te.plusMinutes(2), zeroBlock);
        Map<LocalDateTime, BigDecimal> m = new java.util.HashMap<>(rows.size());
        for (HisCurveSvRow r : rows) {
            if (r.busbarNum() != busbarNum) {
                continue;
            }
            LocalDateTime t = SaveTimeParser.parseToMinute(r.saveTimeRaw());
            if (t == null || t.isBefore(ts) || t.isAfter(te)
                    || r.lowSv() == null || r.highSv() == null
                    || (zeroBlock && (r.lowSv().signum() <= 0 || r.highSv().signum() <= 0))) {
                continue;
            }
            BigDecimal v = r.averageSv() != null && r.averageSv().signum() > 0
                    ? r.averageSv()
                    : r.lowSv().add(r.highSv()).divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
            m.putIfAbsent(t, v);
        }
        return m;
    }

    /** 覆盖该分钟的曲线行（多行声明顺序后者优先）。 */
    private static VqmsArt26Curve coverOf(List<VqmsArt26Curve> curves, LocalDateTime t) {
        VqmsArt26Curve hit = null;
        for (VqmsArt26Curve c : curves) {
            LocalDateTime s = toLdt(c.getPeriodStart());
            LocalDateTime e = toLdt(c.getPeriodEnd());
            if (s != null && e != null && !t.isBefore(s) && !t.isAfter(e)) {
                hit = c;
            }
        }
        return hit;
    }

    private static boolean isAvcClosedLoop(TreeMap<LocalDateTime, Double> onoff, LocalDateTime t) {
        Map.Entry<LocalDateTime, Double> e = onoff.floorEntry(t);
        return e != null && e.getValue() != null && e.getValue() >= 1.0;
    }

    private static LocalDateTime toLdt(java.util.Date d) {
        return d == null ? null : LocalDateTime.ofInstant(d.toInstant(), java.time.ZoneId.systemDefault());
    }

    private TreeMap<LocalDateTime, Double> loadYc(long point, LocalDateTime ts, LocalDateTime te) {
        List<YcHistoryRow> rows = sourceReader.fetchYc(List.of(point), ts.minusMinutes(30), te);
        TreeMap<LocalDateTime, Double> m = new TreeMap<>();
        for (YcHistoryRow r : rows) {
            if (r.ycNum() != point) {
                continue;
            }
            LocalDateTime t = SaveTimeParser.parseToMinute(r.ycTimeRaw());
            if (t != null) {
                m.put(t, r.ycData());
            }
        }
        return m;
    }

    /** "2026Q1" → [2026-01-01, 2026-03-31]。 */
    private static LocalDate[] quarterBounds(String quarter) {
        if (quarter == null || !quarter.matches("\\d{4}Q[1-4]")) {
            throw new ServiceException("季度格式非法（yyyyQn，如 2026Q1）: " + quarter);
        }
        int year = Integer.parseInt(quarter.substring(0, 4));
        int q = Integer.parseInt(quarter.substring(5));
        LocalDate start = LocalDate.of(year, (q - 1) * 3 + 1, 1);
        LocalDate end = start.plusMonths(3).minusDays(1);
        return new LocalDate[]{start, end};
    }
}

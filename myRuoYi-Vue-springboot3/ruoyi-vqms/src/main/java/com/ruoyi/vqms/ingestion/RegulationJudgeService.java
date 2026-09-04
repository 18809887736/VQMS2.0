package com.ruoyi.vqms.ingestion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vqms.domain.VqmsBusbar;
import com.ruoyi.vqms.domain.VqmsBusbarGroup;
import com.ruoyi.vqms.domain.VqmsCommandLedger;
import com.ruoyi.vqms.domain.VqmsDevicePqLimit;
import com.ruoyi.vqms.domain.VqmsExemptAnnotation;
import com.ruoyi.vqms.domain.VqmsJudgeParam;
import com.ruoyi.vqms.domain.VqmsReactiveDevice;
import com.ruoyi.vqms.domain.VqmsRegulationCmd;
import com.ruoyi.vqms.mapper.VqmsBusbarMapper;
import com.ruoyi.vqms.mapper.VqmsBusbarGroupMapper;
import com.ruoyi.vqms.mapper.VqmsCommandLedgerMapper;
import com.ruoyi.vqms.mapper.VqmsDevicePqLimitMapper;
import com.ruoyi.vqms.mapper.VqmsExemptAnnotationMapper;
import com.ruoyi.vqms.mapper.VqmsJudgeParamMapper;
import com.ruoyi.vqms.mapper.VqmsReactiveDeviceMapper;
import com.ruoyi.vqms.mapper.VqmsRegulationCmdMapper;
import com.ruoyi.vqms.source.SourceReader;
import com.ruoyi.vqms.source.SourceRows.HisCurveSvRow;
import com.ruoyi.vqms.source.SourceRows.WarnInfoRow;
import com.ruoyi.vqms.source.SourceRows.YcHistoryRow;
import com.ruoyi.vqms.statistics.DeviceExemptionJudge;
import com.ruoyi.vqms.statistics.DeviceExemptionJudge.DeviceSample;
import com.ruoyi.vqms.statistics.DeviceExemptionJudge.Direction;
import com.ruoyi.vqms.statistics.DeviceExemptionJudge.PqPoint;
import com.ruoyi.vqms.statistics.DeviceExemptionJudge.Verdict;
import com.ruoyi.vqms.statistics.RegulationJudge;
import com.ruoyi.vqms.statistics.RegulationJudge.Band;
import com.ruoyi.vqms.statistics.RegulationJudge.Outcome;
import com.ruoyi.vqms.statistics.SaveTimeParser;
import com.ruoyi.vqms.statistics.VTargetDecoder;

/**
 * 调节合格率判定管线（编排层）：ledger 指令 × 曲线/遥测 → 三状态判定 → vqms_regulation_cmd 幂等 upsert。
 *
 * 判定逻辑全部在 statistics 纯函数（RegulationJudge/VTargetDecoder/SaveTimeParser/DeviceExemptionJudge）；
 * 取数全部走 source Reader；本类只做装配与落库。
 *
 * 免考三源链（源优先级 MANUAL > AUTO_YX > AUTO_DEVICE，逐档独立）：
 *  - MANUAL：vqms_exempt_annotation 已批准（APPROVED 且未撤销）按 (warn_time_raw, millisecond, obj_num) 溯源键匹配
 *  - AUTO_YX：yx501 免考旗阶跃保持采样——快档 @t0+tFast、经档 @t0+tEcon+1
 *  - AUTO_DEVICE：附件6 §三设备级判定——采样时刻曲线带整体低于/高于目标 → 方向 INJECT/ABSORB，
 *    全部 AVC 闭环设备 Q 顶到各自极限（容差 exempt_q_tol_kvar）→ 该档免考
 *
 * v1 简化口径（sim 联调）：
 *  - 判定组 = group 0（220kV），主判定母线 = 组 default_main_busbar_num（未配置 fail-fast）
 *  - 实时电压点 = vqms_busbar.realtime_yc_num（未整定降级：增量指令 INVALID，绝对值目标不受影响）
 */
@Service
public class RegulationJudgeService {

    private static final Logger log = LoggerFactory.getLogger(RegulationJudgeService.class);
    private static final DateTimeFormatter BATCH = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private static final String ALGORITHM_ID = "V2_0";
    private static final int T_ECON = 5;
    private static final int BATCH_SIZE = 500;

    @Autowired
    private SourceReader sourceReader;

    @Autowired
    private VqmsCommandLedgerMapper ledgerMapper;

    @Autowired
    private VqmsRegulationCmdMapper regulationCmdMapper;

    @Autowired
    private VqmsBusbarGroupMapper busbarGroupMapper;

    @Autowired
    private VqmsBusbarMapper busbarMapper;

    @Autowired
    private VqmsJudgeParamMapper judgeParamMapper;

    @Autowired
    private VqmsReactiveDeviceMapper reactiveDeviceMapper;

    @Autowired
    private VqmsDevicePqLimitMapper devicePqLimitMapper;

    @Autowired
    private VqmsExemptAnnotationMapper exemptAnnotationMapper;

    @Autowired
    private VqmsPointConfig pointConfig;

    /** 逐日免考判定装配上下文（主体 + 设备台账 + 当日生效 P-Q 曲线 + 当日设备遥测 + 已批标注 + 完整度闸门）。 */
    private record DayContext(long entityId, BigDecimal qTol, BigDecimal minCompleteness,
                              List<VqmsReactiveDevice> loopDevices,
                              Map<Long, List<PqPoint>> curves,
                              Map<Long, TreeMap<LocalDateTime, Double>> deviceYc,
                              Map<String, VqmsExemptAnnotation> approved) {
    }

    public Map<String, Object> judgeByDateRange(LocalDate start, LocalDate end) {
        if (end.isBefore(start)) {
            throw new ServiceException("结束日不能早于起始日");
        }
        int tFast = resolveTFast();
        long entityId = pointConfig.resolveEntityId();
        long mainBusbar = resolveMainBusbar();
        Long realtimePoint = resolveRealtimePoint(mainBusbar);
        Long exemptFlagPt = pointConfig.optional(pointConfig.loadGatePoints(),
                VqmsPointConfig.EXEMPT_FLAG, "AUTO_YX 免考链停用，免考走 MANUAL/AUTO_DEVICE");
        BigDecimal qTol = resolveQTol();
        BigDecimal minCompleteness = resolveMinCompleteness();
        boolean zeroBlock = resolveFlag("zero_badpoint_block_enabled", 1);
        List<VqmsReactiveDevice> loopDevices = loadLoopDevices();
        Map<Long, List<VqmsDevicePqLimit>> pqRows = loadPqRows(loopDevices);
        Map<String, VqmsExemptAnnotation> approved = loadApprovedAnnotations();
        log.info("判定开始 {}~{} 主体={} tFast={} 主母线={} 实时点={} 免考旗={} qTol={} 完整度闸门τ={} 0值坏点={} 闭环设备={} 曲线版点数={} 已批标注={}",
                start, end, entityId, tFast, mainBusbar,
                realtimePoint == null ? "未整定" : realtimePoint,
                exemptFlagPt == null ? "未整定" : exemptFlagPt,
                qTol.toPlainString(),
                minCompleteness == null ? "关闭" : minCompleteness.toPlainString(), zeroBlock ? "拦截" : "放行",
                loopDevices.size(),
                pqRows.values().stream().mapToInt(List::size).sum(), approved.size());

        Map<String, Long> counts = new HashMap<>();
        for (String k : List.of("judged", "fastQualified", "fastPenalized", "fastExempted", "fastInvalid",
                "econQualified", "econPenalized", "econExempted", "econInvalid",
                "exemptManual", "exemptAutoYx", "exemptAutoDevice")) {
            counts.put(k, 0L);
        }

        List<VqmsRegulationCmd> buffer = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            LocalDateTime dayStart = d.atStartOfDay();
            LocalDateTime dayEnd = d.atTime(23, 59);
            List<VqmsCommandLedger> cmds = ledgerMapper.selectByRange(dayStart, dayEnd);
            if (cmds.isEmpty()) {
                continue;
            }
            Map<LocalDateTime, Band> curve = loadCurve(mainBusbar, dayStart, dayEnd, zeroBlock).bands();
            TreeMap<LocalDateTime, Double> exemptFlag = exemptFlagPt == null
                    ? new TreeMap<>() : loadYc(exemptFlagPt, dayStart, dayEnd);
            TreeMap<LocalDateTime, Double> realtime = realtimePoint == null
                    ? new TreeMap<>() : loadYc(realtimePoint, dayStart, dayEnd);
            DayContext ctx = new DayContext(entityId, qTol, minCompleteness, loopDevices,
                    resolveCurves(pqRows, d), loadDeviceYc(loopDevices, dayStart, dayEnd), approved);

            for (VqmsCommandLedger cmd : cmds) {
                VqmsRegulationCmd row = judgeOne(cmd, curve, exemptFlag, realtime, tFast, ctx, counts);
                buffer.add(row);
                if (buffer.size() >= BATCH_SIZE) {
                    regulationCmdMapper.upsertBatch(new ArrayList<>(buffer));
                    buffer.clear();
                }
            }
        }
        if (!buffer.isEmpty()) {
            regulationCmdMapper.upsertBatch(buffer);
        }
        Map<String, Object> result = new HashMap<>(counts);
        result.put("start", start.toString());
        result.put("end", end.toString());
        result.put("batch", LocalDateTime.now().format(BATCH) + "-JUDGE");
        log.info("判定完成: {}", result);
        return result;
    }

    private VqmsRegulationCmd judgeOne(VqmsCommandLedger cmd, Map<LocalDateTime, Band> curve,
                                       TreeMap<LocalDateTime, Double> exemptFlag,
                                       TreeMap<LocalDateTime, Double> realtime, int tFast,
                                       DayContext ctx, Map<String, Long> counts) {
        LocalDateTime t0 = cmd.getCmdTime();
        BigDecimal rtKv = realtimeAt(realtime, t0);
        BigDecimal targetKv = VTargetDecoder.decodeAny(cmd.getWarnContent(), rtKv);

        // 原始判定不进免考旗：三源免考链在编排层逐档套用（PEN → EXEMPTED 的翻转规则与纯函数一致）
        Outcome o = RegulationJudge.judge(targetKv, curve, t0, tFast, T_ECON, false, false,
                ctx.minCompleteness());
        String fastState = o.fastState();
        String econState = o.econState();
        String srcFast = null;
        String srcEcon = null;
        Long refFast = null;
        Long refEcon = null;

        VqmsExemptAnnotation ann = ctx.approved().get(
                annotationKey(cmd.getWarnTimeRaw(), cmd.getMillisecond(), cmd.getObjNum()));

        if (RegulationJudge.PENALIZED.equals(fastState)) {
            if (ann != null && ("FAST".equals(ann.getTier()) || "BOTH".equals(ann.getTier()))) {
                fastState = RegulationJudge.EXEMPTED;
                srcFast = "MANUAL";
                refFast = ann.getAnnotationId();
                counts.merge("exemptManual", 1L, Long::sum);
            } else if (flagAt(exemptFlag, t0.plusMinutes(tFast))) {
                fastState = RegulationJudge.EXEMPTED;
                srcFast = "AUTO_YX";
                counts.merge("exemptAutoYx", 1L, Long::sum);
            } else if (deviceExempted(curve, targetKv, t0.plusMinutes(tFast), ctx)) {
                fastState = RegulationJudge.EXEMPTED;
                srcFast = "AUTO_DEVICE";
                counts.merge("exemptAutoDevice", 1L, Long::sum);
            }
        }
        if (RegulationJudge.PENALIZED.equals(econState)) {
            if (ann != null && ("ECON".equals(ann.getTier()) || "BOTH".equals(ann.getTier()))) {
                econState = RegulationJudge.EXEMPTED;
                srcEcon = "MANUAL";
                refEcon = ann.getAnnotationId();
                counts.merge("exemptManual", 1L, Long::sum);
            } else if (flagAt(exemptFlag, t0.plusMinutes(T_ECON + 1))) {
                econState = RegulationJudge.EXEMPTED;
                srcEcon = "AUTO_YX";
                counts.merge("exemptAutoYx", 1L, Long::sum);
            } else if (deviceExempted(curve, targetKv, t0.plusMinutes(T_ECON + 1), ctx)) {
                econState = RegulationJudge.EXEMPTED;
                srcEcon = "AUTO_DEVICE";
                counts.merge("exemptAutoDevice", 1L, Long::sum);
            }
        }

        VqmsRegulationCmd r = new VqmsRegulationCmd();
        r.setStatDate(t0.toLocalDate());
        r.setEntityId(ctx.entityId());
        r.setGroupNum(0L);
        r.setWarnTimeRaw(cmd.getWarnTimeRaw());
        r.setMillisecond(cmd.getMillisecond());
        r.setObjNum(cmd.getObjNum());
        r.setCmdTime(t0);
        r.setAlgorithmId(ALGORITHM_ID);
        r.setDecodeAlgorithm("ROT10_V1");
        r.setTargetKv(targetKv);
        r.setResponseMinutes(responseMinutes(targetKv, curve, t0));
        r.setTFastSnapshot(tFast);
        r.setFastState(fastState);
        r.setEconState(econState);
        r.setCompleteness(o.completenessFast().min(o.completenessEcon()));
        r.setInvalidTiers(o.invalidTiers());
        if (targetKv == null) {
            r.setUndecodableReason(cmd.getWarnContent() != null && cmd.getWarnContent().contains("目标值")
                    ? "CYCLE_CODE_INVALID" : "MISSING_T0_VOLTAGE");
        }
        if (srcFast != null || srcEcon != null) {
            // 单列记源：跨档统一记两档中最高优先级源；ref_id 仅 MANUAL 溯源（指向 annotation）
            if ("MANUAL".equals(srcFast) || "MANUAL".equals(srcEcon)) {
                r.setExemptSource("MANUAL");
                r.setExemptRefId(refFast != null ? refFast : refEcon);
            } else if ("AUTO_YX".equals(srcFast) || "AUTO_YX".equals(srcEcon)) {
                r.setExemptSource("AUTO_YX");
            } else {
                r.setExemptSource("AUTO_DEVICE");
            }
        }
        accumulate(counts, r);
        return r;
    }

    /** 附件6 §三设备级免考：采样时刻曲线带整体偏离目标定方向，闭环设备全部顶满才免。 */
    private boolean deviceExempted(Map<LocalDateTime, Band> curve, BigDecimal targetKv,
                                   LocalDateTime at, DayContext ctx) {
        if (targetKv == null) {
            return false;
        }
        Band b = curve.get(at);
        if (b == null) {
            return false;
        }
        Direction dir = DeviceExemptionJudge.resolveDirection(targetKv, targetKv, b.low(), b.high());
        List<DeviceSample> samples = new ArrayList<>(ctx.loopDevices().size());
        for (VqmsReactiveDevice d : ctx.loopDevices()) {
            samples.add(sampleOf(d, ctx.curves().getOrDefault(d.getDeviceId(), List.of()), ctx.deviceYc(), at));
        }
        Verdict v = DeviceExemptionJudge.judge(samples, dir, ctx.qTol());
        if (v.exempted()) {
            return true;
        }
        log.debug("设备免考未过 @{} dir={}: {}", at, dir, v.blockers());
        return false;
    }

    private DeviceSample sampleOf(VqmsReactiveDevice d, List<PqPoint> curve,
                                  Map<Long, TreeMap<LocalDateTime, Double>> deviceYc, LocalDateTime at) {
        TreeMap<LocalDateTime, Double> qMap = d.getqYcNum() == null ? null : deviceYc.get(d.getqYcNum());
        TreeMap<LocalDateTime, Double> pMap = d.getpYcNum() == null ? null : deviceYc.get(d.getpYcNum());
        return new DeviceSample(
                d.getDeviceId(), d.getDeviceCode(),
                d.getDeviceType() == null ? 0 : d.getDeviceType().intValue(),
                !Integer.valueOf(0).equals(d.getInAvcLoop()),
                pMap == null ? null : stepHold(pMap, at),
                qMap == null ? null : stepHold(qMap, at),
                d.getRatedSKva(), d.getRatedQUpKvar(), d.getRatedQDownKvar(), curve);
    }

    /** 当日生效的设备 P-Q 曲线：取 effective_from ≤ day 的最新版本（effective_to 空为开区间）。 */
    private Map<Long, List<PqPoint>> resolveCurves(Map<Long, List<VqmsDevicePqLimit>> pqRows, LocalDate day) {
        Map<Long, List<PqPoint>> m = new HashMap<>();
        for (Map.Entry<Long, List<VqmsDevicePqLimit>> e : pqRows.entrySet()) {
            m.put(e.getKey(), curveOf(e.getValue(), day));
        }
        return m;
    }

    private List<PqPoint> curveOf(List<VqmsDevicePqLimit> rows, LocalDate day) {
        Map<LocalDate, List<VqmsDevicePqLimit>> versions = new HashMap<>();
        for (VqmsDevicePqLimit r : rows) {
            LocalDate from = localDate(r.getEffectiveFrom());
            if (from == null || from.isAfter(day)) {
                continue;
            }
            LocalDate to = localDate(r.getEffectiveTo());
            if (to != null && !day.isBefore(to)) {
                continue;
            }
            versions.computeIfAbsent(from, k -> new ArrayList<>()).add(r);
        }
        if (versions.isEmpty()) {
            return List.of();
        }
        List<VqmsDevicePqLimit> latest = versions.get(Collections.max(versions.keySet()));
        return latest.stream()
                .sorted(Comparator.comparing(VqmsDevicePqLimit::getpKw))
                .map(r -> new PqPoint(r.getpKw(), r.getqUpKvar(), r.getqDownKvar()))
                .toList();
    }

    private static LocalDate localDate(Date d) {
        return d == null ? null : LocalDateTime.ofInstant(d.toInstant(), ZoneId.systemDefault()).toLocalDate();
    }

    private List<VqmsReactiveDevice> loadLoopDevices() {
        VqmsReactiveDevice q = new VqmsReactiveDevice();
        q.setInAvcLoop(1);
        q.setStatus("0");
        List<VqmsReactiveDevice> list = reactiveDeviceMapper.selectVqmsReactiveDeviceList(q);
        return list == null ? List.of() : list;
    }

    private Map<Long, List<VqmsDevicePqLimit>> loadPqRows(List<VqmsReactiveDevice> devices) {
        Map<Long, List<VqmsDevicePqLimit>> m = new HashMap<>();
        for (VqmsReactiveDevice d : devices) {
            VqmsDevicePqLimit q = new VqmsDevicePqLimit();
            q.setDeviceId(d.getDeviceId());
            List<VqmsDevicePqLimit> rows = devicePqLimitMapper.selectVqmsDevicePqLimitList(q);
            if (rows != null && !rows.isEmpty()) {
                m.put(d.getDeviceId(), rows);
            }
        }
        return m;
    }

    private Map<Long, TreeMap<LocalDateTime, Double>> loadDeviceYc(List<VqmsReactiveDevice> devices,
                                                                   LocalDateTime dayStart, LocalDateTime dayEnd) {
        Map<Long, Boolean> points = new HashMap<>();
        for (VqmsReactiveDevice d : devices) {
            if (d.getqYcNum() != null) {
                points.put(d.getqYcNum(), true);
            }
            if (d.getpYcNum() != null) {
                points.put(d.getpYcNum(), true);
            }
        }
        Map<Long, TreeMap<LocalDateTime, Double>> m = new HashMap<>();
        for (Long pt : points.keySet()) {
            m.put(pt, loadYc(pt, dayStart, dayEnd));
        }
        return m;
    }

    private Map<String, VqmsExemptAnnotation> loadApprovedAnnotations() {
        VqmsExemptAnnotation q = new VqmsExemptAnnotation();
        q.setReviewStatus("APPROVED");
        q.setStatus("0");
        List<VqmsExemptAnnotation> list = exemptAnnotationMapper.selectVqmsExemptAnnotationList(q);
        Map<String, VqmsExemptAnnotation> m = new HashMap<>();
        if (list != null) {
            for (VqmsExemptAnnotation a : list) {
                m.put(annotationKey(a.getWarnTimeRaw(), a.getMillisecond(), a.getObjNum()), a);
            }
        }
        return m;
    }

    private static String annotationKey(String warnTimeRaw, String millisecond, Long objNum) {
        return warnTimeRaw + "|" + millisecond + "|" + objNum;
    }

    /** 首个夹住目标值的分钟（t0 起 5 分钟内）；未夹住返回 null。 */
    private Integer responseMinutes(BigDecimal targetKv, Map<LocalDateTime, Band> curve, LocalDateTime t0) {
        if (targetKv == null) {
            return null;
        }
        for (int m = 1; m <= T_ECON; m++) {
            Band b = curve.get(t0.plusMinutes(m));
            if (b != null && b.valid()
                    && targetKv.compareTo(b.low()) >= 0 && targetKv.compareTo(b.high()) <= 0) {
                return m;
            }
        }
        return null;
    }

    /** 曲线装载结果：bands 供判定；dirtyRows = 0 值坏点拦截数（发现④，数据公平性审计留痕）。 */
    private record CurveLoad(Map<LocalDateTime, Band> bands, long dirtyRows) {
    }

    private CurveLoad loadCurve(long busbarNum, LocalDateTime dayStart, LocalDateTime dayEnd, boolean zeroBlock) {
        List<HisCurveSvRow> rows = sourceReader.fetchCurve(List.of(busbarNum),
                dayStart.minusMinutes(2), dayEnd.plusMinutes(7), zeroBlock);
        Map<LocalDateTime, Band> m = new HashMap<>(rows.size() * 2);
        long dirty = 0;
        for (HisCurveSvRow r : rows) {
            if (r.busbarNum() != busbarNum) {
                continue;
            }
            LocalDateTime minute = SaveTimeParser.parseToMinute(r.saveTimeRaw());
            if (minute == null || r.lowSv() == null || r.highSv() == null) {
                continue;
            }
            Band b = zeroBlock ? RegulationJudge.sanitizeBand(r.lowSv(), r.highSv())
                    : new Band(r.lowSv(), r.highSv(), r.lowSv().compareTo(r.highSv()) <= 0);
            if (b == null) {
                dirty++;
                continue;
            }
            m.put(minute, b);
        }
        if (dirty > 0) {
            log.warn("数据质量闸门 {}: 拦截 0 值坏点 {} 行（不采信，等同缺数；现场库核对报告发现④）", busbarNum, dirty);
        }
        return new CurveLoad(m, dirty);
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

    private static BigDecimal stepHold(TreeMap<LocalDateTime, Double> m, LocalDateTime at) {
        Map.Entry<LocalDateTime, Double> e = m.floorEntry(at);
        return e == null ? null : BigDecimal.valueOf(e.getValue());
    }

    /** 免考旗阶跃保持采样：≤at 的最近值是否为 1（真=免考）。 */
    private static boolean flagAt(TreeMap<LocalDateTime, Double> m, LocalDateTime at) {
        BigDecimal v = stepHold(m, at);
        return v != null && v.compareTo(BigDecimal.ONE) >= 0;
    }

    /** 增量 t0 实时电压：floor 采样 + 新鲜度窗口 5 分钟（"t0 实时"语义；
     *  背景 15 分钟网格陈旧值不算，防误解增量——S11 口径）。 */
    private static BigDecimal realtimeAt(TreeMap<LocalDateTime, Double> m, LocalDateTime t0) {
        Map.Entry<LocalDateTime, Double> e = m.floorEntry(t0);
        if (e == null || java.time.Duration.between(e.getKey(), t0).toMinutes() > 5) {
            return null;
        }
        return BigDecimal.valueOf(e.getValue());
    }

    private int resolveTFast() {
        VqmsJudgeParam q = new VqmsJudgeParam();
        q.setParamKey("t_fast");
        List<VqmsJudgeParam> list = judgeParamMapper.selectVqmsJudgeParamList(q);
        if (list != null && !list.isEmpty() && list.get(0).getParamValue() != null) {
            return list.get(0).getParamValue().intValue();
        }
        return 4;
    }

    private BigDecimal resolveQTol() {
        VqmsJudgeParam q = new VqmsJudgeParam();
        q.setParamKey("exempt_q_tol_kvar");
        List<VqmsJudgeParam> list = judgeParamMapper.selectVqmsJudgeParamList(q);
        if (list != null && !list.isEmpty() && list.get(0).getParamValue() != null) {
            return BigDecimal.valueOf(list.get(0).getParamValue());
        }
        return BigDecimal.ZERO;
    }

    /** 0/1 开关类整定参数读取（缺行/缺省取 defaultValue；界面可改，核实单口径原子化）。 */
    private boolean resolveFlag(String key, int defaultValue) {
        VqmsJudgeParam q = new VqmsJudgeParam();
        q.setParamKey(key);
        List<VqmsJudgeParam> list = judgeParamMapper.selectVqmsJudgeParamList(q);
        if (list != null && !list.isEmpty() && list.get(0).getParamValue() != null) {
            return list.get(0).getParamValue() != 0;
        }
        return defaultValue != 0;
    }

    /** 完整度闸门 τ（0~100%，0=关闭；1.0 数据不可用策略 A3/A4 最小口径，运行方可整定）。 */
    private BigDecimal resolveMinCompleteness() {
        VqmsJudgeParam q = new VqmsJudgeParam();
        q.setParamKey("min_window_completeness_pct");
        List<VqmsJudgeParam> list = judgeParamMapper.selectVqmsJudgeParamList(q);
        if (list != null && !list.isEmpty() && list.get(0).getParamValue() != null) {
            return list.get(0).getParamValue() == 0 ? null
                    : BigDecimal.valueOf(list.get(0).getParamValue()).divide(BigDecimal.valueOf(100), 4,
                            java.math.RoundingMode.HALF_UP);
        }
        return null;
    }

    private long resolveMainBusbar() {
        List<VqmsBusbarGroup> groups = busbarGroupMapper.selectVqmsBusbarGroupList(new VqmsBusbarGroup());
        if (groups != null) {
            for (VqmsBusbarGroup g : groups) {
                if (g.getGroupNum() != null && g.getGroupNum() == 0L
                        && g.getDefaultMainBusbarNum() != null) {
                    return g.getDefaultMainBusbarNum();
                }
            }
        }
        throw new ServiceException("判定组未配置: vqms_busbar_group 无 group_num=0 行的 default_main_busbar_num（现场台账整定）");
    }

    /** 主母线 t0 实时电压点：只认 vqms_busbar.realtime_yc_num 台账（现场候选 yc8 东母/yc14 西母）。
     *  未整定返回 null → 增量指令全部 INVALID（绝对值目标解码不受影响），降级告警不阻断。 */
    private Long resolveRealtimePoint(long busbarNum) {
        List<VqmsBusbar> bars = busbarMapper.selectVqmsBusbarList(new VqmsBusbar());
        if (bars != null) {
            for (VqmsBusbar b : bars) {
                if (b.getBusbarNum() != null && b.getBusbarNum() == busbarNum) {
                    if (b.getRealtimeYcNum() != null) {
                        return b.getRealtimeYcNum();
                    }
                    break;
                }
            }
        }
        log.error("母线 {} realtime_yc_num 未整定（vqms_busbar 台账；候选 yc8 东母/yc14 西母）→ 增量指令将全部 INVALID", busbarNum);
        return null;
    }

    private void accumulate(Map<String, Long> counts, VqmsRegulationCmd r) {
        counts.merge("judged", 1L, Long::sum);
        counts.merge("fast" + r.getFastState(), 1L, Long::sum);
        counts.merge("econ" + r.getEconState(), 1L, Long::sum);
    }
}

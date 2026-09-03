package com.ruoyi.vqms.ingestion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
import com.ruoyi.vqms.domain.VqmsJudgeParam;
import com.ruoyi.vqms.domain.VqmsRegulationCmd;
import com.ruoyi.vqms.mapper.VqmsBusbarMapper;
import com.ruoyi.vqms.mapper.VqmsBusbarGroupMapper;
import com.ruoyi.vqms.mapper.VqmsCommandLedgerMapper;
import com.ruoyi.vqms.mapper.VqmsJudgeParamMapper;
import com.ruoyi.vqms.mapper.VqmsRegulationCmdMapper;
import com.ruoyi.vqms.source.SourceReader;
import com.ruoyi.vqms.source.SourceRows.HisCurveSvRow;
import com.ruoyi.vqms.source.SourceRows.WarnInfoRow;
import com.ruoyi.vqms.source.SourceRows.YcHistoryRow;
import com.ruoyi.vqms.statistics.RegulationJudge;
import com.ruoyi.vqms.statistics.RegulationJudge.Band;
import com.ruoyi.vqms.statistics.RegulationJudge.Outcome;
import com.ruoyi.vqms.statistics.SaveTimeParser;
import com.ruoyi.vqms.statistics.VTargetDecoder;

/**
 * 调节合格率判定管线（编排层）：ledger 指令 × 曲线/遥测 → 三状态判定 → vqms_regulation_cmd 幂等 upsert。
 *
 * 判定逻辑全部在 statistics 纯函数（RegulationJudge/VTargetDecoder/SaveTimeParser）；
 * 取数全部走 source Reader；本类只做装配与落库。
 *
 * v1 简化口径（sim 联调）：
 *  - 判定组 = group 0（220kV），主判定母线 = 组 default_main_busbar_num 兜底
 *  - 实时电压点 = vqms_busbar.realtime_yc_num，空则回退 4002（sim 占位，现场核对后落库生效）
 *  - 免考旗 = yx501（点号注册表在册），阶跃保持采样：快档 @t0+tFast、经档 @t0+tEcon+1（窗口闭合后）
 */
@Service
public class RegulationJudgeService {

    private static final Logger log = LoggerFactory.getLogger(RegulationJudgeService.class);
    private static final DateTimeFormatter BATCH = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private static final String ALGORITHM_ID = "V2_0";
    private static final long EXEMPT_FLAG_POINT = 501L;
    private static final long SIM_REALTIME_POINT = 4002L;
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

    public Map<String, Object> judgeByDateRange(LocalDate start, LocalDate end) {
        if (end.isBefore(start)) {
            throw new ServiceException("结束日不能早于起始日");
        }
        int tFast = resolveTFast();
        long mainBusbar = resolveMainBusbar();
        long realtimePoint = resolveRealtimePoint(mainBusbar);
        log.info("判定开始 {}~{} tFast={} 主母线={} 实时点={} 免考旗={}",
                start, end, tFast, mainBusbar, realtimePoint, EXEMPT_FLAG_POINT);

        Map<String, Long> counts = new HashMap<>();
        for (String k : List.of("judged", "fastQualified", "fastPenalized", "fastExempted", "fastInvalid",
                "econQualified", "econPenalized", "econExempted", "econInvalid")) {
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
            Map<LocalDateTime, Band> curve = loadCurve(mainBusbar, dayStart, dayEnd);
            TreeMap<LocalDateTime, Double> exemptFlag = loadYc(EXEMPT_FLAG_POINT, dayStart, dayEnd);
            TreeMap<LocalDateTime, Double> realtime = loadYc(realtimePoint, dayStart, dayEnd);

            for (VqmsCommandLedger cmd : cmds) {
                VqmsRegulationCmd row = judgeOne(cmd, curve, exemptFlag, realtime, tFast);
                buffer.add(row);
                accumulate(counts, row);
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
                                       TreeMap<LocalDateTime, Double> realtime, int tFast) {
        LocalDateTime t0 = cmd.getCmdTime();
        BigDecimal rtKv = realtimeAt(realtime, t0);
        BigDecimal targetKv = VTargetDecoder.decodeAny(cmd.getWarnContent(), rtKv);

        boolean exemptFast = flagAt(exemptFlag, t0.plusMinutes(tFast));
        boolean exemptEcon = flagAt(exemptFlag, t0.plusMinutes(T_ECON + 1));

        Outcome o = RegulationJudge.judge(targetKv, curve, t0, tFast, T_ECON, exemptFast, exemptEcon);

        VqmsRegulationCmd r = new VqmsRegulationCmd();
        r.setStatDate(t0.toLocalDate());
        r.setEntityId(1L);
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
        r.setFastState(o.fastState());
        r.setEconState(o.econState());
        r.setCompleteness(o.completenessFast().min(o.completenessEcon()));
        r.setInvalidTiers(o.invalidTiers());
        if (targetKv == null) {
            r.setUndecodableReason(cmd.getWarnContent() != null && cmd.getWarnContent().contains("目标值")
                    ? "CYCLE_CODE_INVALID" : "MISSING_T0_VOLTAGE");
        }
        if (RegulationJudge.EXEMPTED.equals(o.fastState()) || RegulationJudge.EXEMPTED.equals(o.econState())) {
            r.setExemptSource("AUTO_YX");
        }
        return r;
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

    private Map<LocalDateTime, Band> loadCurve(long busbarNum, LocalDateTime dayStart, LocalDateTime dayEnd) {
        List<HisCurveSvRow> rows = sourceReader.fetchCurve(List.of(busbarNum),
                dayStart.minusMinutes(2), dayEnd.plusMinutes(7));
        Map<LocalDateTime, Band> m = new HashMap<>(rows.size() * 2);
        for (HisCurveSvRow r : rows) {
            if (r.busbarNum() != busbarNum) {
                continue;
            }
            LocalDateTime minute = SaveTimeParser.parseToMinute(r.saveTimeRaw());
            if (minute == null || r.lowSv() == null || r.highSv() == null) {
                continue;
            }
            boolean valid = r.lowSv().compareTo(r.highSv()) <= 0;
            m.put(minute, new Band(r.lowSv(), r.highSv(), valid));
        }
        return m;
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
        return 0L;
    }

    private long resolveRealtimePoint(long busbarNum) {
        List<VqmsBusbar> bars = busbarMapper.selectVqmsBusbarList(new VqmsBusbar());
        if (bars != null) {
            for (VqmsBusbar b : bars) {
                if (b.getBusbarNum() != null && b.getBusbarNum() == busbarNum && b.getRealtimeYcNum() != null) {
                    return b.getRealtimeYcNum();
                }
            }
        }
        return SIM_REALTIME_POINT; // sim 占位：现场核对后 vqms_busbar.realtime_yc_num 落库生效
    }

    private void accumulate(Map<String, Long> counts, VqmsRegulationCmd r) {
        counts.merge("judged", 1L, Long::sum);
        counts.merge("fast" + r.getFastState(), 1L, Long::sum);
        counts.merge("econ" + r.getEconState(), 1L, Long::sum);
    }
}

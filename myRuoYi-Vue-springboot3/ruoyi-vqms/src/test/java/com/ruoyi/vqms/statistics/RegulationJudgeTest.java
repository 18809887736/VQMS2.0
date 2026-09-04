package com.ruoyi.vqms.statistics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.ruoyi.vqms.statistics.RegulationJudge.Band;
import com.ruoyi.vqms.statistics.RegulationJudge.Outcome;

/**
 * 判定器纯函数单测——场景语义对齐造数器 S01~S16（manifest 验收的算术地基）。
 */
class RegulationJudgeTest {

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 3, 15, 10, 0);

    private static Band band(int low, int high) {
        return new Band(BigDecimal.valueOf(low), BigDecimal.valueOf(high), low <= high);
    }

    private static Map<LocalDateTime, Band> curve(int... lowHighPairs) {
        Map<LocalDateTime, Band> m = new HashMap<>();
        for (int i = 0; i < lowHighPairs.length; i += 2) {
            m.put(T0.plusMinutes(1 + i / 2), band(lowHighPairs[i], lowHighPairs[i + 1]));
        }
        return m;
    }

    @Test
    void s01_bothQualified() {
        // 快窗+经窗都夹住 223 → {QUAL, QUAL}
        Map<LocalDateTime, Band> c = curve(222, 224, 222, 224, 222, 224, 222, 224, 222, 224);
        Outcome o = RegulationJudge.judge(BigDecimal.valueOf(223.15), c, T0, 4, 5, false, false);
        assertEquals("QUALIFIED", o.fastState());
        assertEquals("QUALIFIED", o.econState());
    }

    @Test
    void s02_fastPenEconQual() {
        // 快窗 (222,223) 不含 223.15；经窗第5分 (222,224) 夹住 → {PEN, QUAL}
        Map<LocalDateTime, Band> c = curve(222, 223, 222, 223, 222, 223, 222, 223, 222, 224);
        Outcome o = RegulationJudge.judge(BigDecimal.valueOf(223.15), c, T0, 4, 5, false, false);
        assertEquals("PENALIZED", o.fastState());
        assertEquals("QUALIFIED", o.econState());
    }

    @Test
    void s03_fastQualEconPen() {
        // 快窗夹住，经窗漂走 → {QUAL, PEN}
        Map<LocalDateTime, Band> c = curve(222, 224, 222, 224, 222, 224, 222, 224, 226, 228);
        Outcome o = RegulationJudge.judge(BigDecimal.valueOf(223.15), c, T0, 4, 5, false, false);
        assertEquals("QUALIFIED", o.fastState());
        assertEquals("PENALIZED", o.econState());
    }

    @Test
    void s04_bothPen() {
        Map<LocalDateTime, Band> c = curve(226, 228, 226, 228, 226, 228, 226, 228, 226, 228);
        Outcome o = RegulationJudge.judge(BigDecimal.valueOf(223.15), c, T0, 4, 5, false, false);
        assertEquals("PENALIZED", o.fastState());
        assertEquals("PENALIZED", o.econState());
    }

    @Test
    void s05_exemptFastOnly() {
        Map<LocalDateTime, Band> c = curve(226, 228, 226, 228, 226, 228, 226, 228, 226, 228);
        Outcome o = RegulationJudge.judge(BigDecimal.valueOf(223.15), c, T0, 4, 5, true, false);
        assertEquals("EXEMPTED", o.fastState());
        assertEquals("PENALIZED", o.econState());
    }

    @Test
    void s07_allExempt() {
        Map<LocalDateTime, Band> c = curve(226, 228, 226, 228, 226, 228, 226, 228, 226, 228);
        Outcome o = RegulationJudge.judge(BigDecimal.valueOf(223.15), c, T0, 4, 5, true, true);
        assertEquals("EXEMPTED", o.fastState());
        assertEquals("EXEMPTED", o.econState());
    }

    @Test
    void s08_boundaryInclusive() {
        // 快窗 min(low)=223.15 恰等于 V_target（≤闭区间边界算合格）；经窗漂走
        Map<LocalDateTime, Band> c2 = new HashMap<>();
        c2.put(T0.plusMinutes(1), new Band(new BigDecimal("223.15"), new BigDecimal("224"), true));
        c2.put(T0.plusMinutes(2), band(222, 224));
        c2.put(T0.plusMinutes(3), band(222, 224));
        c2.put(T0.plusMinutes(4), band(222, 224));
        c2.put(T0.plusMinutes(5), band(226, 228));
        Outcome o = RegulationJudge.judge(new BigDecimal("223.15"), c2, T0, 4, 5, false, false);
        assertEquals("QUALIFIED", o.fastState());
        assertEquals("PENALIZED", o.econState());
    }

    @Test
    void s13_partialMissingStillJudged() {
        // 缺第3分钟，其余夹住 → 两档仍 QUAL，completeness<1
        Map<LocalDateTime, Band> c = curve(222, 224, 222, 224, 222, 224, 222, 224, 222, 224);
        c.remove(T0.plusMinutes(3));
        Outcome o = RegulationJudge.judge(BigDecimal.valueOf(223.15), c, T0, 4, 5, false, false);
        assertEquals("QUALIFIED", o.fastState());
        assertEquals(0, new BigDecimal("0.75").compareTo(o.completenessFast()));
        assertEquals("QUALIFIED", o.econState());
    }

    @Test
    void s14_fastWindowAllMissing() {
        // 快窗 4 分钟全缺 → INVALID；经窗第5分在 → QUAL
        Map<LocalDateTime, Band> c = new HashMap<>();
        c.put(T0.plusMinutes(5), band(222, 224));
        Outcome o = RegulationJudge.judge(BigDecimal.valueOf(223.15), c, T0, 4, 5, false, false);
        assertEquals("INVALID", o.fastState());
        assertEquals("QUALIFIED", o.econState());
        assertEquals("FAST", o.invalidTiers());
    }

    @Test
    void s16_lGreaterThanH_invalidTier() {
        // 快窗含 low>high 异常行 → 快档 INVALID；经窗正常 → QUAL
        Map<LocalDateTime, Band> c = new HashMap<>();
        c.put(T0.plusMinutes(1), band(222, 224));
        c.put(T0.plusMinutes(2), band(226, 224)); // low>high 异常
        c.put(T0.plusMinutes(3), band(222, 224));
        c.put(T0.plusMinutes(4), band(222, 224));
        c.put(T0.plusMinutes(5), band(222, 224));
        Outcome o = RegulationJudge.judge(BigDecimal.valueOf(223.15), c, T0, 4, 5, false, false);
        assertEquals("INVALID", o.fastState());
        assertEquals("QUALIFIED", o.econState());
        assertEquals("FAST", o.invalidTiers());
    }

    @Test
    void nullTarget_bothInvalid() {
        Map<LocalDateTime, Band> c = curve(222, 224, 222, 224, 222, 224, 222, 224, 222, 224);
        Outcome o = RegulationJudge.judge(null, c, T0, 4, 5, false, false);
        assertEquals("INVALID", o.fastState());
        assertEquals("INVALID", o.econState());
        assertEquals("FAST,ECON", o.invalidTiers());
    }

    // ────────────────── 数据公平性闸门（发现④ 脏值 + τ 完整度）──────────────────

    @Test
    void sanitizeBand_rejectsZeroBadPoints() {
        // 发现④：high/low 任一侧 0.0 坏点 → 不采信（null）
        assertNull(RegulationJudge.sanitizeBand(new BigDecimal("0"), new BigDecimal("230")));
        assertNull(RegulationJudge.sanitizeBand(new BigDecimal("220"), new BigDecimal("0")));
        assertNull(RegulationJudge.sanitizeBand(new BigDecimal("-1"), new BigDecimal("230")));
        assertNull(RegulationJudge.sanitizeBand(null, new BigDecimal("230")));
        // 正常行
        assertEquals(new Band(new BigDecimal("222"), new BigDecimal("224"), true),
                RegulationJudge.sanitizeBand(new BigDecimal("222"), new BigDecimal("224")));
        // low>high 保留行但标 invalid（S16 口径）
        assertEquals(new Band(new BigDecimal("226"), new BigDecimal("224"), false),
                RegulationJudge.sanitizeBand(new BigDecimal("226"), new BigDecimal("224")));
    }

    @Test
    void zeroLowRowBlocked_wouldNotWidenEnvelope() {
        // 偏高不到位（L=225>目标）：若 low=0 行不被拦截，minLow=0 会把 223.15 误夹进包络 → 误 QUAL
        Map<LocalDateTime, Band> c = new HashMap<>();
        c.put(T0.plusMinutes(1), new Band(new BigDecimal("0"), new BigDecimal("226"), true)); // 坏点（已被 sanitizeBand 拦截的形态）
        c.put(T0.plusMinutes(2), band(225, 226));
        c.put(T0.plusMinutes(3), band(225, 226));
        c.put(T0.plusMinutes(4), band(225, 226));
        c.put(T0.plusMinutes(5), band(225, 226));
        // 直接验证：不带坏点行 → PEN（拦截后包络 [225,226] 不含 223.15）
        Map<LocalDateTime, Band> sanitized = new HashMap<>(c);
        sanitized.remove(T0.plusMinutes(1));
        Outcome o = RegulationJudge.judge(BigDecimal.valueOf(223.15), sanitized, T0, 4, 5, false, false);
        assertEquals("PENALIZED", o.fastState());
        // 反证：带上坏点行（未拦截）→ minLow=0 → 误 QUAL（这正是要拦的原因）
        Outcome poisoned = RegulationJudge.judge(BigDecimal.valueOf(223.15), c, T0, 4, 5, false, false);
        assertEquals("QUALIFIED", poisoned.fastState());
    }

    @Test
    void tauGate_belowThresholdInvalid() {
        // 快窗 4 分钟仅 1 分钟有数（completeness 0.25 < τ=0.5）→ INVALID；经窗 1/1 正常 → QUAL
        Map<LocalDateTime, Band> c = new HashMap<>();
        c.put(T0.plusMinutes(4), band(222, 224));
        c.put(T0.plusMinutes(5), band(222, 224));
        Outcome o = RegulationJudge.judge(BigDecimal.valueOf(223.15), c, T0, 4, 5, false, false,
                new BigDecimal("0.5"));
        assertEquals("INVALID", o.fastState());
        assertEquals(0, new BigDecimal("0.25").compareTo(o.completenessFast()));
        assertEquals("FAST", o.invalidTiers());
        assertEquals("QUALIFIED", o.econState());
    }

    @Test
    void tauGate_atOrAboveThresholdStillJudged() {
        // 快窗缺 1 分钟（0.75 ≥ 0.5）→ 正常判 QUAL（S13 口径不回归）
        Map<LocalDateTime, Band> c = curve(222, 224, 222, 224, 222, 224, 222, 224, 222, 224);
        c.remove(T0.plusMinutes(3));
        Outcome o = RegulationJudge.judge(BigDecimal.valueOf(223.15), c, T0, 4, 5, false, false,
                new BigDecimal("0.5"));
        assertEquals("QUALIFIED", o.fastState());
    }

    @Test
    void tauGate_zeroDisables() {
        // τ=0（关闭）→ 缺数窗口仍按原口径硬判（0.25 完整度的快窗判 PEN）
        Map<LocalDateTime, Band> c = new HashMap<>();
        c.put(T0.plusMinutes(4), band(226, 227)); // 不夹 → PEN
        c.put(T0.plusMinutes(5), band(222, 224));
        Outcome off = RegulationJudge.judge(BigDecimal.valueOf(223.15), c, T0, 4, 5, false, false,
                BigDecimal.ZERO);
        assertEquals("PENALIZED", off.fastState());
        // null 同关闭（旧签名兼容路径）
        assertEquals("PENALIZED",
                RegulationJudge.judge(BigDecimal.valueOf(223.15), c, T0, 4, 5, false, false).fastState());
    }

    // ────────────────── 数据不可用处置原子开关（JudgePolicy）──────────────────

    @Test
    void policy_undecodableAsPenalized() {
        Map<LocalDateTime, Band> c = curve(222, 224, 222, 224, 222, 224, 222, 224, 222, 224);
        assertEquals("INVALID", RegulationJudge.judge(null, c, T0, 4, 5, false, false,
                new RegulationJudge.JudgePolicy(null, 0, 0, 0)).fastState());
        assertEquals("PENALIZED", RegulationJudge.judge(null, c, T0, 4, 5, false, false,
                new RegulationJudge.JudgePolicy(null, 1, 0, 0)).fastState());
    }

    @Test
    void policy_windowMissingAsPenalized() {
        Map<LocalDateTime, Band> c = new HashMap<>();
        c.put(T0.plusMinutes(5), band(222, 224));
        assertEquals("INVALID", RegulationJudge.judge(BigDecimal.valueOf(223.15), c, T0, 4, 5, false, false,
                new RegulationJudge.JudgePolicy(null, 0, 0, 0)).fastState());
        assertEquals("PENALIZED", RegulationJudge.judge(BigDecimal.valueOf(223.15), c, T0, 4, 5, false, false,
                new RegulationJudge.JudgePolicy(null, 0, 1, 0)).fastState());
    }

    @Test
    void policy_bandInvertedAsPenalized() {
        Map<LocalDateTime, Band> c = new HashMap<>();
        c.put(T0.plusMinutes(1), band(226, 224));
        c.put(T0.plusMinutes(2), band(222, 224));
        c.put(T0.plusMinutes(3), band(222, 224));
        c.put(T0.plusMinutes(4), band(222, 224));
        c.put(T0.plusMinutes(5), band(222, 224));
        assertEquals("INVALID", RegulationJudge.judge(BigDecimal.valueOf(223.15), c, T0, 4, 5, false, false,
                new RegulationJudge.JudgePolicy(null, 0, 0, 0)).fastState());
        assertEquals("PENALIZED", RegulationJudge.judge(BigDecimal.valueOf(223.15), c, T0, 4, 5, false, false,
                new RegulationJudge.JudgePolicy(null, 0, 0, 1)).fastState());
    }

}

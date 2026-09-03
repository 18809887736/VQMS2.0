package com.ruoyi.vqms.statistics;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}

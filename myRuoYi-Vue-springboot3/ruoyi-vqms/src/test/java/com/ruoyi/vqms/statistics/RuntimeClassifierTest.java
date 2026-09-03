package com.ruoyi.vqms.statistics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.ruoyi.vqms.statistics.RuntimeClassifier.DayStats;
import com.ruoyi.vqms.statistics.RuntimeClassifier.MinuteState;

/**
 * 投运率分类纯函数单测——U01~U07 场景语义（manifest 验收的算术地基）。
 */
class RuntimeClassifierTest {

    private static final BigDecimal CAP = BigDecimal.valueOf(600000);

    @Test
    void gridCodeDecode() {
        assertTrue(RuntimeClassifier.isGrid(BigDecimal.valueOf(11)));
        assertTrue(RuntimeClassifier.isGrid(BigDecimal.valueOf(12)));
        assertFalse(RuntimeClassifier.isGrid(BigDecimal.valueOf(0)));
        assertFalse(RuntimeClassifier.isGrid(BigDecimal.valueOf(9)));
        assertFalse(RuntimeClassifier.isGrid(null));
    }

    @Test
    void classifyFourBuckets() {
        assertEquals(MinuteState.IN_SERVICE, RuntimeClassifier.classify(true, true, false));
        assertEquals(MinuteState.IN_SERVICE, RuntimeClassifier.classify(true, true, true));
        assertEquals(MinuteState.EXIT_NON_GRID, RuntimeClassifier.classify(true, false, false));
        assertEquals(MinuteState.EXIT_GRID, RuntimeClassifier.classify(true, false, true));
        assertEquals(MinuteState.OFFLINE, RuntimeClassifier.classify(false, true, false));
        assertEquals(MinuteState.OFFLINE, RuntimeClassifier.classify(false, false, false));
    }

    @Test
    void u01_allUp() {
        DayStats d = RuntimeClassifier.summarize(1440, 0, 0, 0, CAP);
        assertEquals(0, new BigDecimal("100.000").compareTo(d.ratePct()));
        assertTrue(d.qualified());
        assertNull(d.penaltyScore());
    }

    @Test
    void u02_nonGridExit1Min() {
        // 1439/1440 = 99.931% ≥99 合格
        DayStats d = RuntimeClassifier.summarize(1439, 0, 1, 0, CAP);
        assertEquals(0, new BigDecimal("99.931").compareTo(d.ratePct()));
        assertTrue(d.qualified());
    }

    @Test
    void u03_gridExitExempt() {
        // 免责出分母：1439/(1439+0) = 100%
        DayStats d = RuntimeClassifier.summarize(1439, 1, 0, 0, CAP);
        assertEquals(0, new BigDecimal("100.000").compareTo(d.ratePct()));
        assertTrue(d.qualified());
    }

    @Test
    void u04_offlineNotCounted() {
        DayStats d = RuntimeClassifier.summarize(1080, 0, 0, 360, CAP);
        assertEquals(0, new BigDecimal("100.000").compareTo(d.ratePct()));
        assertTrue(d.qualified());
    }

    @Test
    void u05_massExitPenalty() {
        // 1410/1440 = 97.917% <99 → 罚 (99−97.917)×60×0.02 = 1.2996 分
        DayStats d = RuntimeClassifier.summarize(1410, 0, 30, 0, CAP);
        assertEquals(0, new BigDecimal("97.917").compareTo(d.ratePct()));
        assertFalse(d.qualified());
        assertEquals(0, new BigDecimal("1.29960").compareTo(d.penaltyScore()));
    }

    @Test
    void u06_stepHold60Min() {
        DayStats d = RuntimeClassifier.summarize(1380, 0, 60, 0, CAP);
        assertEquals(0, new BigDecimal("95.833").compareTo(d.ratePct()));
        assertFalse(d.qualified());
    }

    @Test
    void zeroDenominator() {
        // 全天未并网 → 无基数，率/罚款 null（非真 0%）
        DayStats d = RuntimeClassifier.summarize(0, 0, 0, 1440, CAP);
        assertNull(d.ratePct());
        assertNull(d.penaltyScore());
    }
}

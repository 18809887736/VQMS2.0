package com.ruoyi.vqms.statistics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.ruoyi.vqms.statistics.Art27Calculator.MonthResult;

class Art27CalculatorTest {

    private static BigDecimal bd(String v) { return new BigDecimal(v); }

    @Test
    void qualifiedZeroPenalty() {
        // 可用率 99.5% ≥ 99 → 无罚
        MonthResult r = Art27Calculator.month(bd("716.4"), bd("720"), bd("60000"), 0, 0);
        assertEquals(0, bd("99.500").compareTo(r.availabilityPct()));
        assertEquals(0, BigDecimal.ZERO.compareTo(r.totalPenalty()));
    }

    @Test
    void shortfallPenalty() {
        // 700/720h = 97.222%，缺额 1.778pp × 6万千瓦 × 0.1 = 1.067 分
        MonthResult r = Art27Calculator.month(bd("700"), bd("720"), bd("60000"), 0, 0);
        assertEquals(0, bd("97.222").compareTo(r.availabilityPct()));
        assertEquals(0, bd("1.778").compareTo(r.shortfallPct()));
        assertEquals(0, bd("1.067").compareTo(r.availabilityPenalty()));
    }

    @Test
    void dailyPenaltiesAndTotal() {
        // 速率 2 天 + 铭牌 1 天：3 × 0.1 × 6 = 1.8；可用率罚 1.067 → 合计 2.867
        MonthResult r = Art27Calculator.month(bd("700"), bd("720"), bd("60000"), 2, 1);
        assertEquals(0, bd("1.800").compareTo(r.daysPenalty()));
        assertEquals(0, bd("2.867").compareTo(r.totalPenalty()));
    }

    @Test
    void monthCapApplies() {
        // 大缺额 + 多天 → 合计超上限（6万千瓦 × 5 = 30 分）截断
        MonthResult r = Art27Calculator.month(bd("300"), bd("720"), bd("60000"), 20, 10);
        assertTrue(r.capped());
        assertEquals(0, bd("30.000").compareTo(r.cappedPenalty()));
        assertTrue(r.totalPenalty().compareTo(bd("30.000")) > 0);
    }

    @Test
    void zeroEnergizedNoBasis() {
        MonthResult r = Art27Calculator.month(bd("100"), BigDecimal.ZERO, bd("60000"), 0, 0);
        assertNull(r.availabilityPct());
        assertNull(r.totalPenalty());
    }
}

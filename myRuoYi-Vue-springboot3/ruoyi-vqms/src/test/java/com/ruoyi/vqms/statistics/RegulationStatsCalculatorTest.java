package com.ruoyi.vqms.statistics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.ruoyi.vqms.statistics.RegulationStatsCalculator.RegulationRates;
import com.ruoyi.vqms.statistics.RegulationStatsCalculator.TierRates;

class RegulationStatsCalculatorTest {

    private static final BigDecimal CAP = BigDecimal.valueOf(600000);

    @Test
    void fullRateNoPenalty() {
        TierRates t = RegulationStatsCalculator.tier(288, 288, 0, CAP);
        assertEquals(0, new BigDecimal("100.000").compareTo(t.ratePct()));
        assertNull(t.penaltyScore());
    }

    @Test
    void exemptedRemovedFromDenominator() {
        // total=100, qualified=98, exempted=2 → 98/(100−2)=100% 不罚（剔除法）
        TierRates t = RegulationStatsCalculator.tier(100, 98, 2, CAP);
        assertEquals(0, new BigDecimal("100.000").compareTo(t.ratePct()));
        assertNull(t.penaltyScore());
    }

    @Test
    void shortfallPenalty() {
        // 8500/8600 = 98.837%，缺额 1.163pp，罚 = 1.163×60×0.02 = 1.3956
        TierRates t = RegulationStatsCalculator.tier(8600, 8500, 0, CAP);
        assertEquals(0, new BigDecimal("98.837").compareTo(t.ratePct()));
        assertEquals(0, new BigDecimal("1.39560").compareTo(t.penaltyScore()));
    }

    @Test
    void invalidStaysInDenominator() {
        // 固定分母口径（规则表未配置）：invalid 不减
        TierRates t = RegulationStatsCalculator.tier(100, 99, 0, CAP);
        assertEquals(0, new BigDecimal("99.000").compareTo(t.ratePct()));
        assertEquals(0, new BigDecimal("1.20000").compareTo(t.penaltyScore()));
    }

    @Test
    void zeroDenominator() {
        TierRates t = RegulationStatsCalculator.tier(2, 0, 2, CAP);
        assertNull(t.ratePct());
        assertNull(t.penaltyScore());
    }

    @Test
    void twoTierTotal() {
        RegulationRates r = RegulationStatsCalculator.compute(100, 98, 2, 99, 0, CAP);
        assertEquals(0, new BigDecimal("100.000").compareTo(r.fast().ratePct()));
        assertEquals(0, new BigDecimal("99.000").compareTo(r.econ().ratePct()));
        assertEquals(0, new BigDecimal("1.20000").compareTo(r.penaltyTotal()));
        assertEquals(2, r.exemptedTotal());
    }
}

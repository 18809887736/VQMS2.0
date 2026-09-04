package com.ruoyi.vqms.statistics;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 第27条动态无功补偿装置考核纯函数（辅助服务细则 p11）：
 *  - 投入自动可用率 = Σ装置自动可用小时 / Σ升压变带电小时 × 100%，合格线 99%
 *  - 缺额每 1pp 月考 额定容量(万千瓦) × 0.1 分（小数 pp 连续计，口径对齐投运率 shortfall）
 *  - 调节速率/铭牌能力不符：每天 0.1 分/万千瓦（登记值参与合计）
 *  - 各项之和上限：额定容量(万千瓦) × 5 分/月
 *  - 风光储过渡期：细则执行 6 个月后开展考核（2025-04 起）
 */
public final class Art27Calculator {

    private static final BigDecimal QUALIFIED_LINE = new BigDecimal("99");
    private static final BigDecimal RATE_PENALTY_PER_PP = new BigDecimal("0.1");
    private static final BigDecimal DAILY_PENALTY = new BigDecimal("0.1");
    private static final BigDecimal MONTH_CAP = new BigDecimal("5");

    private Art27Calculator() {
    }

    /** 月度考核结果。 */
    public record MonthResult(BigDecimal availabilityPct, BigDecimal shortfallPct,
                              BigDecimal availabilityPenalty, BigDecimal daysPenalty,
                              BigDecimal totalPenalty, BigDecimal cappedPenalty, boolean capped) {
    }

    /**
     * 月度考核计算。
     *
     * @param autoHours      Σ装置投入自动可用小时
     * @param energizedHours Σ升压变带电小时（0 → 率/罚款 null，无考核基数）
     * @param ratedCapacityKw 装置额定容量 kW（考核基数）
     * @param ratePenaltyDays 调节速率不符天数（第 2 款，0=无）
     * @param nameplateDays  铭牌能力不符天数（第 3 款，0=无）
     */
    public static MonthResult month(BigDecimal autoHours, BigDecimal energizedHours,
                                    BigDecimal ratedCapacityKw,
                                    int ratePenaltyDays, int nameplateDays) {
        if (energizedHours == null || energizedHours.signum() == 0
                || autoHours == null || ratedCapacityKw == null) {
            return new MonthResult(null, null, null, null, null, null, false);
        }
        BigDecimal rate = autoHours.multiply(BigDecimal.valueOf(100))
                .divide(energizedHours, 3, RoundingMode.HALF_UP);
        BigDecimal shortfall = QUALIFIED_LINE.subtract(rate).max(BigDecimal.ZERO);
        BigDecimal capWmw = ratedCapacityKw.divide(BigDecimal.valueOf(10_000), 6, RoundingMode.HALF_UP);
        BigDecimal availabilityPenalty = shortfall.signum() > 0
                ? shortfall.multiply(capWmw).multiply(RATE_PENALTY_PER_PP).setScale(3, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal daysPenalty = BigDecimal.valueOf(ratePenaltyDays + nameplateDays)
                .multiply(DAILY_PENALTY).multiply(capWmw).setScale(3, RoundingMode.HALF_UP);
        BigDecimal total = availabilityPenalty.add(daysPenalty);
        BigDecimal cap = capWmw.multiply(MONTH_CAP).setScale(3, RoundingMode.HALF_UP);
        boolean hit = total.compareTo(cap) > 0;
        return new MonthResult(rate, shortfall, availabilityPenalty, daysPenalty,
                total, hit ? cap : total, hit);
    }
}

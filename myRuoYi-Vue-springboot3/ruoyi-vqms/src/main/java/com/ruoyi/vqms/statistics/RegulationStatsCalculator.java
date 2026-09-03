package com.ruoyi.vqms.statistics;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 调节合格率率/罚款查询层重算（纯函数，rollup 铁律：只对计数求和，绝不平均率列）。
 *
 * 口径（设计文档 3.12 分层）：
 *  - exempted = 附件6 明文豁免、剔除法天然出分母
 *  - invalid/undecodable 是否减分母由 policy_param 当前生效规则表决定——未配置=固定分母（不减）
 *  - 合格线 100%；罚分 = 缺额pp × 容量(kW)/10⁴ × 0.02；缺额=0 不产罚款
 *  - 分母为 0 → 率 null（无发令基数）
 */
public final class RegulationStatsCalculator {

    public static final BigDecimal QUALIFIED_LINE = BigDecimal.valueOf(100);

    /** 单档率/缺额/罚分。 */
    public record TierRates(BigDecimal ratePct, BigDecimal shortfallPct, BigDecimal penaltyScore) {
    }

    /** 合计视图（两档 + 免考总数）。 */
    public record RegulationRates(TierRates fast, TierRates econ,
                                  int exemptedTotal, BigDecimal penaltyTotal) {
    }

    private RegulationStatsCalculator() {
    }

    /** 单档计算：分母 = total − exempted（剔除法）；capacity 为 null 不产罚款。 */
    public static TierRates tier(int total, int qualified, int exempted, BigDecimal capacityKw) {
        int denominator = total - exempted;
        if (denominator <= 0) {
            return new TierRates(null, null, null);
        }
        BigDecimal rate = BigDecimal.valueOf(qualified * 100L)
                .divide(BigDecimal.valueOf(denominator), 3, RoundingMode.HALF_UP);
        BigDecimal shortfall = QUALIFIED_LINE.subtract(rate).max(BigDecimal.ZERO);
        BigDecimal penalty = null;
        if (capacityKw != null && shortfall.signum() > 0) {
            penalty = shortfall.multiply(capacityKw)
                    .divide(BigDecimal.valueOf(10_000), 3, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("0.02"));
        }
        return new TierRates(rate, shortfall, penalty);
    }

    /** 两档合计。 */
    public static RegulationRates compute(int total,
                                          int qualifiedFast, int exemptedFast,
                                          int qualifiedEcon, int exemptedEcon,
                                          BigDecimal capacityKw) {
        TierRates fast = tier(total, qualifiedFast, exemptedFast, capacityKw);
        TierRates econ = tier(total, qualifiedEcon, exemptedEcon, capacityKw);
        BigDecimal total2 = null;
        if (fast.penaltyScore() != null && econ.penaltyScore() != null) {
            total2 = fast.penaltyScore().add(econ.penaltyScore());
        } else if (fast.penaltyScore() != null) {
            total2 = fast.penaltyScore();
        } else if (econ.penaltyScore() != null) {
            total2 = econ.penaltyScore();
        }
        return new RegulationRates(fast, econ, exemptedFast + exemptedEcon, total2);
    }
}

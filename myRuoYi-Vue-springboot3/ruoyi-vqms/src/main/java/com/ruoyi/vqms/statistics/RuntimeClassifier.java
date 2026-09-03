package com.ruoyi.vqms.statistics;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 投运率逐分钟分类与统计（纯函数）。
 *
 * 口径（1.0 正式 v1_0 §1 + 附件6）：
 *  - 四桶：投运 / 非电网退出（扣罚，在分母）/ 电网退出（免责，出分母）/ 未并网（不计账）
 *  - 并网 = yc511≥10 或 yc512≥10（带电×10+机数 编码）
 *  - 并网 且 AVC 投 → 投运；并网 且 AVC 退 → 按退出原因分桶（521/522=1 电网免责；其余一律非电网，从严）
 *  - 投运率 = 投运 / (投运 + 非电网退出) × 100；合格线 99%；
 *    分母为 0（零并网分钟）→ 率无效（NULL 语义，非真 0%）
 *  - 罚分 = max(0, 99−率) × 容量(kW)/10⁴ × 0.02
 */
public final class RuntimeClassifier {

    public static final BigDecimal QUALIFIED_LINE = new BigDecimal("99");

    public enum MinuteState { IN_SERVICE, EXIT_NON_GRID, EXIT_GRID, OFFLINE }

    /** 单分钟分类。grid=并网；avcIn=AVC 投；exitGridFlag=退出原因含电网(521/522 任一=1)。 */
    public static MinuteState classify(boolean grid, boolean avcIn, boolean exitGridFlag) {
        if (!grid) {
            return MinuteState.OFFLINE;
        }
        if (avcIn) {
            return MinuteState.IN_SERVICE;
        }
        return exitGridFlag ? MinuteState.EXIT_GRID : MinuteState.EXIT_NON_GRID;
    }

    /** 并网编码判据：值 = 带电(1/0)×10 + 机组数，≥10 即并网。null 视为无信号（不带电）。 */
    public static boolean isGrid(BigDecimal code) {
        return code != null && code.compareTo(BigDecimal.TEN) >= 0;
    }

    /** 日统计结果（率/罚款为纯函数推导值，写快照用）。 */
    public record DayStats(int inServiceMin, int exitGridMin, int exitNonGridMin, int offlineMin,
                           BigDecimal ratePct, boolean qualified, BigDecimal shortfallPct,
                           BigDecimal penaltyScore) {
    }

    /** 由四桶分钟数 + 容量(kW) 汇总。零分母 → 率/罚款 null（无并网基数）。 */
    public static DayStats summarize(int inService, int exitGrid, int exitNonGrid, int offline,
                                     BigDecimal capacityKw) {
        int denominator = inService + exitNonGrid;
        BigDecimal rate = denominator == 0 ? null
                : BigDecimal.valueOf(inService * 100L).divide(BigDecimal.valueOf(denominator), 3, RoundingMode.HALF_UP);
        boolean qualified = rate != null && rate.compareTo(QUALIFIED_LINE) >= 0;
        BigDecimal shortfall = rate == null ? null
                : QUALIFIED_LINE.subtract(rate).max(BigDecimal.ZERO);
        BigDecimal penalty = null;
        if (shortfall != null && capacityKw != null && shortfall.signum() > 0) {
            penalty = shortfall.multiply(capacityKw)
                    .divide(BigDecimal.valueOf(10_000), 3, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("0.02"));
        }
        return new DayStats(inService, exitGrid, exitNonGrid, offline, rate, qualified, shortfall, penalty);
    }
}

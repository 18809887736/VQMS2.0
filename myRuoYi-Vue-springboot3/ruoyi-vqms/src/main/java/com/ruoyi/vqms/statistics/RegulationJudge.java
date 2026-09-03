package com.ruoyi.vqms.statistics;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 调节合格率判定器（纯函数，包络法）。
 *
 * 口径（1.0 正式 v1_0 §2 + 2.0 拍板沿用）：
 *  - 两档平行互不隶属：快速性窗口 [1, t_fast]、经济性窗口 [t_fast+1, t_econ]，各自判各自记
 *  - 包络判据：V_target ∈ [min(low_SV), max(high_SV)]（窗口内夹住即该档合格，边界含等号）
 *  - 档无效（INVALID）：窗口全缺数据，或窗口存在 low>high 异常行（该档剔除）
 *  - completeness：窗口内有数据分钟数 / 窗口总分钟数（部分缺不影响聚合）
 *  - 免考（EXEMPT）：该档判不合格后，若免考旗采样为真（AUTO_YX yx501 等，旗值由管线采样后传入）
 *
 * states：QUALIFIED / PENALIZED / EXEMPTED / INVALID（与 vqms_regulation_cmd.fast_state 同词）。
 */
public final class RegulationJudge {

    public static final String QUALIFIED = "QUALIFIED";
    public static final String PENALIZED = "PENALIZED";
    public static final String EXEMPTED = "EXEMPTED";
    public static final String INVALID = "INVALID";

    /** 单分钟电压带（kV）。valid=false 表示该行 low>high 异常。 */
    public record Band(BigDecimal low, BigDecimal high, boolean valid) {
    }

    /**
     * 判定结果：两档状态 + 完整度 + 原始按档无效标记（FAST/ECON/FAST,ECON/null）。
     */
    public record Outcome(String fastState, String econState,
                          BigDecimal completenessFast, BigDecimal completenessEcon,
                          String invalidTiers) {
    }

    private RegulationJudge() {
    }

    /**
     * 判定一条指令。
     *
     * @param targetKv   解码后目标电压（null → 两档均 INVALID，解码失败不上判定）
     * @param curve      分钟 → 电压带（主判定母线，已过闸门）
     * @param t0         指令对齐分钟
     * @param tFast      快速性档窗口上限（分钟，含）
     * @param tEcon      经济性档窗口上限（分钟，含，恒 =5）
     * @param exemptFast 快档免考旗采样（true → PEN 转 EXEMPTED）
     * @param exemptEcon 经档免考旗采样
     */
    public static Outcome judge(BigDecimal targetKv, Map<LocalDateTime, Band> curve,
                                LocalDateTime t0, int tFast, int tEcon,
                                boolean exemptFast, boolean exemptEcon) {
        if (targetKv == null) {
            return new Outcome(INVALID, INVALID, BigDecimal.ZERO, BigDecimal.ZERO, "FAST,ECON");
        }
        TierResult fast = judgeTier(targetKv, curve, t0.plusMinutes(1), t0.plusMinutes(tFast));
        TierResult econ = judgeTier(targetKv, curve, t0.plusMinutes(tFast + 1), t0.plusMinutes(tEcon));

        String fastState = fast.state();
        if (PENALIZED.equals(fastState) && exemptFast) {
            fastState = EXEMPTED;
        }
        String econState = econ.state();
        if (PENALIZED.equals(econState) && exemptEcon) {
            econState = EXEMPTED;
        }
        String invalidTiers = null;
        if (fast.invalid && econ.invalid) {
            invalidTiers = "FAST,ECON";
        } else if (fast.invalid) {
            invalidTiers = "FAST";
        } else if (econ.invalid) {
            invalidTiers = "ECON";
        }
        return new Outcome(fastState, econState, fast.completeness, econ.completeness, invalidTiers);
    }

    private record TierResult(String state, BigDecimal completeness, boolean invalid) {
    }

    private static TierResult judgeTier(BigDecimal targetKv, Map<LocalDateTime, Band> curve,
                                        LocalDateTime from, LocalDateTime to) {
        long total = java.time.Duration.between(from, to).toMinutes() + 1;
        List<Band> bands = new ArrayList<>();
        for (LocalDateTime m = from; !m.isAfter(to); m = m.plusMinutes(1)) {
            Band b = curve.get(m);
            if (b != null) {
                bands.add(b);
            }
        }
        if (bands.isEmpty()) {
            return new TierResult(INVALID, BigDecimal.ZERO, true);
        }
        // 窗口存在 low>high 异常行 → 该档无效（S16 口径）
        if (bands.stream().anyMatch(b -> !b.valid())) {
            return new TierResult(INVALID, completenessOf(bands.size(), total), true);
        }
        BigDecimal minLow = bands.stream().map(Band::low).reduce(BigDecimal::min).orElseThrow();
        BigDecimal maxHigh = bands.stream().map(Band::high).reduce(BigDecimal::max).orElseThrow();
        boolean inBand = targetKv.compareTo(minLow) >= 0 && targetKv.compareTo(maxHigh) <= 0;
        return new TierResult(inBand ? QUALIFIED : PENALIZED, completenessOf(bands.size(), total), false);
    }

    private static BigDecimal completenessOf(int have, long total) {
        if (total <= 0) {
            return BigDecimal.ONE;
        }
        return BigDecimal.valueOf(have).divide(BigDecimal.valueOf(total), 4, java.math.RoundingMode.HALF_UP)
                .min(BigDecimal.ONE);
    }
}

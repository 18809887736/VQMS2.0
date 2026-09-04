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

    /**
     * 数据不可用处置策略（原子化，界面可整定，核实单口径族）：
     * 每类失效独立处置——0=剔除分母（INVALID，保守不罚）/ 1=计不合格（PENALIZED，倒逼数据质量）。
     * A3/A4（部分缺/完整度）由 minCompleteness τ 闸门承载（独立参数）。
     */
    public record JudgePolicy(BigDecimal minCompleteness,
                              int undecodableAction,      // A1 解码失败（A1a/A1b/A1c 合并）
                              int windowMissingAction,    // A2 窗口全缺
                              int bandInvertedAction      // A2 窗口存在 low>high 异常行
    ) {
        public static JudgePolicy defaults() {
            return new JudgePolicy(null, 0, 0, 0);
        }

        static JudgePolicy withTau(BigDecimal tau) {
            return new JudgePolicy(tau, 0, 0, 0);
        }
    }

    /** 单分钟电压带（kV）。valid=false 表示该行 low>high 异常。 */
    public record Band(BigDecimal low, BigDecimal high, boolean valid) {
    }

    /**
     * 数据质量闸门（现场库核对报告发现④）：his_curve_sv 存在 high/low = 0.0 采集坏点，
     * 不拦则包络被毒化（low=0 → 下界无限宽 → 误判合格）。
     * 任一侧 ≤ 0 视为脏行不采信 → 返回 null（该分钟等同缺数，走 completeness 降级链）；
     * low>high 保留行但标 invalid（S16 口径不变）。
     */
    public static Band sanitizeBand(BigDecimal low, BigDecimal high) {
        if (low == null || high == null) {
            return null;
        }
        if (low.signum() <= 0 || high.signum() <= 0) {
            return null;
        }
        return new Band(low, high, low.compareTo(high) <= 0);
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
                                boolean exemptFast, boolean exemptEcon, BigDecimal minCompleteness) {
        return judge(targetKv, curve, t0, tFast, tEcon, exemptFast, exemptEcon,
                JudgePolicy.withTau(minCompleteness));
    }

    /** 兼容默认策略（全部剔除分母、τ 关闭）。 */
    public static Outcome judge(BigDecimal targetKv, Map<LocalDateTime, Band> curve,
                                LocalDateTime t0, int tFast, int tEcon,
                                boolean exemptFast, boolean exemptEcon) {
        return judge(targetKv, curve, t0, tFast, tEcon, exemptFast, exemptEcon, JudgePolicy.defaults());
    }

    /**
     * 判定一条指令（带完整度闸门）。
     *
     * @param minCompleteness 档窗口最低完整度（0~1，null 或 0=关闭）：
     *                        completeness &lt; τ → 该档 INVALID（数据公平性：缺数窗不硬判，
     *                        1.0 数据不可用策略 A3/A4 最小口径；τ 由 vqms_judge_param.min_window_completeness_pct 整定）
     */
    public static Outcome judge(BigDecimal targetKv, Map<LocalDateTime, Band> curve,
                                LocalDateTime t0, int tFast, int tEcon,
                                boolean exemptFast, boolean exemptEcon, JudgePolicy policy) {
        if (targetKv == null) {
            // A1 解码失败处置：0=INVALID 剔除分母（默认）/ 1=PENALIZED 计不合格
            String st = policy.undecodableAction() == 1 ? PENALIZED : INVALID;
            return new Outcome(st, st, BigDecimal.ZERO, BigDecimal.ZERO, INVALID.equals(st) ? "FAST,ECON" : null);
        }
        TierResult fast = judgeTier(targetKv, curve, t0.plusMinutes(1), t0.plusMinutes(tFast), policy);
        TierResult econ = judgeTier(targetKv, curve, t0.plusMinutes(tFast + 1), t0.plusMinutes(tEcon), policy);

        String fastState = fast.state();
        if (PENALIZED.equals(fastState) && exemptFast) {
            fastState = EXEMPTED;
        }
        String econState = econ.state();
        if (PENALIZED.equals(econState) && exemptEcon) {
            econState = EXEMPTED;
        }
        String invalidTiers = null;
        // invalidTiers 只标 INVALID 状态的档（处置=计不合格时不是"无效"，不标）
        if (INVALID.equals(fastState) && INVALID.equals(econState)) {
            invalidTiers = "FAST,ECON";
        } else if (INVALID.equals(fastState)) {
            invalidTiers = "FAST";
        } else if (INVALID.equals(econState)) {
            invalidTiers = "ECON";
        }
        return new Outcome(fastState, econState, fast.completeness, econ.completeness, invalidTiers);
    }

    private record TierResult(String state, BigDecimal completeness, boolean invalid) {
    }

    private static TierResult judgeTier(BigDecimal targetKv, Map<LocalDateTime, Band> curve,
                                        LocalDateTime from, LocalDateTime to, JudgePolicy policy) {
        long total = java.time.Duration.between(from, to).toMinutes() + 1;
        List<Band> bands = new ArrayList<>();
        for (LocalDateTime m = from; !m.isAfter(to); m = m.plusMinutes(1)) {
            Band b = curve.get(m);
            if (b != null) {
                bands.add(b);
            }
        }
        if (bands.isEmpty()) {
            // A2 窗口全缺处置：0=INVALID 剔除（默认）/ 1=PENALIZED 计不合格
            return new TierResult(policy.windowMissingAction() == 1 ? PENALIZED : INVALID,
                    BigDecimal.ZERO, true);
        }
        // 窗口存在 low>high 异常行 → 处置：0=该档 INVALID 剔除（S16 默认）/ 1=PENALIZED 计不合格
        if (bands.stream().anyMatch(b -> !b.valid())) {
            return new TierResult(policy.bandInvertedAction() == 1 ? PENALIZED : INVALID,
                    completenessOf(bands.size(), total), true);
        }
        // 完整度闸门（A3/A4 最小口径）：可用度 < τ 的窗口不硬判——缺数缺出来的"不合格"不是电厂责任
        BigDecimal completeness = completenessOf(bands.size(), total);
        BigDecimal minCompleteness = policy.minCompleteness();
        if (minCompleteness != null && minCompleteness.signum() > 0
                && completeness.compareTo(minCompleteness) < 0) {
            return new TierResult(INVALID, completeness, true);
        }
        BigDecimal minLow = bands.stream().map(Band::low).reduce(BigDecimal::min).orElseThrow();
        BigDecimal maxHigh = bands.stream().map(Band::high).reduce(BigDecimal::max).orElseThrow();
        boolean inBand = targetKv.compareTo(minLow) >= 0 && targetKv.compareTo(maxHigh) <= 0;
        return new TierResult(inBand ? QUALIFIED : PENALIZED, completeness, false);
    }

    private static BigDecimal completenessOf(int have, long total) {
        if (total <= 0) {
            return BigDecimal.ONE;
        }
        return BigDecimal.valueOf(have).divide(BigDecimal.valueOf(total), 4, java.math.RoundingMode.HALF_UP)
                .min(BigDecimal.ONE);
    }
}

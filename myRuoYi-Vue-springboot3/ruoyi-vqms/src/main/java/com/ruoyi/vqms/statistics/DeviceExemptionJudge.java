package com.ruoyi.vqms.statistics;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 设备级免考判定纯函数（附件6 §三）：
 * 纳入 AVC 闭环控制的全部无功设备，在正确方向上顶到各自极限仍不达标 → 该时段免考。
 *
 * 方向规约：电压偏低（低于目标区间）→ 需要发出（INJECT，顶发出上限）；
 *           电压偏高（高于目标区间）→ 需要吸收（ABSORB，顶吸收下限）。
 * 极限按设备类型解析：
 *   1 同步发电机/调相机：P-Q 曲线按当前 P 线性插值（无曲线则回退静态额定）
 *   2 逆变器型（风/光/储）：Q = ±√(S²−P²)
 *   3 SVC/STATCOM：静态双向额定
 *   4 电容器组：单向发出（吸收能力为 0）
 *   5 电抗器：单向吸收（发出能力为 0）
 * 任一闭环设备遥测缺失、方向错误或留有余力 → 不免考（保守口径：未尽力的举证责任在电厂侧）。
 */
public final class DeviceExemptionJudge {

    private DeviceExemptionJudge() {
    }

    /** 需要的无功方向。 */
    public enum Direction { INJECT, ABSORB }

    /**
     * 方向推导：采样时刻实际电压区间相对目标区间的位置。
     * 低于目标区间下沿 → INJECT；高于上沿 → ABSORB；在区间内 → null（已达标，免考判定无意义）。
     */
    public static Direction resolveDirection(BigDecimal targetLow, BigDecimal targetHigh,
                                             BigDecimal actualLow, BigDecimal actualHigh) {
        if (targetLow == null || targetHigh == null || actualLow == null || actualHigh == null) {
            return null;
        }
        if (actualHigh.compareTo(targetLow) < 0) {
            return Direction.INJECT;
        }
        if (actualLow.compareTo(targetHigh) > 0) {
            return Direction.ABSORB;
        }
        return null;
    }

    /** P-Q 曲线单点。 */
    public record PqPoint(BigDecimal pKw, BigDecimal qUpKvar, BigDecimal qDownKvar) {
    }

    /**
     * 单设备判定输入快照。
     *
     * @param inAvcLoop 是否纳入 AVC 闭环（false 整体跳过——政策只考察闭环设备）
     * @param pKw       当前有功 kW（发电机曲线插值 / 逆变器容量圆用；可空）
     * @param qKvar     当前无功 kvar（可空=遥测缺失）
     */
    public record DeviceSample(long deviceId, String deviceCode, int deviceType, boolean inAvcLoop,
                               BigDecimal pKw, BigDecimal qKvar,
                               BigDecimal ratedSKva, BigDecimal ratedQUpKvar, BigDecimal ratedQDownKvar,
                               List<PqPoint> pqCurve) {
        public DeviceSample {
            pqCurve = pqCurve == null ? List.of() : List.copyOf(pqCurve);
        }
    }

    /**
     * 判定结论。
     *
     * @param exempted 全部闭环设备正确方向顶到极限 → true
     * @param blockers 未尽力设备清单（设备号 + 原因；遥测缺失/方向错误/留有余力/极限不可解析）
     */
    public record Verdict(boolean exempted, List<String> blockers) {
        static Verdict pass() {
            return new Verdict(true, List.of());
        }
    }

    /**
     * 判定入口。
     *
     * @param devices   全量设备快照（闭环过滤在函数内做）
     * @param direction 需要的无功方向（null=方向不可判定，保守不免考）
     * @param tolKvar   顶满容差 kvar（|Q − 极限| ≤ tol 视为顶满，现场整定）
     */
    public static Verdict judge(List<DeviceSample> devices, Direction direction, BigDecimal tolKvar) {
        if (direction == null) {
            return new Verdict(false, List.of("DIRECTION_UNRESOLVED"));
        }
        BigDecimal tol = tolKvar == null ? BigDecimal.ZERO : tolKvar;
        List<String> blockers = new ArrayList<>();
        boolean anyLoop = false;
        for (DeviceSample d : devices) {
            if (!d.inAvcLoop()) {
                continue;
            }
            anyLoop = true;
            if (d.qKvar() == null) {
                blockers.add(d.deviceCode() + ":Q_MISSING");
                continue;
            }
            BigDecimal limit = resolveLimit(d, direction);
            if (limit == null) {
                blockers.add(d.deviceCode() + ":LIMIT_UNRESOLVED");
                continue;
            }
            if (direction == Direction.INJECT) {
                // 顶发出上限：Q ≥ limit − tol
                if (d.qKvar().compareTo(limit.subtract(tol)) < 0) {
                    blockers.add(d.deviceCode() + ":NOT_AT_Q_UP(q=" + d.qKvar().toPlainString()
                            + ",limit=" + limit.toPlainString() + ")");
                }
            } else {
                // 顶吸收下限：Q ≤ limit + tol（limit 为负值）
                if (d.qKvar().compareTo(limit.add(tol)) > 0) {
                    blockers.add(d.deviceCode() + ":NOT_AT_Q_DOWN(q=" + d.qKvar().toPlainString()
                            + ",limit=" + limit.toPlainString() + ")");
                }
            }
        }
        if (!anyLoop) {
            return new Verdict(false, List.of("NO_AVC_LOOP_DEVICE"));
        }
        return blockers.isEmpty() ? Verdict.pass() : new Verdict(false, blockers);
    }

    /** 按类型解析当前方向极限 kvar；不可解析返回 null。 */
    private static BigDecimal resolveLimit(DeviceSample d, Direction direction) {
        return switch (d.deviceType()) {
            case 1 -> generatorLimit(d, direction);
            case 2 -> inverterLimit(d, direction);
            case 3 -> staticLimit(d, direction);
            case 4 -> direction == Direction.INJECT ? d.ratedQUpKvar() : BigDecimal.ZERO;
            case 5 -> direction == Direction.ABSORB ? d.ratedQDownKvar() : BigDecimal.ZERO;
            default -> null;
        };
    }

    /** 发电机类：P-Q 曲线插值；无曲线回退静态额定。 */
    private static BigDecimal generatorLimit(DeviceSample d, Direction direction) {
        if (d.pqCurve().isEmpty()) {
            return staticLimit(d, direction);
        }
        if (d.pKw() == null) {
            return null;
        }
        List<PqPoint> curve = d.pqCurve().stream()
                .sorted(Comparator.comparing(PqPoint::pKw)).toList();
        BigDecimal p = d.pKw();
        if (p.compareTo(curve.get(0).pKw()) <= 0) {
            return direction == Direction.INJECT ? curve.get(0).qUpKvar() : curve.get(0).qDownKvar();
        }
        if (p.compareTo(curve.get(curve.size() - 1).pKw()) >= 0) {
            PqPoint last = curve.get(curve.size() - 1);
            return direction == Direction.INJECT ? last.qUpKvar() : last.qDownKvar();
        }
        for (int i = 1; i < curve.size(); i++) {
            PqPoint hi = curve.get(i);
            PqPoint lo = curve.get(i - 1);
            if (p.compareTo(hi.pKw()) <= 0) {
                BigDecimal span = hi.pKw().subtract(lo.pKw());
                BigDecimal ratio = p.subtract(lo.pKw()).divide(span, MathContext.DECIMAL64);
                return direction == Direction.INJECT
                        ? lo.qUpKvar().add(hi.qUpKvar().subtract(lo.qUpKvar()).multiply(ratio))
                        : lo.qDownKvar().add(hi.qDownKvar().subtract(lo.qDownKvar()).multiply(ratio));
            }
        }
        return null;
    }

    /** 逆变器型：Q = ±√(S²−P²)。 */
    private static BigDecimal inverterLimit(DeviceSample d, Direction direction) {
        if (d.ratedSKva() == null || d.pKw() == null) {
            return null;
        }
        BigDecimal s2 = d.ratedSKva().multiply(d.ratedSKva());
        BigDecimal p2 = d.pKw().multiply(d.pKw());
        if (p2.compareTo(s2) >= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal q = s2.subtract(p2).sqrt(MathContext.DECIMAL64);
        return direction == Direction.INJECT ? q : q.negate();
    }

    private static BigDecimal staticLimit(DeviceSample d, Direction direction) {
        return direction == Direction.INJECT ? d.ratedQUpKvar() : d.ratedQDownKvar();
    }
}

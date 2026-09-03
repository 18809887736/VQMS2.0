package com.ruoyi.vqms.statistics;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 指令解码器 ROT10_V1（纯函数）。
 *
 * 口径：现场核对报告发现①（阻断级），2026-09-02 VQMS2.0 权威口径，
 * 与造数器 src/decode.py 互为跨语言奇偶校验（tests 样例 = Python L0 样例）。
 *
 * 目标值形态：'收到远方遥调执行指令:220KV目标值,12315.4.'
 *   编码 = 首位轮转码{1,2,3}（均匀轮转，不参与数值）+ 余值（可含一位小数）÷ 10 = kV
 *   '12315.4' → 231.54；'12340' → 234.0
 * 增量形态：'收到远方遥调执行指令:辽宁母线电压增量指令编码值处理,2202.'
 *   4 位整数：第1位方向(2=加/1=减) · 第2位循环码 · 第3-4位幅值(×100V)
 *   V_target = t0 实时电压 ± 幅值
 */
public final class VTargetDecoder {

    private static final Pattern TRAILING_CODE = Pattern.compile(",([\\w.]+)\\.\\s*$");
    private static final Set<String> ROTATE_CODES = Set.of("1", "2", "3");

    private VTargetDecoder() {
    }

    /** 目标值解码；失败（脏值/轮转码非法/空余值）返回 null。 */
    public static BigDecimal decodeTargetValue(String text) {
        String code = extractCode(text);
        if (code == null || code.length() < 2) {
            return null;
        }
        String rot = code.substring(0, 1);
        String rest = code.substring(1);
        if (!ROTATE_CODES.contains(rot)) {
            return null;
        }
        try {
            return new BigDecimal(rest).divide(BigDecimal.TEN, 2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 增量解码：V_target = realtime ± 幅值(kV)。缺实时电压/编码非法返回 null。
     *  编码形态 dCmm（4 位）：d=方向 1 降/2 升、C=循环码（合法域 0..5，对齐 decodeTargetValue 轮转码校验）、mm=幅值×0.1kV。 */
    public static BigDecimal decodeIncrement(String text, BigDecimal realtimeKv) {
        String code = extractCode(text);
        if (code == null || code.length() != 4 || code.contains(".")) {
            return null;
        }
        if (realtimeKv == null) {
            return null;
        }
        int direction;
        int cycleCode;
        int magnitudeUnits;
        try {
            direction = Integer.parseInt(code.substring(0, 1));
            cycleCode = Integer.parseInt(code.substring(1, 2));
            magnitudeUnits = Integer.parseInt(code.substring(2, 4));
        } catch (NumberFormatException e) {
            return null;
        }
        if (cycleCode < 0 || cycleCode > 5) {
            return null; // 循环码非法（脏增量码不宽松收进——现场零实例形态，从严）
        }
        BigDecimal delta = BigDecimal.valueOf(magnitudeUnits).multiply(BigDecimal.valueOf(0.1));
        if (direction == 2) {
            return realtimeKv.add(delta).setScale(2, RoundingMode.HALF_UP);
        }
        if (direction == 1) {
            return realtimeKv.subtract(delta).setScale(2, RoundingMode.HALF_UP);
        }
        return null;
    }

    /** 自动识别：含"目标值"走目标值解码，否则按增量。 */
    public static BigDecimal decodeAny(String text, BigDecimal realtimeKv) {
        if (text != null && text.contains("目标值")) {
            return decodeTargetValue(text);
        }
        return decodeIncrement(text, realtimeKv);
    }

    private static String extractCode(String text) {
        if (text == null) {
            return null;
        }
        Matcher m = TRAILING_CODE.matcher(text.trim());
        return m.find() ? m.group(1) : null;
    }
}

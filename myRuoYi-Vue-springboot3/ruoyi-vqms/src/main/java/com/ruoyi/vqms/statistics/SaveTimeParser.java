package com.ruoyi.vqms.statistics;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

/**
 * 外部源 varchar 时间解析（纯函数）。
 *
 * 源库时间列均为 varchar，两种形态：
 *   '2021-05-07 15:27:57.556'（带三位毫秒）/ '2026-03-15 10:00:00'（不带）
 * 全部按北京历法（Asia/Shanghai）解释；秒≥30 进位到分钟（就近取整），
 * 与造数器 jitter（写前一分 57/58 秒）互为回环。
 */
public final class SaveTimeParser {

    private static final Pattern WITH_MS = Pattern.compile(
            "^(\\d{4}-\\d{2}-\\d{2}) (\\d{2}):(\\d{2}):(\\d{2})\\.(\\d{1,6})$");
    private static final Pattern WITHOUT_MS = Pattern.compile(
            "^(\\d{4}-\\d{2}-\\d{2}) (\\d{2}):(\\d{2}):(\\d{2})$");

    private static final DateTimeFormatter BASE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private SaveTimeParser() {
    }

    /**
     * 解析并对齐到分钟（秒≥30 进位）。非法格式返回 null（调用方按数据不可用处理）。
     */
    public static LocalDateTime parseToMinute(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.trim();
        if (WITH_MS.matcher(s).matches() || WITHOUT_MS.matcher(s).matches()) {
            try {
                LocalDateTime dt = LocalDateTime.parse(s.length() > 19 ? s.substring(0, 19) : s, BASE);
                return dt.getSecond() >= 30 ? dt.plusMinutes(1).withSecond(0).withNano(0)
                        : dt.withSecond(0).withNano(0);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    /** 仅解析不取整（保留原始秒，毫秒忽略）。非法返回 null。 */
    public static LocalDateTime parseRaw(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.trim();
        if (WITH_MS.matcher(s).matches() || WITHOUT_MS.matcher(s).matches()) {
            try {
                return LocalDateTime.parse(s.length() > 19 ? s.substring(0, 19) : s, BASE);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }
}

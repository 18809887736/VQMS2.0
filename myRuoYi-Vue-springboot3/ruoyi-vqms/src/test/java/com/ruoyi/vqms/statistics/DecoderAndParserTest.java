package com.ruoyi.vqms.statistics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

/**
 * 纯函数单测——样例与造数器 tools/avc-data-gen tests/test_l0_units.py 严格一致（跨语言奇偶校验）。
 */
class DecoderAndParserTest {

    private static BigDecimal kv(String s) {
        return new BigDecimal(s);
    }

    /** 数值相等断言（BigDecimal equals 严格比较 scale，跨形态解码用 compareTo）。 */
    private static void assertKv(String expected, BigDecimal actual) {
        assertEquals(0, kv(expected).compareTo(actual), "expected " + expected + " but was " + actual);
    }

    // ── VTargetDecoder：目标值（ROT10_V1）──

    @Test
    void decodeTargetRotate() {
        assertKv("231.54", VTargetDecoder.decodeTargetValue("收到远方遥调执行指令:220KV目标值,12315.4."));
        assertKv("234.0", VTargetDecoder.decodeTargetValue("收到远方遥调执行指令:220KV目标值,12340."));
        assertKv("223.15", VTargetDecoder.decodeTargetValue("收到远方遥调执行指令:220KV目标值,12231.5."));
        assertKv("223.15", VTargetDecoder.decodeTargetValue("收到远方遥调执行指令:220KV目标值,22231.5."));
        assertKv("223.15", VTargetDecoder.decodeTargetValue("收到远方遥调执行指令:220KV目标值,32231.5."));
        assertKv("225.0", VTargetDecoder.decodeTargetValue("收到远方遥调执行指令:220KV目标值,12250."));
    }

    @Test
    void decodeTargetFailures() {
        assertNull(VTargetDecoder.decodeTargetValue("收到远方遥调执行指令:主省220KV目标值,abc."));
        assertNull(VTargetDecoder.decodeTargetValue("收到远方遥调执行指令:220KV目标值,02231.5."));
        assertNull(VTargetDecoder.decodeTargetValue(null));
    }

    // ── VTargetDecoder：增量 ──

    @Test
    void decodeIncrement() {
        assertKv("234.45", VTargetDecoder.decodeIncrement(
                "收到远方遥调执行指令:辽宁母线电压增量指令编码值处理,2202.", kv("234.25")));
        assertKv("234.05", VTargetDecoder.decodeIncrement(
                "收到远方遥调执行指令:辽宁母线电压增量指令编码值处理,1202.", kv("234.25")));
        assertNull(VTargetDecoder.decodeIncrement("...,2202.", null));
    }

    @Test
    void decodeAnyDispatch() {
        assertKv("231.54", VTargetDecoder.decodeAny("...目标值,12315.4.", null));
        assertKv("234.45", VTargetDecoder.decodeAny("...增量指令...,2202.", kv("234.25")));
    }

    // ── SaveTimeParser：取整边界 ──

    @Test
    void parseRoundBoundaries() {
        assertEquals("10:00", SaveTimeParser.parseToMinute("2026-03-15 10:00:29.400").toString().substring(11, 16));
        assertEquals("10:01", SaveTimeParser.parseToMinute("2026-03-15 10:00:30.400").toString().substring(11, 16));
        assertEquals("10:00", SaveTimeParser.parseToMinute("2026-03-15 10:00:00").toString().substring(11, 16));
        assertEquals("10:59", SaveTimeParser.parseToMinute("2026-03-15 10:59:29").toString().substring(11, 16));
        assertEquals("11:00", SaveTimeParser.parseToMinute("2026-03-15 10:59:30").toString().substring(11, 16));
        // 毫秒不影响进位判定（秒位为准）
        assertEquals("10:00", SaveTimeParser.parseToMinute("2026-03-15 10:00:29.999").toString().substring(11, 16));
    }

    @Test
    void parseJitterRoundTrip() {
        // 造数器 jitter：写前一分 57/58 秒 → 取整回目标分钟
        assertEquals(LocalDateTime.of(2026, 3, 15, 10, 0), SaveTimeParser.parseToMinute("2026-03-15 09:59:57.100"));
        assertEquals(LocalDateTime.of(2026, 3, 15, 10, 0), SaveTimeParser.parseToMinute("2026-03-15 09:59:58.237"));
    }

    @Test
    void parseFailures() {
        assertNull(SaveTimeParser.parseToMinute(null));
        assertNull(SaveTimeParser.parseToMinute("garbage"));
        assertNull(SaveTimeParser.parseToMinute("2026-13-45 99:99:99"));
    }
}

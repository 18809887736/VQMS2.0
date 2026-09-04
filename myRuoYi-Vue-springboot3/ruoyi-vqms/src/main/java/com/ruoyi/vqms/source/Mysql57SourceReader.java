package com.ruoyi.vqms.source;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.ruoyi.vqms.source.SourceRows.HisCurveSvRow;
import com.ruoyi.vqms.source.SourceRows.WarnInfoRow;
import com.ruoyi.vqms.source.SourceRows.YcHistoryRow;
import com.ruoyi.vqms.statistics.SaveTimeParser;

/**
 * MySQL 5.7 外部源实现（qheatavchisdb）。
 *
 * 闸门：
 *  - 时间范围按"原文分钟"过滤（varchar 列无法走索引排序，量级可控全扫后内存过滤；
 *    his_curve_sv/warn_info 无主键无索引是外部库现状）
 *  - 时间非法行丢弃；0 值坏点拦截按调用方传入开关（核实单 §4 界面可整定，默认拦截——发现④）
 *  - 截断防护：结果行数达到 maxRows 上限即 ERROR 告警（静默截断会致合格率虚高，数据公平性底线）
 */
@Repository
public class Mysql57SourceReader implements SourceReader {

    private static final Logger log = LoggerFactory.getLogger(Mysql57SourceReader.class);
    private static final DateTimeFormatter MIN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    /** 与 SourceDataSourceConfig#setMaxRows 一致；达到即视为可能被截断。 */
    private static final int MAX_ROWS = 1_000_000;

    private final JdbcTemplate jdbc;

    public Mysql57SourceReader(@Qualifier("sourceJdbcTemplate") JdbcTemplate sourceJdbcTemplate) {
        this.jdbc = sourceJdbcTemplate;
    }

    /** 长区间回补防静默截断：达到 maxRows 上限时结果不完整，必须告警（宁可失败不可虚高）。 */
    private static void assertNotTruncated(int rawSize, String what, LocalDateTime start, LocalDateTime end) {
        if (rawSize >= MAX_ROWS) {
            log.error("源库查询结果达到 maxRows={} 上限（{} {}~{}）：结果大概率被截断，"
                    + "请缩小重算区间分批执行——静默截断会导致合格率虚高", MAX_ROWS, what, start, end);
        }
    }

    @Override
    public List<WarnInfoRow> fetchCommands(LocalDateTime start, LocalDateTime end) {
        String lo = start.format(MIN) + ":00";
        String hi = end.format(MIN) + ":59";
        List<WarnInfoRow> raw = jdbc.query(
                "select warn_time, millisecond, warn_type, obj_num, warn_info from warn_info "
                        + "where warn_type = 5 and warn_time >= ? and warn_time <= ?",
                (rs, i) -> new WarnInfoRow(rs.getString(1), rs.getString(2), rs.getLong(3),
                        getLong(rs, 4), rs.getString(5)),
                lo, hi);
        assertNotTruncated(raw.size(), "warn_info", start, end);
        List<WarnInfoRow> out = new ArrayList<>(raw.size());
        for (WarnInfoRow r : raw) {
            LocalDateTime t = SaveTimeParser.parseToMinute(r.warnTimeRaw());
            if (t == null || t.isBefore(start) || t.isAfter(end)) {
                continue; // 时间非法或取整后出界（如 :57 写入取整回界内则保留）
            }
            out.add(r);
        }
        return out;
    }

    @Override
    public List<HisCurveSvRow> fetchCurve(Collection<Long> busbarNums, LocalDateTime start, LocalDateTime end,
                                          boolean zeroBadpointBlock) {
        if (busbarNums == null || busbarNums.isEmpty()) {
            return List.of();
        }
        String lo = start.format(MIN) + ":00";
        String hi = end.format(MIN) + ":59";
        String inList = joinNums(busbarNums);
        // 取整边界放宽 2 分钟，容纳 jitter 写入"前一分 57/58 秒"形态
        List<HisCurveSvRow> raw = jdbc.query(
                "select save_time, busbar_num, high_SV, low_SV, average_SV, plan_SV from his_curve_sv "
                        + "where busbar_num in (" + inList + ") and save_time >= ? and save_time <= ?",
                (rs, i) -> new HisCurveSvRow(rs.getString(1), rs.getLong(2), rs.getBigDecimal(3),
                        rs.getBigDecimal(4), rs.getBigDecimal(5), rs.getBigDecimal(6)),
                shiftMinute(lo, -2), shiftMinute(hi, 2));
        assertNotTruncated(raw.size(), "his_curve_sv", start, end);
        List<HisCurveSvRow> out = new ArrayList<>(raw.size());
        Set<String> seen = new HashSet<>(raw.size() * 2);
        for (HisCurveSvRow r : raw) {
            LocalDateTime t = SaveTimeParser.parseToMinute(r.saveTimeRaw());
            if (t == null || t.isBefore(start) || t.isAfter(end)) {
                continue;
            }
            if (zeroBadpointBlock && (isZero(r.highSv()) || isZero(r.lowSv()) || isZero(r.averageSv()))) {
                continue; // 0.0 脏值拦截（防包络被毒化）
            }
            if (!seen.add(r.saveTimeRaw().trim() + "|" + r.busbarNum())) {
                continue; // 去重
            }
            out.add(r);
        }
        return out;
    }

    @Override
    public List<YcHistoryRow> fetchYc(Collection<Long> ycNums, LocalDateTime start, LocalDateTime end) {
        if (ycNums == null || ycNums.isEmpty()) {
            return List.of();
        }
        String lo = start.format(MIN) + ":00";
        String hi = end.format(MIN) + ":59";
        List<YcHistoryRow> raw = jdbc.query(
                "select yc_num, yc_time, yc_data from yc_history "
                        + "where yc_num in (" + joinNums(ycNums) + ") and yc_time >= ? and yc_time <= ?",
                (rs, i) -> new YcHistoryRow(rs.getLong(1), rs.getString(2), rs.getDouble(3)),
                lo, hi);
        assertNotTruncated(raw.size(), "yc_history", start, end);
        List<YcHistoryRow> out = new ArrayList<>(raw.size());
        Set<String> seen = new HashSet<>();
        for (YcHistoryRow r : raw) {
            LocalDateTime t = SaveTimeParser.parseToMinute(r.ycTimeRaw());
            if (t == null || t.isBefore(start) || t.isAfter(end)) {
                continue;
            }
            if (!seen.add(r.ycNum() + "|" + r.ycTimeRaw().trim())) {
                continue;
            }
            out.add(r);
        }
        return out;
    }

    private static Long getLong(ResultSet rs, int col) throws SQLException {
        long v = rs.getLong(col);
        return rs.wasNull() ? null : v;
    }

    private static boolean isZero(java.math.BigDecimal v) {
        return v == null || v.signum() == 0;
    }

    private static String joinNums(Collection<Long> nums) {
        StringBuilder sb = new StringBuilder();
        for (Long n : nums) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(n);
        }
        return sb.toString();
    }

    private static String shiftMinute(String dateTimeStr, int delta) {
        LocalDateTime t = LocalDateTime.parse(dateTimeStr,
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).plusMinutes(delta);
        return t.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}

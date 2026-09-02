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
 *  - his_curve_sv：0.0 脏值拦截 + (save_time,busbar_num) 去重
 *  - 时间非法行丢弃（脏值计数由调用方按 rows 差值记账）
 */
@Repository
public class Mysql57SourceReader implements SourceReader {

    private static final DateTimeFormatter MIN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final JdbcTemplate jdbc;

    public Mysql57SourceReader(@Qualifier("sourceJdbcTemplate") JdbcTemplate sourceJdbcTemplate) {
        this.jdbc = sourceJdbcTemplate;
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
    public List<HisCurveSvRow> fetchCurve(Collection<Long> busbarNums, LocalDateTime start, LocalDateTime end) {
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
        List<HisCurveSvRow> out = new ArrayList<>(raw.size());
        Set<String> seen = new HashSet<>(raw.size() * 2);
        for (HisCurveSvRow r : raw) {
            LocalDateTime t = SaveTimeParser.parseToMinute(r.saveTimeRaw());
            if (t == null || t.isBefore(start) || t.isAfter(end)) {
                continue;
            }
            if (isZero(r.highSv()) || isZero(r.lowSv()) || isZero(r.averageSv())) {
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

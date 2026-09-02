package com.ruoyi.vqms.source;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import com.ruoyi.vqms.source.SourceRows.HisCurveSvRow;
import com.ruoyi.vqms.source.SourceRows.WarnInfoRow;
import com.ruoyi.vqms.source.SourceRows.YcHistoryRow;

/**
 * 外部源只读 Reader 接口（实现对可配置切换；判定层只依赖本接口，不触达 JDBC）。
 *
 * 闸门口径（实现方必须遵守）：
 *  - 时间按 SaveTimeParser 解析对齐（秒≥30 进位）；无法解析的行丢弃并计入脏值
 *  - his_curve_sv：0.0 脏值拦截；(save_time, busbar_num) 去重；仅返回已登记母线
 *  - plan_SV 携带返回但判定侧不得使用（废值口径）
 */
public interface SourceReader {

    /** 抓取遥调指令（warn_type=5），按时间范围（含端点，对齐到分钟）。 */
    List<WarnInfoRow> fetchCommands(LocalDateTime start, LocalDateTime end);

    /** 抓取母线电压曲线（指定母线集合，双写去重后）。 */
    List<HisCurveSvRow> fetchCurve(Collection<Long> busbarNums, LocalDateTime start, LocalDateTime end);

    /** 抓取遥测历史（指定点号集合）。 */
    List<YcHistoryRow> fetchYc(Collection<Long> ycNums, LocalDateTime start, LocalDateTime end);
}

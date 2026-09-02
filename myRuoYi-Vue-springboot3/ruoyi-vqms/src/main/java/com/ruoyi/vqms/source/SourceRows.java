package com.ruoyi.vqms.source;

/**
 * 外部源行记录（只读形态，与 qheatavchisdb 列一一对应）。
 */
public final class SourceRows {

    private SourceRows() {
    }

    /** warn_info 一行（warn_type=5 指令）。 */
    public record WarnInfoRow(String warnTimeRaw, String millisecond, long warnType, Long objNum, String warnInfo) {
    }

    /** his_curve_sv 一行。high/low/average/plan 为 kV。 */
    public record HisCurveSvRow(String saveTimeRaw, long busbarNum,
                                java.math.BigDecimal highSv, java.math.BigDecimal lowSv,
                                java.math.BigDecimal averageSv, java.math.BigDecimal planSv) {
    }

    /** yc_history 一行。 */
    public record YcHistoryRow(long ycNum, String ycTimeRaw, double ycData) {
    }
}

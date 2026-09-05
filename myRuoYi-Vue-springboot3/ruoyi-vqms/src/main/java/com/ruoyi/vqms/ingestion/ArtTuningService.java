package com.ruoyi.vqms.ingestion;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vqms.domain.VqmsArt26Curve;
import com.ruoyi.vqms.mapper.VqmsArt26CurveMapper;
import com.ruoyi.vqms.mapper.VqmsBusbarGroupMapper;
import com.ruoyi.vqms.mapper.VqmsReactiveDeviceMapper;

/**
 * 对端 AVC 配置库整定（编排层，界面版）：上传 QHeatAvcRtdb.db（SQLite 只读）→ 与现台账比对 →
 * diff 预览（不落库）→ 人工确认后执行（幂等 UPDATE）。
 *
 * 整定范围与 CLI 工具（tools/avc-tuning）一致：
 *  - AVC_INFO.AVCStatusYxNum → point_map 'avc_onoff'
 *  - BUSBAR_GROUP.MainBarYcNum → busbar_group(group 0)
 *  - BUSBAR.realVYcNum → busbar(0/1)
 *  - GENERATOR.p/qYcNum → reactive_device(GEN_01/02)
 *  - 容量/Q 额定差异只警示不落库（拍板④监管确认）
 * 并网编码/退出原因/免考旗无配置库源，不整定。
 */
@Service
public class ArtTuningService {

    private static final Logger log = LoggerFactory.getLogger(ArtTuningService.class);

    @Autowired
    private VqmsPointConfig pointConfig;

    @Autowired
    private com.ruoyi.vqms.mapper.VqmsBusbarMapper busbarMapper;

    @Autowired
    private VqmsBusbarGroupMapper busbarGroupMapper;

    @Autowired
    private VqmsReactiveDeviceMapper reactiveDeviceMapper;

    @Autowired
    private com.ruoyi.vqms.mapper.VqmsEntityMapper entityMapper;

    private BigDecimal curCapacityKw() {
        var list = entityMapper.selectVqmsEntityList(new com.ruoyi.vqms.domain.VqmsEntity());
        return list == null || list.isEmpty() ? null : list.get(0).getRatedCapacityKw();
    }

    /** diff 行：what/where 标识 + 现值/配置库值 + sql（可执行项；警示项 null）。 */
    public record DiffRow(String section, String what, String current, String config, boolean executable, String sql, String note) {
    }

    public record Preview(int changes, int warnings, List<DiffRow> rows, String dbInfo) {
    }

    /** 解析上传的配置库并生成 diff（不落库）。 */
    public Preview preview(Path dbFile) {
        log.info("整定预览: 文件 {} 大小 {} 字节", dbFile, dbFile.toFile().length());
        List<DiffRow> rows = new ArrayList<>();
        int changes = 0;
        int warnings = 0;
        // sqlite-jdbc 的 URI 参数（mode=ro）必须带 file: 前缀，否则参数被并入文件名建出空库
        try (Connection cn = DriverManager.getConnection("jdbc:sqlite:file:" + dbFile.toAbsolutePath() + "?mode=ro")) {
            Map<String, Object> avc = firstRow(cn, "select AVCStatusYxNum from AVC_INFO");
            Map<String, Object> grp = firstRow(cn, "select MainBarYcNum from BUSBAR_GROUP");
            List<Map<String, Object>> bars = allRows(cn, "select busbarNum, busbarName, realVYcNum, TargetMAX, TargetMIN, vUpUpLimit, vDownDownLimit from BUSBAR order by busbarNum");
            List<Map<String, Object>> gens = allRows(cn, "select generatorNum, ratingPPower, maxQPower, minQPower, pYcNum, qYcNum from GENERATOR order by generatorNum");

            // 1) AVC 投退点
            Map<String, Long> pts = pointConfig.loadGatePoints();
            Long curOnoff = pts.get(VqmsPointConfig.AVC_ONOFF);
            Object cfgOnoff = avc.get("AVCStatusYxNum");
            if (cfgOnoff != null && !String.valueOf(cfgOnoff).equals(String.valueOf(curOnoff))) {
                changes++;
                // 主键占号让位：目标号若被无语义键的资料行占用（如 1001 现场候选考据行），先删资料行再换号
                String cleanup = occupiedByReferenceRow(cfgOnoff)
                        ? "delete from vqms_yc_point_map where point_num = " + cfgOnoff + " and point_key is null; " : "";
                rows.add(new DiffRow("点号", "AVC投退 avc_onoff", str(curOnoff), str(cfgOnoff), true,
                        cleanup + "update vqms_yc_point_map set point_num = " + cfgOnoff + ", point_kind='X', point_type='yx' where point_key = 'avc_onoff' and point_num = " + curOnoff + ";",
                        "AVC_INFO.AVCStatusYxNum" + (cleanup.isEmpty() ? "" : "（含占号资料行让位）")));
            }

            // 2) 主母线号指示点
            var groups = busbarGroupMapper.selectVqmsBusbarGroupList(new com.ruoyi.vqms.domain.VqmsBusbarGroup());
            Long curMain = null;
            if (groups != null) {
                for (var g : groups) {
                    if (g.getGroupNum() != null && g.getGroupNum() == 0L) {
                        curMain = g.getMainIndicatorYcNum();
                    }
                }
            }
            Object cfgMain = grp.get("MainBarYcNum");
            if (cfgMain != null && !String.valueOf(cfgMain).equals(String.valueOf(curMain))) {
                changes++;
                rows.add(new DiffRow("点号", "主母线号指示（group 0）", str(curMain), str(cfgMain), true,
                        "update vqms_busbar_group set main_indicator_yc_num = " + cfgMain + " where group_num = 0 and main_indicator_yc_num = " + curMain + ";",
                        "BUSBAR_GROUP.MainBarYcNum"));
            }

            // 3) 母线实时电压点
            var busBars = busbarMapper.selectVqmsBusbarList(new com.ruoyi.vqms.domain.VqmsBusbar());
            for (Map<String, Object> bar : bars) {
                long bn = ((Number) bar.get("busbarNum")).longValue();
                Object cfgReal = bar.get("realVYcNum");
                Long curReal = null;
                if (busBars != null) {
                    for (var b : busBars) {
                        if (b.getBusbarNum() != null && b.getBusbarNum() == bn) {
                            curReal = b.getRealtimeYcNum();
                        }
                    }
                }
                if (cfgReal != null && !String.valueOf(cfgReal).equals(String.valueOf(curReal))) {
                    changes++;
                    rows.add(new DiffRow("点号", "母线 " + bn + " 实时电压", str(curReal), str(cfgReal), true,
                            "update vqms_busbar set realtime_yc_num = " + cfgReal + " where busbar_num = " + bn + " and realtime_yc_num = " + curReal + ";",
                            "BUSBAR.realVYcNum"));
                }
            }

            // 4) 设备 P/Q 点 + 额定校核
            var devices = reactiveDeviceMapper.selectVqmsReactiveDeviceList(new com.ruoyi.vqms.domain.VqmsReactiveDevice());
            int gi = 0;
            for (Map<String, Object> gen : gens) {
                gi++;
                String code = String.format("GEN_%02d", gi);
                var dev = findDevice(devices, code);
                Object cfgP = gen.get("pYcNum");
                Object cfgQ = gen.get("qYcNum");
                if (dev != null) {
                    if (cfgP != null && !String.valueOf(cfgP).equals(String.valueOf(dev.getpYcNum()))) {
                        changes++;
                        rows.add(new DiffRow("点号", code + " P 点", str(dev.getpYcNum()), str(cfgP), true,
                                "update vqms_reactive_device set p_yc_num = " + cfgP + " where device_code = '" + code + "' and p_yc_num = " + dev.getpYcNum() + ";",
                                "GENERATOR.pYcNum"));
                    }
                    if (cfgQ != null && !String.valueOf(cfgQ).equals(String.valueOf(dev.getqYcNum()))) {
                        changes++;
                        rows.add(new DiffRow("点号", code + " Q 点", str(dev.getqYcNum()), str(cfgQ), true,
                                "update vqms_reactive_device set q_yc_num = " + cfgQ + " where device_code = '" + code + "' and q_yc_num = " + dev.getqYcNum() + ";",
                                "GENERATOR.qYcNum"));
                    }
                    changes += tuneRating(rows, code, "Q 上限", dev.getRatedQUpKvar(), gen.get("maxQPower"), "rated_q_up_kvar", dev.getDeviceId());
                    changes += tuneRating(rows, code, "Q 下限", dev.getRatedQDownKvar(), gen.get("minQPower"), "rated_q_down_kvar", dev.getDeviceId());
                } else {
                    warnings++;
                    rows.add(new DiffRow("台账", code, "无台账行", str(gen.get("pYcNum")) + "/" + str(gen.get("qYcNum")),
                            false, null, "⚠️ 台账缺机组，需人工补录 vqms_reactive_device"));
                }
            }

            // 5) 容量校核（不落库）
            BigDecimal capCfg = BigDecimal.ZERO;
            for (Map<String, Object> gen : gens) {
                Object p = gen.get("ratingPPower");
                if (p instanceof Number n) {
                    capCfg = capCfg.add(BigDecimal.valueOf(n.doubleValue()));
                }
            }
            // 5) 容量（Leo 2026-09-05 拍板纳入整定范围；结算口径仍以监管确认为准，整定后如监管口径不同可在并网主体页再改）
            BigDecimal capCur = curCapacityKw();
            if (capCur == null || capCfg.stripTrailingZeros().compareTo(capCur.stripTrailingZeros()) != 0) {
                changes++;
                rows.add(new DiffRow("校核", "主体容量 kW（Σ ratingPPower）", str(capCur), capCfg.stripTrailingZeros().toPlainString(),
                        true,
                        "update vqms_entity set rated_capacity_kw = " + capCfg.stripTrailingZeros().toPlainString()
                                + " where entity_id = 1 and rated_capacity_kw = " + (capCur == null ? "null" : capCur.toPlainString()) + ";",
                        "GENERATOR.ratingPPower 合计；结算口径如有不同以监管确认为准（核实单 §1）"));
            }

            StringBuilder info = new StringBuilder("BUSBAR ").append(bars.size()).append(" 行 / GENERATOR ").append(gens.size()).append(" 行");
            return new Preview(changes, warnings, rows, info.toString());
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("配置库解析失败", e);
            throw new ServiceException("配置库解析失败: " + e.getMessage());
        }
    }

    /** Q 额定差异整定（Leo 2026-09-05 拍板纳入范围）：改台账静态额定；P-Q 曲线（vqms_device_pq_limit）按需另行换版。 */
    private static int tuneRating(List<DiffRow> rows, String code, String what, BigDecimal cur, Object cfg,
                                  String column, Long deviceId) {
        if (cfg instanceof Number n) {
            BigDecimal c = BigDecimal.valueOf(n.doubleValue());
            if (cur == null || cur.compareTo(c) != 0) {
                rows.add(new DiffRow("校核", code + " " + what, str(cur), str(c), true,
                        "update vqms_reactive_device set " + column + " = " + c.toPlainString()
                                + " where device_id = " + deviceId + " and " + column + " = " + cur + ";",
                        "GENERATOR." + (column.endsWith("up_kvar") ? "maxQPower" : "minQPower")
                                + "；P-Q 曲线如需同步换版走设备P-Q极曲线页"));
                return 1;
            }
        }
        return 0;
    }

    /** 目标点号是否被无语义键的资料行占用（point_map 主键冲突源）。 */
    private boolean occupiedByReferenceRow(Object pointNum) {
        try {
            Long target = Long.parseLong(String.valueOf(pointNum));
            var all = pointConfig.loadGatePoints(); // 语义行不含冲突；查全表需 mapper
            return occupiedViaMapper(target);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Autowired
    private com.ruoyi.vqms.mapper.VqmsYcPointMapMapper pointMapMapper;

    private boolean occupiedViaMapper(long target) {
        var q = new com.ruoyi.vqms.domain.VqmsYcPointMap();
        q.setPointNum(target);
        var rows = pointMapMapper.selectVqmsYcPointMapList(q);
        if (rows == null) {
            return false;
        }
        for (var r : rows) {
            if (r.getPointKey() == null || r.getPointKey().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static com.ruoyi.vqms.domain.VqmsReactiveDevice findDevice(
            List<com.ruoyi.vqms.domain.VqmsReactiveDevice> devices, String code) {
        if (devices == null) {
            return null;
        }
        for (var d : devices) {
            if (code.equals(d.getDeviceCode())) {
                return d;
            }
        }
        return null;
    }

    private static String str(Object v) {
        return v == null ? "null" : String.valueOf(v);
    }

    private static Map<String, Object> firstRow(Connection cn, String sql) throws Exception {
        List<Map<String, Object>> rows = allRows(cn, sql);
        if (rows.isEmpty()) {
            throw new ServiceException("配置库空表: " + sql);
        }
        return rows.get(0);
    }

    private static List<Map<String, Object>> allRows(Connection cn, String sql) throws Exception {
        List<Map<String, Object>> out = new ArrayList<>();
        try (ResultSet rs = cn.createStatement().executeQuery(sql)) {
            int cols = rs.getMetaData().getColumnCount();
            while (rs.next()) {
                Map<String, Object> m = new HashMap<>();
                for (int i = 1; i <= cols; i++) {
                    m.put(rs.getMetaData().getColumnLabel(i), rs.getObject(i));
                }
                out.add(m);
            }
        }
        return out;
    }
}

package com.ruoyi.vqms.ingestion;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vqms.domain.VqmsEntity;
import com.ruoyi.vqms.domain.VqmsYcPointMap;
import com.ruoyi.vqms.mapper.VqmsEntityMapper;
import com.ruoyi.vqms.mapper.VqmsYcPointMapMapper;

/**
 * 点号注册表装配（现场接线配置化）：管线一律按语义键消费 vqms_yc_point_map，
 * 现场换号只改库（UPDATE point_num / gate_enabled），不改代码不发版。
 *
 * 语义键清单（gate_enabled=1 且 status=0 才生效）：
 *  - grid_signal_main / grid_signal_aux  并网编码 正/副母（投运率）
 *  - avc_onoff                           AVC 投退（投运率）
 *  - exit_reason_main / exit_reason_aux  AVC 退出原因 正/副母（投运率）
 *  - exempt_flag                         免考旗 yx（调节免考 AUTO_YX；现场无源可停用）
 * 实时电压/主母线号/设备 P-Q 走各自台账（vqms_busbar / vqms_busbar_group / vqms_reactive_device）。
 */
@Service
public class VqmsPointConfig {

    private static final Logger log = LoggerFactory.getLogger(VqmsPointConfig.class);

    public static final String GRID_SIGNAL_MAIN = "grid_signal_main";
    public static final String GRID_SIGNAL_AUX = "grid_signal_aux";
    public static final String AVC_ONOFF = "avc_onoff";
    public static final String EXIT_REASON_MAIN = "exit_reason_main";
    public static final String EXIT_REASON_AUX = "exit_reason_aux";
    public static final String EXEMPT_FLAG = "exempt_flag";

    @Autowired
    private VqmsYcPointMapMapper pointMapMapper;

    @Autowired
    private VqmsEntityMapper entityMapper;

    /** 加载全部启用语义点号：point_key → point_num（仅 gate_enabled=1、status=0）。 */
    public Map<String, Long> loadGatePoints() {
        VqmsYcPointMap q = new VqmsYcPointMap();
        q.setGateEnabled(1);
        q.setStatus("0");
        List<VqmsYcPointMap> rows = pointMapMapper.selectVqmsYcPointMapList(q);
        Map<String, Long> m = new HashMap<>();
        if (rows != null) {
            for (VqmsYcPointMap r : rows) {
                if (r.getPointKey() != null && r.getPointNum() != null) {
                    m.put(r.getPointKey(), r.getPointNum());
                }
            }
        }
        return m;
    }

    /** 必需点号：缺失即配置未整定，fail-fast（列出键名引导现场整定）。 */
    public long require(Map<String, Long> pts, String key) {
        Long v = pts.get(key);
        if (v == null) {
            throw new ServiceException("点号注册表缺配置: point_key='" + key
                    + "'（vqms_yc_point_map 现场整定：置 gate_enabled=1 并核对 point_num）");
        }
        return v;
    }

    /** 可降级点号：缺失返回 null，调用方按缺源降级（如免考旗无源 → AUTO_YX 链停用）。 */
    public Long optional(Map<String, Long> pts, String key, String degradeHint) {
        Long v = pts.get(key);
        if (v == null) {
            log.warn("点号未整定 point_key='{}' → {}（vqms_yc_point_map 现场核对后置 gate_enabled=1）", key, degradeHint);
        }
        return v;
    }

    /** 考核主体：vqms_entity 取唯一有效行（单主体阶段；多主体扩展时改按台账路由）。 */
    public long resolveEntityId() {
        VqmsEntity q = new VqmsEntity();
        q.setStatus("0");
        List<VqmsEntity> list = entityMapper.selectVqmsEntityList(q);
        if (list == null || list.isEmpty()) {
            throw new ServiceException("考核主体未配置: vqms_entity 无 status=0 行");
        }
        return list.get(0).getEntityId();
    }
}

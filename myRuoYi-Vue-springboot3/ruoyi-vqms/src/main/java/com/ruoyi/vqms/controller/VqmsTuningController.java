package com.ruoyi.vqms.controller;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vqms.ingestion.ArtTuningService;
import com.ruoyi.vqms.ingestion.ArtTuningService.DiffRow;
import com.ruoyi.vqms.ingestion.ArtTuningService.Preview;
import com.ruoyi.vqms.source.SourceReader;

/**
 * 对端 AVC 配置库整定 Controller（界面版）：
 *  POST /vqms/tuning/preview  上传 QHeatAvcRtdb.db → diff 预览（不落库）
 *  POST /vqms/tuning/apply    确认执行 diff 中的可执行 SQL（幂等 UPDATE；逐条 JDBC，容量/Q 额定警示项不执行）
 * 权限 vqms:tuning:*；@Log 审计。
 */
@RestController
@RequestMapping("/vqms/tuning")
public class VqmsTuningController extends BaseController {

    @Autowired
    private ArtTuningService tuningService;

    @Autowired
    private javax.sql.DataSource dataSource;

    @PreAuthorize("@ss.hasPermi('vqms:tuning:run')")
    @Log(title = "配置库整定-预览", businessType = BusinessType.OTHER)
    @PostMapping("/preview")
    public AjaxResult preview(@RequestParam("file") MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new ServiceException("请上传配置库文件（QHeatAvcRtdb.db）");
        }
        Path tmp = Files.createTempFile("vqms_rtdb_", ".db");
        try {
            // transferTo 对已存在的临时文件有截断坑（留下空库），用流复制覆盖
            Files.copy(file.getInputStream(), tmp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            Preview p = tuningService.preview(tmp);
            return success(p);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    /** 确认执行：body = preview 返回的可执行 SQL 列表（逐条幂等 UPDATE，单条失败即中止并报错）。 */
    @PreAuthorize("@ss.hasPermi('vqms:tuning:run')")
    @Log(title = "配置库整定-执行", businessType = BusinessType.UPDATE)
    @PostMapping("/apply")
    public AjaxResult apply(@RequestBody List<String> sqls) throws Exception {
        if (sqls == null || sqls.isEmpty()) {
            throw new ServiceException("无可执行语句");
        }
        int done = 0;
        try (var cn = dataSource.getConnection(); var st = cn.createStatement()) {
            cn.setAutoCommit(false);
            for (String sql : sqls) {
                if (!sql.trim().toLowerCase().startsWith("update vqms_")) {
                    cn.rollback();
                    throw new ServiceException("拒绝非 vqms_ 表语句: " + sql);
                }
                done += st.executeUpdate(sql);
            }
            cn.commit();
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("执行失败（已回滚）: " + e.getMessage());
        }
        return success("整定完成，生效 " + done + " 行——请重算判定验证等价");
    }
}

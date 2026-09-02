# VQMS2.0 项目说明 / Project Guide

## 项目概述 / Overview

VQMS（Voltage Quality Management System）= 基于 **RuoYi-Vue**（前后端分离版，Spring Security + JWT + Redis）的 AVC 母线电压质量监测与考核系统。

VQMS (Voltage Quality Management System) is an AVC bus-voltage quality monitoring and assessment system built on **RuoYi-Vue** (front/back-end separated edition, Spring Security + JWT + Redis).

- 代码库 / Codebase：`myRuoYi-Vue-springboot3/`（若依 Spring Boot 3 版，标准 ruoyi-admin/common/framework/system/quartz/generator/ui 七模块 / standard 7-module RuoYi layout）
- 业务 / Business domain：对接东北能源监管局"两个细则"（2024-09-04 印发，2024-10-01 施行），重点 AVC/电压/无功考核分析 / Implements the Northeast China "Two Detailed Rules" (issued 2024-09-04, effective 2024-10-01), focused on AVC / voltage / reactive-power assessment
- 政策原文与提炼口径 / Policy sources：`docs/政策口径/`（`_extract_2024.txt` 为 2024 版全文提取，可 grep 检索，含页码标记 / full-text extraction of the 2024 edition with page markers, grep-able）

## 单位规约（全项目强制）/ Unit Conventions (mandatory project-wide)

| 量 / Quantity | 规范单位 / Unit | 存储 / Storage |
|---|---|---|
| 电压 / Voltage | **kV** | decimal |
| 无功功率 / Reactive power | **kvar** | decimal |
| 有功 / 容量 / Active power / Capacity | **kW** | decimal |
| 合格率 / 投运率 / Qualification & availability rates | **%** | decimal |
| 时间（聚合）/ Time (aggregated) | **分钟数 / minutes** | int |

**时区 / Timezone**：所有时间一律北京时间（UTC+8 / `Asia/Shanghai`），全项目统一 / All timestamps are Beijing time (UTC+8 / `Asia/Shanghai`) everywhere in the project.

**换算注意 / Conversion note**：政策原文用"万千瓦/万千乏"，与规范单位差 10⁴（如 ±1 万千乏 = 10000 kvar；1.5 千伏 = 1.5 kV）/ The policy text uses 万千瓦/万千乏 (10⁴ kW / 10⁴ kvar); convert by a factor of 10⁴ against the canonical units (e.g. ±1 万千乏 = 10000 kvar).

## 数据库规约 / Database Conventions

- **新增数据表一律以 `vqms_` 前缀命名** / All newly created tables MUST be prefixed with `vqms_`（与若依自带 sys_/gen_/qrtz_ 等系统表区分 / to distinguish from RuoYi's built-in sys_/gen_/qrtz_ tables）

## 政策依据核心口径 / Policy Key Points（附件6 AVC，2024 版 p47-49 / Annex 6 AVC, 2024 ed.）

- **投运率 / Availability rate** = AVC 投运时间 / 并网运行时间 ×100%（扣除电网原因退出时间 / grid-caused outage time deducted from the denominator）；合格线 **99%**；缺额每百分点 `额定容量 × 0.02 分/万千瓦` (0.02 points per MW-nameplate per missing percentage point)
- **调节合格率 / Regulation qualification rate** = 执行合格点数 / 发令次数 ×100%；合格判据：主站电压/无功指令下达后 **1 分钟内**调整到合格区间 / must settle into the qualified band within **1 minute** after the dispatch command；合格线 **100%**；缺额同上 `0.02 分/万千瓦`
- **合格区间 / Qualified band**：500kV ±1.5kV；220kV ±1kV 或 / or 机组无功偏差 unit reactive deviation ±1 万kvar（风光储 renewables/storage ±0.5 万kvar）；66kV 及以下 / and below ±1% 额定电压 rated voltage 或 / or ±0.4 万kvar
- **结算 / Settlement**：1 分 = 1000 元 (1 point = 1000 CNY)；同一事件多条款取最大 / single event charged once under the largest applicable clause；月度结零按分类上网电量返还 / monthly balance cleared, refunded pro-rata by energy category

### 免考判定 / Exemption Rules（两条独立路径，不能混用 / two independent paths, never merged）

1. **投运率免责 / Availability exemption**：AVC 退出时段的退出原因 ∈ {电网原因 / grid-caused} → 从分母扣除时间 / deduct from denominator（数据 / data：AVC 投退 yx 信号 + 退出原因标注 status signal + outage-cause tagging）
2. **调节合格率免责 / Regulation exemption**（附件6 §三 / Annex 6 §3）：全部 AVC 闭环无功设备在**正确方向**（电压偏低→发出 / low voltage → inject；偏高→吸收 / high voltage → absorb）顶到**各自极限**仍不达标 → 该时段免考 / all closed-loop reactive devices at their respective limits in the correct direction yet still out of band → that period is exempt（数据 / data：设备级无功遥测 Q vs 双向极限配置 / per-device reactive telemetry vs bidirectional limit config；发电机类极限随 P 变的 P-Q 曲线 / generator limits are P-dependent P-Q curves、逆变器型 inverter-type ±√(S²−P²)、电容/电抗单向 / cap & reactor unidirectional）

**三状态模型 / Three-state model**：每个发令点记 / each command point is recorded as 执行合格 qualified / 不合格-非免考 unqualified-nonexempt / 不合格-免考 unqualified-exempt；**剔除法**记账 / **exclusion bookkeeping**（免考点同时移出分子分母、不进罚 / exempt points removed from both numerator and denominator, no penalty）。

### VQMS 细化分档 / VQMS Tiering（附件6 无此规定，5 分钟阈值外部依据待确认 / not in Annex 6; external basis for the 5-min threshold pending）

不合格（>1 分钟）按响应时长分档 / Unqualified (>1 min) points are tiered by response time：**[1,5) 分钟 = 调节快速性考核 regulation-speed assessment；≥5 分钟 = 调节经济性考核 regulation-economics assessment**；阈值现场可整定 / thresholds field-adjustable。

### 相关联考核 / Related Clauses（正文条款 / main-body articles）

- 第 26 条 母线电压 / Art. 26 bus voltage：季度电压曲线为依据 / quarterly voltage curve as the basis；"AVC 主站闭环调节控制的并网主体免于考核" / entities under AVC master closed-loop control are exempt（需识别 AVC 投退状态 / requires AVC in/out-of-service state）
- 第 27 条 SVG/SVC/调相机 / Art. 27 SVG/SVC/synchronous condenser：投入自动可用率 99%，每降 1pp 月考 0.1 分/万千瓦 / auto-availability 99%, 0.1 point per MW per missing pp monthly（风光储 6 个月过渡期 / 6-month grace period for renewables & storage）
- 辅助服务细则 / Ancillary-services rules：基本无功 / basic reactive = 迟相 PF≥0.85 发出 / inject at lagging PF≥0.85、进相 PF≥0.97 吸收 / absorb at leading PF≥0.97（义务 / obligatory）；有偿无功 300 元/万千乏时 / paid reactive 300 CNY per 10⁴ kvar·h

## 注意事项 / Notes

- 实现口径一律以 **2024 版**细则为准（2020 版旧文件已从仓库移除 / always implement against the 2024 edition; the 2020-edition files were removed from the repo）
- 仓库根 `.gitignore` 含 `*.zip` 规则，需入库的 zip 要 `git add -f` / the root `.gitignore` excludes `*.zip`; use `git add -f` for zips that must be tracked
- `myRuoYi-Vue-springboot3` 的 7 个 ruoyi-* 模块目录设了 Windows 只读属性（attrib +R）；构建前需解除 / the 7 ruoyi-* module directories carry the Windows read-only attribute; clear it before building：
  ```bash
  cd /c/work/VQMS2.0/myRuoYi-Vue-springboot3 && for m in ruoyi-quartz ruoyi-system ruoyi-ui ruoyi-admin ruoyi-common ruoyi-framework ruoyi-generator; do (cd "$m" && MSYS_NO_PATHCONV=1 attrib -R /S /D '*') && (cd .. && MSYS_NO_PATHCONV=1 attrib -R "$m"); done
  ```

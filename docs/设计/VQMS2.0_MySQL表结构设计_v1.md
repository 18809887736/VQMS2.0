# VQMS2.0 MySQL 表结构设计 v1

> 状态：**设计稿，待 Leo 审阅拍板**（2026-09-02）
> 输入：① 政策口径（docs/政策口径，附件6 + 三状态模型 + 分档）② 外部数据现实（docs/外部DB + 现场库核对报告 2026-08-26）③ VQMS 1.0 成熟实现（C:\work\VQMS\sql\vqms.sql v5.0 对齐版）
> 目标库：与 RuoYi sys_* 同库；MySQL 8；`utf8mb4_0900_ai_ci`；InnoDB；全部 `vqms_` 前缀

---

## 一、设计原则（继承 1.0 铁律 + 2.0 修订）

1. **存储切分铁律**：外部源（qheatavchisdb @10.0.0.9）是 raw 唯一真相源，原始曲线/遥测绝不入主库；主库只存 **管理配置 + 派生判定/统计**。唯一有界例外：`vqms_command_ledger`（指令原文只增摘录，~288 行/天）。
2. **率/罚款不落表（调节侧）**：rollup 只对计数求和，绝不平均率列；率与罚款由查询层按计数重算。投运侧保留快照列（纯函数单一来源写回，NULL=无基数语义）。
3. **幂等摄取**：可空列经生成列归一（NULL→''/-1）进唯一键，`INSERT IGNORE` 结构性拦截重复抓取，无 TOCTOU 竞态。
4. **判定/统计纯函数化**：DB 只落结果；算法版本、参数快照随行落库，保证跨整定期可复现可审计。
5. **阈值带生效区间**：变更不回溯历史，重算按当时生效带执行。
6. **逻辑 FK 不建物理外键**；comment 内嵌拍板决策记录；RuoYi 审计五件套（create_by/create_time/update_by/update_time/remark）+ status。
7. **时区**：全部 datetime 按 Asia/Shanghai 北京历法；源 varchar 时间在摄取层解析（秒≥30 进位到分钟）。
8. **单位**：kV/kvar/kW 一律 decimal；时间聚合 int 分钟；率 %（仅投运快照列）。
9. **母线编号值对齐**（Leo 2026-09-02 确认）：`vqms_busbar.busbar_num` 直接沿用 `his_curve_sv.busbar_num` 的编号值（0/1/2…），运行时按值直接对应，不建映射表；数据中出现而台账未登记的 busbar_num 由摄取闸门跳过并记 `vqms_ingest_log`。

## 二、表清单总览（16 张，Phase 1）

| # | 表 | 用途 | 相对 1.0 |
|---|---|---|---|
| 1 | vqms_entity | 并网主体台账（考核基数载体） | **新增**（1.0 改进项⑤） |
| 2 | vqms_busbar_group | 母线组（主母线判定单元） | 挂 entity_id，容量上移主体 |
| 3 | vqms_busbar | 主母线元数据 | 基本沿用 |
| 4 | vqms_busbar_threshold | 阈值带（生效区间） | 删遗留死列 plan_sv_invalid_policy |
| 5 | vqms_yc_point_map | 点号→语义注册（含对端 JS 虚拟点） | 种子只留真实候选，**剔除合成占位** |
| 6 | vqms_judge_param | 判定整定参数（CHECK 双层） | 沿用 |
| 7 | vqms_policy_param | 数据不可用处置策略 KV | 沿用（选套留空） |
| 8 | vqms_command_ledger | AVC 指令流水账（只增） | **增 cmd_time datetime 解析列** |
| 9 | vqms_regulation_cmd | 指令级判定明细 | 增解码版本/响应时长/免考溯源，修 uk NULL 旁路 |
| 10 | vqms_exempt_annotation | 调节免考人工标注 | **新增**（yx501 现场无源的必然路径） |
| 11 | vqms_exit_annotation | AVC 退出原因标注（投运率免责） | **新增**（yc521/522 未落盘前的人工路径） |
| 12 | vqms_reactive_device | 无功设备台账（免考判定单元） | **新增**（附件6 §三 设备级判定） |
| 13 | vqms_device_pq_limit | P-Q 双向极限曲线 | **新增**（随 P 变的发电机类极限） |
| 14 | vqms_regulation_stats | 调节合格率汇总（D/M/Y 合一） | **三表合一** + entity 维度 |
| 15 | vqms_runtime_stats | 投运率记账（D/M/Y 合一） | **三表合一** + entity 维度 |
| 16 | vqms_ingest_log | 摄取批次日志（数据质量闸门审计） | **新增** |

Phase 2 预留（本稿不含 DDL）：第 26 条母线电压考核（季度电压曲线表）、第 27 条 SVG/SVC 可用率考核、有偿无功补偿计算。

## 三、DDL

### 3.1 并网主体（考核主体，罚款基数载体）

```sql
create table vqms_entity (
  entity_id          bigint       not null auto_increment comment '并网主体ID',
  entity_code        varchar(64)  not null                comment '主体编号（调度口径）',
  entity_name        varchar(128) not null                comment '主体名称',
  entity_type        char(1)      not null default '1'    comment '类型（字典 vqms_entity_type）：1=火电 2=水电 3=核电 4=风电 5=光伏 6=新型储能 7=光热 8=其他',
  rated_capacity_kw  decimal(12,3) default null           comment '额定容量 kW（考核基数：缺额pp×容量/10000×0.02分；NULL=待补录，补录前不产罚款数）',
  avc_closed_loop    tinyint(1)   not null default 1      comment '是否 AVC 主站闭环控制主体（第26条电压考核豁免判定输入；现场默认闭环）',
  effective_from     date         default null            comment '并网生效日',
  effective_to       date         default null            comment '解列日；NULL=在运',
  status             char(1)      not null default '0'    comment '状态：0=正常, 1=停用',
  create_by          varchar(64)  default ''              comment '创建者',
  create_time        datetime     default current_timestamp comment '创建时间',
  update_by          varchar(64)  default ''              comment '更新者',
  update_time        datetime     default current_timestamp on update current_timestamp comment '更新时间',
  remark             varchar(255) default null            comment '备注',
  primary key (entity_id),
  unique key uk_entity_code (entity_code)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_0900_ai_ci comment='VQMS 并网主体台账（考核主体）';
-- 种子：本厂 = 2×300MW 火电（GENERATOR.ratingPPower=300000 kW×2），rated_capacity_kw=600000 待现场核实
```

### 3.2 母线组 / 主母线

```sql
create table vqms_busbar_group (
  group_num               bigint       not null                comment '母线组编号',
  entity_id               bigint       not null                comment '所属并网主体（逻辑FK → vqms_entity）',
  group_name              varchar(64)  not null                comment '组名',
  v_grade                 tinyint      not null                comment '电压等级编码，对齐字典 vqms_v_grade：0=500kV, 1=220kV, 2=66kV及以下(预留)',
  main_indicator_yc_num   bigint       default null            comment '该组"当前主母线号"指示点（对端 BUSBAR_GROUP.MainBarYcNum=3 候选）；未接入前为空',
  default_main_busbar_num bigint       default null            comment '指示点不可用兜底主母线号；NULL=不兜底→该组该分钟无主母线',
  max_staleness_minutes   int          not null default 30     comment '指示点陈旧窗口(分钟)',
  status                  char(1)      not null default '0'    comment '状态：0=正常, 1=停用',
  create_by               varchar(64)  default ''              comment '创建者',
  create_time             datetime     default current_timestamp comment '创建时间',
  update_by               varchar(64)  default ''              comment '更新者',
  update_time             datetime     default current_timestamp on update current_timestamp comment '更新时间',
  remark                  varchar(255) default null            comment '备注',
  primary key (group_num)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_0900_ai_ci comment='VQMS 母线组（主母线判定单元）';

create table vqms_busbar (
  busbar_num      bigint       not null                comment '主母线编号，对齐 his_curve_sv.busbar_num',
  busbar_name     varchar(64)  not null                comment '母线名称（现场 0/1=220kV 东/西母线，2=500kV 待拍板）',
  v_grade         tinyint      not null                comment '电压等级编码，同 vqms_v_grade',
  group_num       bigint       default null            comment '所属母线组（逻辑FK → vqms_busbar_group）',
  nominal_kv      decimal(10,3) not null               comment '标称电压 kV',
  realtime_yc_num bigint       default null            comment '该母线 t0 实时电压 yc 点（增量指令算 V_target 用；候选 yc8 东母/yc14 西母）',
  status          char(1)      not null default '0'    comment '状态：0=正常, 1=停用',
  create_by       varchar(64)  default ''              comment '创建者',
  create_time     datetime     default current_timestamp comment '创建时间',
  update_by       varchar(64)  default ''              comment '更新者',
  update_time     datetime     default current_timestamp on update current_timestamp comment '更新时间',
  remark          varchar(255) default null            comment '备注',
  primary key (busbar_num)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_0900_ai_ci comment='VQMS 主母线元数据';
```

### 3.3 阈值带（生效区间，变更不回溯）

```sql
create table vqms_busbar_threshold (
  threshold_id   bigint       not null auto_increment comment '主键',
  busbar_num     bigint       not null                comment '母线编号（逻辑FK → vqms_busbar）',
  criterion_type varchar(8)   not null default 'AVC'  comment '口径：AVC=附件6 合格区间 / GB=国标；附件6：500kV±1.5kV、220kV±1kV、66kV及以下±1%额定',
  tolerance_v    decimal(10,3) default null           comment 'AVC 容差 kV：220kV=1.000, 500kV=1.500；66kV 及以下按 ±1% 在判定层由 nominal_kv 折算，本列可空',
  effective_from date         not null                comment '生效起始日（含）',
  effective_to   date         default null            comment '生效结束日（含），NULL=至今有效',
  create_by      varchar(64)  default ''              comment '创建者',
  create_time    datetime     default current_timestamp comment '创建时间',
  update_by      varchar(64)  default ''              comment '更新者',
  update_time    datetime     default current_timestamp on update current_timestamp comment '更新时间',
  remark         varchar(255) default null            comment '备注',
  primary key (threshold_id),
  key idx_busbar_effective (busbar_num, effective_from, effective_to)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_0900_ai_ci comment='VQMS 母线电压合格阈值（带生效区间）';
-- 1.0 的 plan_sv_invalid_policy 死列已删（plan_SV 废值不读，处置在 judge 层）
```

### 3.4 点号语义注册（含对端 JS 虚拟点）

```sql
create table vqms_yc_point_map (
  point_num     bigint       not null                comment '点号（yc/yx 统一注册，对齐外部 yc_history / yx_history）',
  point_kind    char(1)      not null default 'C'    comment 'C=遥测 yc / X=遥信 yx',
  point_name    varchar(64)  not null                comment '语义名称',
  point_type    varchar(32)  default null            comment 'busbar_id=主母线号 / voltage=电压 / power=有功 / reactive=无功 / yx=开关量 / analog=编码量',
  entity_id     bigint       default null            comment '归属主体（逻辑FK）',
  busbar_num    bigint       default null            comment '关联母线（逻辑FK）',
  unit          varchar(32)  default null            comment '单位（模拟量）',
  state_1_label varchar(32)  default null            comment 'yx 值=1 语义',
  state_0_label varchar(32)  default null            comment 'yx 值=0 语义',
  gate_enabled  tinyint(1)   not null default 0      comment '是否启用为考核门控：1=启用；真实环境默认 0，现场核对后置 1',
  status        char(1)      not null default '0'    comment '状态：0=正常, 1=停用',
  create_time   datetime     default current_timestamp comment '创建时间',
  update_time   datetime     default current_timestamp on update current_timestamp comment '更新时间',
  remark        varchar(255) default null            comment '备注（含现场核对结论）',
  primary key (point_num)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_0900_ai_ci comment='VQMS 点号语义注册表（含对端 JS 引擎虚拟点）';
-- 种子只录真实候选（对端配置库考据），合成占位点号一律不种（1.0 撞号教训）：
--   yc3 主母线号 / yc8 东母电压 / yc14 西母电压 / yc216+316 机组有功 / yc217+317 机组无功
--   yc11/17 母线总无功 / yc511/512 并网母线号编码（带电×10+机数，≥10=并网）
--   yc521/522 AVC退出原因三态（0=未退出/1=电网原因免责/2=非电网扣罚；对端接口已定待落盘）
--   yx1001 AVC投退 / yx210/310 机组投退 / yx2003 远方就地 / yx3001~3009 闭锁总信号
--   yx501 免考旗：现场库不存在（核对报告发现③）——可注册但标注"无源，待对端实现"
```

### 3.5 判定整定参数（CHECK 双层：行本地值域 + 锁定行钉值）

```sql
create table vqms_judge_param (
  param_id    bigint      not null auto_increment comment '主键',
  param_key   varchar(64) not null                comment '参数键',
  param_value int         not null                comment '参数值（分钟数）',
  name        varchar(64) not null                comment '参数名称',
  description varchar(255) default null           comment '说明',
  value_min   int         default null            comment '值域下限（含）',
  value_max   int         default null            comment '值域上限（含）',
  status      char(1)     not null default '0'    comment '状态：0=正常, 1=停用',
  create_by   varchar(64) default ''              comment '创建者',
  create_time datetime    default current_timestamp comment '创建时间',
  update_by   varchar(64) default ''              comment '更新者',
  update_time datetime    default current_timestamp on update current_timestamp comment '更新时间',
  remark      varchar(255) default null           comment '备注',
  primary key (param_id),
  unique key uk_param_key (param_key),
  constraint ck_value_range check (value_min is null or value_max is null or param_value between value_min and value_max),
  constraint ck_locked_rows check (
    (param_key <> 't_econ' or (param_value <=> 5 and value_min <=> 5 and value_max <=> 5))
    and (param_key <> 'tier_threshold_fast' or (param_value <=> 1 and value_min <=> 1 and value_max <=> 1))
    and (param_key <> 'tier_threshold_econ' or (param_value <=> 5 and value_min <=> 5 and value_max <=> 5))
    and (param_key <> 't_fast' or (value_min <=> 1 and value_max <=> 4))
  )
) engine=innodb default charset=utf8mb4 collate=utf8mb4_0900_ai_ci comment='VQMS 判定整定参数';
-- 种子（1.0 Leo 2026-08-14 定，2.0 沿用）：t_fast=4 可整定[1,4]；t_econ=5 锁定（指令5分钟间隔）；
--   分档阈值 1/5 为附件6 政策值锁定。CHECK 不拦 DELETE，必需行删除由 Service 层拦。
```

### 3.6 数据不可用策略参数（选套留空）

```sql
create table vqms_policy_param (
  param_id    bigint       not null auto_increment comment '主键',
  param_key   varchar(64)  not null                comment '参数键（undecodable_mode / invalid_tier_mode / partial_missing_mode / partial_missing_threshold_pct）',
  param_value varchar(255) default null            comment '参数值（{COUNT_NORMAL, EXCLUDE_REPORTED, COUNT_UNQUALIFIED, PEND_MARKED}；选套前整表留空）',
  name        varchar(64)  not null                comment '参数名称',
  description varchar(255) default null            comment '说明',
  create_by   varchar(64)  default ''              comment '创建者',
  create_time datetime     default current_timestamp comment '创建时间',
  update_by   varchar(64)  default ''              comment '更新者',
  update_time datetime     default null            comment '更新时间',
  primary key (param_id),
  unique key uk_policy_key (param_key)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_0900_ai_ci comment='VQMS 数据不可用策略参数表（选套留空）';
```

### 3.7 AVC 指令流水账（原始事实，只增；铁律唯一例外）

```sql
create table vqms_command_ledger (
  id             bigint       not null auto_increment comment '主键',
  warn_time_raw  varchar(32)  not null                comment '指令时间原文（warn_info.warn_time 原样，忠实摘录）',
  cmd_time       datetime     not null                comment '解析后指令时刻（北京历法；源 varchar 双格式解析、秒≥30 进位到分钟由摄取层完成并保证确定性）',
  millisecond    varchar(255) default null            comment '毫秒原文（与源同宽防截断假碰撞）',
  warn_type      int          not null default 5      comment '类型；电压指令=5（本账只收指令）',
  obj_num        bigint       default null            comment '对象编号（现场恒=2 厂级；不参与逻辑 FK）',
  warn_content   varchar(255) default null            comment '指令文本原文（目标值/增量值编码在此文本内，判定层解码）',
  fetched_at     datetime     default current_timestamp comment '抓取入库时间',
  millisecond_uk varchar(255) generated always as (coalesce(millisecond, '')) stored comment 'uk 键列：NULL 归一空串（应用不读写）',
  obj_num_uk     bigint       generated always as (coalesce(obj_num, -1)) stored comment 'uk 键列：NULL 归一 -1（应用不读写）',
  primary key (id),
  unique key uk_cmd (warn_time_raw, millisecond_uk, obj_num_uk),
  key idx_cmd_time (cmd_time)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_0900_ai_ci comment='VQMS AVC 指令流水账（原始事实，只增；INSERT IGNORE 幂等）';
-- 2.0 相对 1.0：新增 cmd_time datetime 解析列（1.0 改进项⑥），查询/判定不再受制于 varchar 原文
```

### 3.8 指令级判定明细

```sql
create table vqms_regulation_cmd (
  id                 bigint       not null auto_increment comment '主键',
  stat_date          date         not null                comment '统计归属日（t0 所在日，北京历法）',
  entity_id          bigint       not null                comment '考核主体（逻辑FK → vqms_entity）',
  group_num          bigint       default null            comment '判定母线组（逻辑FK；NULL=未关联组）',
  warn_time_raw      varchar(32)  not null                comment '指令时间原文（对齐 ledger，溯源键成分）',
  millisecond        varchar(255) default null            comment '毫秒原文',
  obj_num            bigint       default null            comment '对象编号',
  cmd_time           datetime     not null                comment '解析后指令时刻 t0',
  algorithm_id       varchar(16)  not null                comment '判定算法注册 ID（V2_0/STUB/MIXED）',
  decode_algorithm   varchar(16)  not null default 'ROT10_V1' comment '解码算法版本：ROT10_V1=首位轮转码{1,2,3}+余值÷10（核对报告发现①口径；旧÷100 已证伪）',
  target_kv          decimal(10,3) default null           comment '解码后目标电压 kV；NULL=解码失败（见 undecodable_reason）',
  response_minutes   int          default null            comment '响应时长（分钟）：t0 → 电压入合格区间时刻；NULL=未入区间/窗口缺数据',
  t_fast_snapshot    int          not null                comment '判定时 t_fast 快照（可复现可审计）',
  fast_state         varchar(16)  not null                comment '快速档最终记账：QUALIFIED/PENALIZED/EXEMPTED/INVALID',
  econ_state         varchar(16)  not null                comment '经济档最终记账（两档平行互不隶属）',
  completeness       decimal(5,4) not null                comment '窗口完整度 [0,1]',
  invalid_tiers      varchar(16)  default null            comment '原始按档无效标记 FAST/ECON/FAST,ECON；NULL=无',
  undecodable_reason varchar(32)  default null            comment '解码失败归因 CYCLE_CODE_INVALID/MISSING_T0_VOLTAGE/CORRUPTED_ENCODING；NULL=成功',
  exempt_source      varchar(16)  default null            comment '免考来源：AUTO_YX(免考旗采样)/AUTO_DEVICE(设备级Q极限判定)/MANUAL(人工标注)；NULL=无免考',
  exempt_ref_id      bigint       default null            comment '免考溯源：MANUAL→vqms_exempt_annotation.id；AUTO_*→NULL（依据随行）',
  disposition        varchar(32)  default null            comment '策略处置桶 COUNT_NORMAL/EXCLUDE_REPORTED/COUNT_UNQUALIFIED/PEND_MARKED；NULL=策略未生效',
  hit_rule_id        varchar(8)   default null            comment '戊命中规则 ID（R001…）',
  fetched_at         datetime     default current_timestamp comment '写入时间',
  obj_num_uk         bigint       generated always as (coalesce(obj_num, -1)) stored comment 'uk 键列（应用不读写）',
  millisecond_uk     varchar(255) generated always as (coalesce(millisecond, '')) stored comment 'uk 键列（应用不读写）',
  primary key (id),
  unique key uk_cmd_result (warn_time_raw, millisecond_uk, obj_num_uk),
  key idx_stat_date (stat_date, entity_id)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_0900_ai_ci comment='VQMS 调节合格率指令级明细（判定+免考+策略处置）';
-- 2.0 变更：①修 1.0 uk NULL 旁路（millisecond 原列→millisecond_uk 生成列进键）②删 yx501_fast/econ 双列
--   （现场无源，核对报告发现③）——免考旗采样归入 exempt_source=AUTO_YX 的判定证据，不单列
--   ③新增 decode_algorithm/response_minutes/exempt_source/exempt_ref_id 审计列
```

### 3.9 调节免考人工标注（审计留痕，重算时应用）

```sql
create table vqms_exempt_annotation (
  annotation_id  bigint       not null auto_increment comment '主键',
  entity_id      bigint       not null                comment '考核主体（逻辑FK）',
  warn_time_raw  varchar(32)  not null                comment '指向指令（与 millisecond/obj_num 组成溯源键）',
  millisecond    varchar(255) default null            comment '毫秒原文（溯源键成分）',
  obj_num        bigint       default null            comment '对象编号（溯源键成分）',
  tier           varchar(8)   not null default 'BOTH' comment '免考档：FAST/ECON/BOTH',
  exempt_reason  varchar(255) not null                comment '免考依据（附件6§三：全部闭环无功设备正确方向顶满仍不达标 等）',
  evidence       varchar(512) default null            comment '佐证材料描述（设备Q曲线截图/调度电话记录等）',
  status         char(1)      not null default '0'    comment '状态：0=有效, 1=撤销',
  create_by      varchar(64)  default ''              comment '标注人',
  create_time    datetime     default current_timestamp comment '标注时间',
  update_by      varchar(64)  default ''              comment '更新者',
  update_time    datetime     default current_timestamp on update current_timestamp comment '更新时间',
  remark         varchar(255) default null            comment '备注',
  primary key (annotation_id),
  key idx_cmd (warn_time_raw, obj_num)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_0900_ai_ci comment='VQMS 调节免考人工标注（yx501/设备级判定无源期间的人工路径，重算时应用）';
```

### 3.10 AVC 退出原因标注（投运率免责输入）

```sql
create table vqms_exit_annotation (
  annotation_id bigint       not null auto_increment comment '主键',
  entity_id     bigint       not null                comment '考核主体（逻辑FK）',
  period_start  datetime     not null                comment '退出时段起（含，北京历法）',
  period_end    datetime     not null                comment '退出时段止（含）',
  exit_reason   varchar(16)  not null                comment 'GRID=电网原因（免责，出分母）/ NON_GRID=非电网（扣罚，在分母）/ UNKNOWN=原因不明',
  source        varchar(8)   not null default 'MANUAL' comment '来源：AUTO_YC=yc521/522 三态点自动 / MANUAL=人工标注',
  evidence      varchar(512) default null            comment '依据（闭锁信号/检修票/调度记录）',
  status        char(1)      not null default '0'    comment '状态：0=有效, 1=撤销',
  create_by     varchar(64)  default ''              comment '标注人',
  create_time   datetime     default current_timestamp comment '标注时间',
  update_by     varchar(64)  default ''              comment '更新者',
  update_time   datetime     default current_timestamp on update current_timestamp comment '更新时间',
  remark        varchar(255) default null            comment '备注',
  primary key (annotation_id),
  key idx_period (entity_id, period_start, period_end)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_0900_ai_ci comment='VQMS AVC 退出原因标注（投运率免责判定输入；yc521/522 落盘前以人工为主）';
```

### 3.11 无功设备台账 + P-Q 极限曲线（调节免考设备级判定，附件6 §三）

```sql
create table vqms_reactive_device (
  device_id        bigint       not null auto_increment comment '主键',
  entity_id        bigint       not null                comment '所属主体（逻辑FK）',
  device_code      varchar(64)  not null                comment '设备编号',
  device_name      varchar(128) not null                comment '设备名称',
  device_type      tinyint      not null                comment '1=同步发电机/调相机 2=逆变器型(风/光/储) 3=SVC/STATCOM 4=电容器组 5=电抗器（字典 vqms_device_type）',
  in_avc_loop      tinyint(1)   not null default 1      comment '是否纳入 AVC 闭环控制（免考判定只考察闭环设备）',
  rated_s_kva      decimal(12,3) default null           comment '视在容量 kVA（逆变器型：Q=±√(S²−P²) 判定用）',
  rated_q_up_kvar  decimal(12,3) default null           comment '发出上限 kvar（对称/单向设备额定；发电机类本列空、查 P-Q 曲线表）',
  rated_q_down_kvar decimal(12,3) default null          comment '吸收下限 kvar（负值；单向设备空）',
  q_yc_num         bigint       default null            comment '无功遥测点号（逻辑FK → vqms_yc_point_map）',
  p_yc_num         bigint       default null            comment '有功遥测点号（P-Q 曲线插值用）',
  status           char(1)      not null default '0'    comment '状态：0=正常, 1=停用',
  create_by        varchar(64)  default ''              comment '创建者',
  create_time      datetime     default current_timestamp comment '创建时间',
  update_by        varchar(64)  default ''              comment '更新者',
  update_time      datetime     default current_timestamp on update current_timestamp comment '更新时间',
  remark           varchar(255) default null            comment '备注',
  primary key (device_id),
  unique key uk_device_code (entity_id, device_code)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_0900_ai_ci comment='VQMS 无功设备台账（附件6§三 免考判定单元）';

create table vqms_device_pq_limit (
  id          bigint       not null auto_increment comment '主键',
  device_id   bigint       not null                comment '设备（逻辑FK → vqms_reactive_device；仅发电机类需要）',
  p_kw        decimal(12,3) not null               comment '有功工况点 kW',
  q_up_kvar   decimal(12,3) not null               comment '该 P 下发出上限 kvar',
  q_down_kvar decimal(12,3) not null               comment '该 P 下吸收下限 kvar（负值）',
  effective_from date       not null                comment '生效起始日（曲线换版不回溯）',
  effective_to date         default null            comment '生效结束日；NULL=至今',
  create_time datetime     default current_timestamp comment '创建时间',
  update_time datetime     default current_timestamp on update current_timestamp comment '更新时间',
  remark      varchar(255) default null            comment '备注',
  primary key (id),
  unique key uk_device_p (device_id, p_kw, effective_from)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_0900_ai_ci comment='VQMS 设备 P-Q 双向极限曲线（蓝本：对端 GENERATOR_P_QLimit）';
-- 免考判定（AUTO_DEVICE）：电压偏低→查各闭环设备是否顶到 q_up_kvar（或 ±√(S²−P²)）；偏高→q_down_kvar；
--   方向错或留余力均不算尽力。电容/电抗单向（rated_q 单列填），SVC/STATCOM 连续对称。
```

### 3.12 调节合格率汇总（D/M/Y 合一 + 主体维度）

```sql
create table vqms_regulation_stats (
  id                bigint       not null auto_increment comment '主键',
  stat_grain        char(1)      not null                comment '粒度：D=日 / M=月 / Y=年',
  stat_period       date         not null                comment '统计期（D=当日 / M=当月首日 / Y=当年1月1日）',
  entity_id         bigint       not null                comment '考核主体（逻辑FK）',
  algorithm_id      varchar(16)  default null            comment '周期内算法：单一=该 ID / 混合=MIXED',
  total_cmds        int          not null default 0      comment '发令总次数=分母（固定分母口径）',
  qualified_fast    int          not null default 0      comment '快速档合格数',
  penalized_fast    int          not null default 0      comment '快速档不合格-非免考（扣罚）',
  exempted_fast     int          not null default 0      comment '快速档不合格-免考（剔除，不进分子不进罚）',
  invalid_fast      int          not null default 0      comment '快速档无效（数据不可用）',
  qualified_econ    int          not null default 0,
  penalized_econ    int          not null default 0,
  exempted_econ     int          not null default 0,
  invalid_econ      int          not null default 0,
  undecodable_count int          not null default 0      comment '解码失败指令数',
  pended_count      int          not null default 0      comment '丁档挂起数',
  excluded_count    int          not null default 0      comment '乙档剔除披露计数',
  completeness_sum  decimal(14,4) not null default 0     comment '完整度求和（均值=sum/total_cmds，绝不存平均率）',
  recompute_at      datetime     default current_timestamp on update current_timestamp comment '重算批次时间（幂等覆盖）',
  primary key (id),
  unique key uk_grain_period_entity (stat_grain, stat_period, entity_id)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_0900_ai_ci comment='VQMS 调节合格率汇总（rollup 只存计数；率/罚款查询层重算：合格率=qualified/(total−invalid−exempted−undecodable 按口径)，缺额罚款=penalized 合计×容量/10⁴×0.02）';
-- 三状态模型+剔除法在计数列上的体现：exempted 列即"免考点"，率值显示口径与罚额推导均由查询层纯函数完成
```

### 3.13 投运率记账（D/M/Y 合一 + 主体维度）

```sql
create table vqms_runtime_stats (
  id                    bigint       not null auto_increment comment '主键',
  stat_grain            char(1)      not null                comment '粒度：D/M/Y',
  stat_period           date         not null                comment '统计期（同 regulation_stats 约定）',
  entity_id             bigint       not null                comment '考核主体（逻辑FK）',
  in_service_min        int          not null default 0      comment '投运分钟',
  exit_grid_min         int          not null default 0      comment '电网原因退出分钟（免责，出分母）',
  exit_nongrid_min      int          not null default 0      comment '非电网退出分钟（扣罚，在分母）',
  offline_min           int          not null default 0      comment '未并网分钟（不计账，透传核对）',
  rated_capacity_kw     decimal(12,3) default null           comment '计算时额定容量快照 kW（来源 vqms_entity）',
  rate_pct              decimal(6,3) default null            comment '投运率快照 %（in_service/(in_service+exit_nongrid)×100）；NULL=零并网分钟（无基数，非真0%）',
  shortfall_pct         decimal(6,3) default null            comment '缺额 pp 快照 max(0, 99−率)',
  penalty_score         decimal(12,3) default null           comment '考核罚款快照 分（缺额×容量/10⁴×0.02）',
  recompute_at          datetime     default current_timestamp on update current_timestamp comment '重算批次时间',
  primary key (id),
  unique key uk_grain_period_entity (stat_grain, stat_period, entity_id)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_0900_ai_ci comment='VQMS AVC 投运率记账（四桶分钟计数；率/罚款由 RuntimeStatistics 纯函数写回快照）';
```

### 3.14 摄取批次日志（数据质量闸门审计）

```sql
create table vqms_ingest_log (
  id                bigint       not null auto_increment comment '主键',
  batch_no          varchar(32)  not null                comment '批次号（如 20260902-0300-R）',
  source_table      varchar(64)  not null                comment '外部源表（warn_info/his_curve_sv/yc_history…）',
  range_start       datetime     not null                comment '抓取时段起',
  range_end         datetime     not null                comment '抓取时段止',
  rows_read         int          not null default 0      comment '读取行数',
  rows_accepted     int          not null default 0      comment '入库行数',
  rows_skipped_dirty int         not null default 0      comment '脏值跳过（0.0 电压/plan_SV 废值域/时间格式异常）',
  rows_skipped_dup  int          not null default 0      comment '重复跳过（uk 拦截）',
  skip_detail       varchar(512) default null            comment '跳过明细摘要（计数+原因分类）',
  status            char(1)      not null default '0'    comment '状态：0=成功, 1=部分失败, 2=失败',
  started_at        datetime     default current_timestamp comment '开始时间',
  finished_at       datetime     default null            comment '结束时间',
  remark            varchar(255) default null            comment '备注',
  primary key (id),
  unique key uk_batch (batch_no, source_table)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_0900_ai_ci comment='VQMS 摄取批次日志（数据质量闸门：脏值拦截/去重/时间解析异常的留痕）';
```

### 3.15 字典与调度种子（同 1.0 模式）

```sql
-- vqms_v_grade（sys_dict）：0=500kV, 1=220kV, 2=66kV及以下(预留，±1% 口径)
-- vqms_entity_type（sys_dict）：1火电 2水电 3核电 4风电 5光伏 6新型储能 7光热 8其他
-- vqms_device_type（sys_dict）：1同步机/调相机 2逆变器型 3SVC/STATCOM 4电容器 5电抗器
-- sys_job 种子：vqmsStatsTask.recomputeYesterday() 每日 03:00，默认暂停（上线拍板后启用）
```

## 四、与 1.0 的差异决策记录

| # | 决策 | 理由 |
|---|---|---|
| D1 | 新增 vqms_entity 主体维度，rated_capacity_kw 从 busbar_group 上移 | 政策考核主体=并网主体；1.0 厂级单一口径改进项⑤；多主体扩展就绪 |
| D2 | 统计三粒度三表合一（stat_grain+stat_period） | 1.0 改进项②；rollup SQL 与代码减半；PK 类型统一 |
| D3 | ledger/regulation_cmd 增 cmd_time datetime 解析列 | 1.0 改进项⑥；原文列保留作溯源键 |
| D4 | 解码算法版本化 ROT10_V1，弃 ÷100 | 现场核对报告发现①（阻断级）：÷100 全量误判 |
| D5 | 删 yx501_fast/econ 双列，免考改 exempt_source 三源（AUTO_YX/AUTO_DEVICE/MANUAL）+ vqms_exempt_annotation 审计表 | 核对报告发现③：yx501 现场无源；免考人工路径必须存在 |
| D6 | 新增 vqms_exit_annotation | 投运率免责（电网原因扣时）在 yc521/522 落盘前无自动源 |
| D7 | 新增 reactive_device + device_pq_limit | 附件6 §三 设备级免考判定的配置基础（政策口径 md 要求按设备类型建模极限） |
| D8 | 新增 vqms_ingest_log | 数据质量闸门（0值/废值/重复/格式）留痕，核对报告发现④的可审计落点 |
| D9 | regulation_cmd 修复 uk NULL 旁路（millisecond_uk 生成列入键） | 1.0 该表用原列入键，NULL 重复行可绕过幂等 |
| D10 | collation 统一 utf8mb4_0900_ai_ci；种子剔除合成占位点号 | 1.0 改进项①④ |
| D11 | 阈值表删 plan_sv_invalid_policy 死列 | 1.0 改进项③ |

## 五、待 Leo 拍板

1. **busbar 2（500kV，530/531kV 真数据 705 行）是否纳入考核范围**？纳入则种 vqms_busbar 行+组+阈值 1.500kV
2. **统计粒度合一（D2）是否接受**？若坚持 1.0 三表结构也可回退（结构差异仅键列）
3. **Phase 2 范围确认**：第 26 条电压考核（季度曲线表）、第 27 条 SVG/SVC、有偿无功——现在建表还是等 Phase 1 跑通？
4. **vqms_entity 种子**：本厂额定容量 600000 kW（2×300MW）待现场核实
5. **免考人工标注的审批流**：单级标注即可，还是需复核节点（标注人≠复核人）？

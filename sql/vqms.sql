-- ============================================================
-- VQMS2.0 电压质量监测系统 - 建表脚本（v1.0，2026-09-02）
--
-- 设计依据：docs/设计/VQMS2.0_MySQL表结构设计_v1.md（5 项拍板全部完成）
--   ① 运行时 220kV/500kV 单档在运、台账按档登记（busbar 0/1=220kV，2=500kV）
--   ② 统计粒度合一（stat_grain D/M/Y + stat_period DATE + entity 唯一键）
--   ③ Phase 2（第26条电压考核/第27条SVG-SVC/有偿无功）等 Phase 1 跑通再建
--   ④ entity 容量 600000 kW 种入、待现场核实
--   ⑤ 免考人工标注两级复核（PENDING→APPROVED 生效，标注人≠复核人）
--
-- 相对 VQMS 1.0（C:\work\VQMS\sql\vqms.sql）的关键变更：
--   * 新增主体维度 vqms_entity（考核基数载体），容量从 busbar_group 上移
--   * 统计三粒度三表合一为 vqms_regulation_stats / vqms_runtime_stats
--   * ledger/cmd 增 cmd_time datetime 解析列（保留原文列作溯源键）
--   * 解码算法版本化 ROT10_V1（轮转码{1,2,3}+÷10；1.0 的 ÷100 已被现场核对证伪）
--   * 删 yx501 依赖：免考改三源（AUTO_YX/AUTO_DEVICE/MANUAL）+ 两级复核标注表
--   * 新增投运率退出原因标注表（yc521/522 落盘前的人工路径）
--   * 新增无功设备台账 + P-Q 极限曲线（附件6§三 设备级免考判定）
--   * 新增摄取批次日志（数据质量闸门留痕）
--   * 修复 1.0 vqms_regulation_cmd 的 uk NULL 旁路（millisecond_uk 生成列入键）
--   * collation 统一 utf8mb4_0900_ai_ci；种子剔除合成占位点号
--
-- ⚠️⚠️ 破坏性脚本，严禁对已有数据的环境重复执行 ⚠️⚠️
--   每张表开头都是 DROP TABLE IF EXISTS——重跑会清空重建：
--   人工维护的配置表（entity/busbar/threshold/point_map/judge_param 等）会回到初始种子，
--   现场已录入的母线、调整过的容差/整定参数、人工标注、免考复核记录全部丢失；
--   vqms_command_ledger 为只增流水账，重跑会清空（可从外部源重抓，无人工数据损失）。
--   仅限全新部署首启执行；线上变更表结构请用 ALTER 或增量迁移脚本，勿整脚本重跑。
--
-- 与 RuoYi sys_* 表同库；库名由部署决定，本脚本不含 CREATE DATABASE/USE
-- 首启执行顺序：00-create-app-user.sh → quartz.sql → ry_20260417.sql → vqms.sql → vqms_menu.sql
--   （vqms_menu.sql 须在 ry 建好 sys_menu 后执行；本脚本末尾 UPDATE 覆盖 ry 默认值）
-- ============================================================


-- ============================================================
-- 一、主体与母线元数据
-- ============================================================

-- 1、并网主体（考核主体，罚款基数载体）
drop table if exists vqms_entity;
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

-- 种子：本厂 2×300MW（对端 GENERATOR.ratingPPower=300000 kW×2），容量待现场核实（拍板④）
insert into vqms_entity (entity_code, entity_name, entity_type, rated_capacity_kw, remark) values
  ('PLANT_01', '本厂（名称待现场核实）', '1', 600000.000, '额定容量来自对端 AVC 配置库 2×300MW；⚠️待与监管结算口径核实后修正（拍板④ 2026-09-02）');


-- 2、母线组（主母线判定单元）
--    与外部库 QHeatAvcRtdb.BUSBAR_GROUP（大写、不读）只是同名，毫无关系
drop table if exists vqms_busbar_group;
create table vqms_busbar_group (
  group_num               bigint       not null                comment '母线组编号',
  entity_id               bigint       not null                comment '所属并网主体（逻辑FK → vqms_entity）',
  group_name              varchar(64)  not null                comment '组名',
  v_grade                 tinyint      not null                comment '电压等级编码，对齐字典 vqms_v_grade：0=500kV, 1=220kV, 2=66kV及以下(预留)',
  main_indicator_yc_num   bigint       default null            comment '该组当前主母线号指示点（对端 BUSBAR_GROUP.MainBarYcNum=3 候选）；未接入前为空',
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

-- 种子：两档各一组（拍板①：运行时单档在运，数据对上哪档统计哪档）。
--   500kV 组兜底=2：组内唯一母线无"选哪条"歧义，指示点失效兜底零风险（评审确认 2026-09-02，修正 1.0 占位惯性）
insert into vqms_busbar_group (group_num, entity_id, group_name, v_grade, main_indicator_yc_num, default_main_busbar_num) values
  (0, 1, '220kV母线组', 1, 4001, 0),
  (1, 1, '500kV母线组', 0, null, 2);


-- 3、主母线元数据
drop table if exists vqms_busbar;
create table vqms_busbar (
  busbar_num      bigint       not null                comment '主母线编号，对齐 his_curve_sv.busbar_num（值对齐直连，设计原则9）',
  busbar_name     varchar(64)  not null                comment '母线名称（0/1=220kV 东/西母线，2=500kV——拍板①单档在运、按档登记）',
  v_grade         tinyint      not null                comment '电压等级编码，对齐字典 vqms_v_grade：0=500kV, 1=220kV, 2=66kV及以下(预留)',
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

-- 种子：busbar 2（500kV，现场 705 行真数据 530/531kV）按拍板①登记
-- 实时电压点 sim 号 4002/4003 启用（真实候选 yc8 东母/yc14 西母，现场核对后 UPDATE 台账即可）
insert into vqms_busbar (busbar_num, busbar_name, v_grade, group_num, nominal_kv, realtime_yc_num) values
  (0, '220kV 东母线', 1, 0, 220.000, 4002),
  (1, '220kV 西母线', 1, 0, 220.000, 4003),
  (2, '500kV 母线',  0, 1, 500.000, null);


-- 4、阈值（带生效区间，变更不回溯）
drop table if exists vqms_busbar_threshold;
create table vqms_busbar_threshold (
  threshold_id   bigint       not null auto_increment comment '主键',
  busbar_num     bigint       not null                comment '母线编号（逻辑FK → vqms_busbar）',
  criterion_type varchar(8)   not null default 'AVC'  comment '口径：AVC=附件6 合格区间 / GB=国标；附件6：500kV±1.5kV、220kV±1kV、66kV及以下±1%额定',
  tolerance_v    decimal(10,3) default null           comment 'AVC 容差 kV：220kV=1.000, 500kV=1.500；66kV及以下按 ±1% 在判定层由 nominal_kv 折算，本列可空',
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

-- 种子：附件6 政策容差（220kV±1.000 / 500kV±1.500）
insert into vqms_busbar_threshold (busbar_num, criterion_type, tolerance_v, effective_from) values
  (0, 'AVC', 1.000, '2026-01-01'),
  (1, 'AVC', 1.000, '2026-01-01'),
  (2, 'AVC', 1.500, '2026-01-01');


-- ============================================================
-- 二、判定输入配置
-- ============================================================

-- 5、点号语义注册（含对端 JS 引擎虚拟点；种子只录真实候选，合成占位一律不种——1.0 撞号教训）
drop table if exists vqms_yc_point_map;
create table vqms_yc_point_map (
  point_num     bigint       not null                comment '点号（yc/yx 统一注册，对齐外部 yc_history / yx_history）',
  point_key     varchar(64)  default null            comment '语义键（管线消费用，唯一：grid_signal_main/avc_onoff/exempt_flag 等；空=资料行不参与管线）',
  point_kind    char(1)      not null default 'C'    comment 'C=遥测 yc / X=遥信 yx',
  point_name    varchar(64)  not null                comment '语义名称',
  point_type    varchar(32)  default null            comment 'busbar_id=主母线号 / voltage=电压 / power=有功 / reactive=无功 / yx=开关量 / analog=编码量',
  entity_id     bigint       default null            comment '归属主体（逻辑FK）',
  busbar_num    bigint       default null            comment '关联母线（逻辑FK）',
  unit          varchar(32)  default null            comment '单位（模拟量）',
  state_1_label varchar(32)  default null            comment 'yx 值=1 语义',
  state_0_label varchar(32)  default null            comment 'yx 值=0 语义',
  gate_enabled  tinyint(1)   not null default 0      comment '是否启用为考核门控：1=启用；真实环境默认0，现场核对后置1',
  status        char(1)      not null default '0'    comment '状态：0=正常, 1=停用',
  create_time   datetime     default current_timestamp comment '创建时间',
  update_time   datetime     default current_timestamp on update current_timestamp comment '更新时间',
  remark        varchar(255) default null            comment '备注（含现场核对结论）',
  primary key (point_num),
  unique key uk_point_key (point_key)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_0900_ai_ci comment='VQMS 点号语义注册表';

-- 种子：真实候选（资料行，无语义键，gate_enabled=0 现场核对后启用）+ 管线语义键行（sim/JS 派生号，已启用）
-- 现场接线配置化（2026-09-03）：管线按 point_key 消费，现场换号只 UPDATE 本表，不改代码不发版
insert into vqms_yc_point_map (point_num, point_key, point_kind, point_name, point_type, unit, state_1_label, state_0_label, gate_enabled, remark) values
  (3,    null, 'C', '主母线号(现场候选)',     'busbar_id', null,  null, null, 0, 'BUSBAR_GROUP.MainBarYcNum=3；值域预期 0/1=东/西母线，待现场核对'),
  (8,    null, 'C', '实时母线电压·东母',      'voltage',   'kV',  null, null, 0, 'BUSBAR.realVYcNum=8；CHUNNEL_YC=1#高压采集×0.01；待现场核对（vqms_busbar.realtime_yc_num 落库生效）'),
  (14,   null, 'C', '实时母线电压·西母',      'voltage',   'kV',  null, null, 0, 'BUSBAR.realVYcNum=14；CHUNNEL_YC=2#高压采集×0.01；待现场核对（vqms_busbar.realtime_yc_num 落库生效）'),
  (216,  null, 'C', '实时有功·1号机',         'power',     'kW',  null, null, 0, 'GENERATOR.pYcNum=216；与 yc316 相加得全厂总有功；待现场核对（vqms_reactive_device.p_yc_num 落库生效）'),
  (316,  null, 'C', '实时有功·2号机',         'power',     'kW',  null, null, 0, 'GENERATOR.pYcNum=316；与 yc216 相加得全厂总有功；待现场核对（vqms_reactive_device.p_yc_num 落库生效）'),
  (217,  null, 'C', '实时无功·1号机',         'reactive',  'kvar',null, null, 0, 'GENERATOR.qYcNum=217；设备级免考判定用；待现场核对（vqms_reactive_device.q_yc_num 落库生效）'),
  (317,  null, 'C', '实时无功·2号机',         'reactive',  'kvar',null, null, 0, 'GENERATOR.qYcNum=317；设备级免考判定用；待现场核对（vqms_reactive_device.q_yc_num 落库生效）'),
  (11,   null, 'C', '母线总无功·正母单元',    'reactive',  'kvar',null, null, 0, '对端 JS 计算（yc217+317 累加）；结果点 11'),
  (17,   null, 'C', '母线总无功·副母单元',    'reactive',  'kvar',null, null, 0, '对端 JS 计算；结果点 17'),
  (511,  'grid_signal_main',  'C', '并网编码·正母单元', 'analog', null, null, null, 1, '对端 JS：带电(1/0)×10+并网机组数；电厂并网=yc511≥10 OR yc512≥10'),
  (512,  'grid_signal_aux',   'C', '并网编码·副母单元', 'analog', null, null, null, 1, '同 yc511（副母单元）'),
  (521,  'exit_reason_main',  'C', 'AVC退出原因·正母',  'analog', null, null, null, 1, '三态 0=未退出/1=电网原因(免责)/2=非电网(扣罚)；对端接口已定待落盘，落盘前走人工标注'),
  (522,  'exit_reason_aux',   'C', 'AVC退出原因·副母',  'analog', null, null, null, 1, '三态同 yc521（副母）'),
  (3009, 'avc_onoff',         'C', 'AVC投退(sim)',      'analog', null, '投入', '退出', 1, 'sim 占位号：真实库 yc3009=四号机组下闭锁总信号（JS_DATA js109）撞号不同义；真实候选 yx1001（AVC_INFO.AVCStatusYxNum）——现场核对后 UPDATE 本行 point_num=1001'),
  (1001, null, 'X', 'AVC投退(现场候选)',      'yx',        null,  '投入', '退出', 0, 'AVC_INFO.AVCStatusYxNum=1001；语义待现场核对（核对后把 avc_onoff 语义键行换号到此）'),
  (210,  null, 'X', '机组投退·1号机',         'yx',        null,  '并网', '解列', 0, '对端 JS（断路器∧刀闸）；结果点 210'),
  (310,  null, 'X', '机组投退·2号机',         'yx',        null,  '并网', '解列', 0, '对端 JS（断路器∧刀闸）；结果点 310'),
  (2003, null, 'X', '远方就地总',             'yx',        null,  '远方', '就地', 0, '对端派生点 OR(yx12,yx23)；warn_info 有 obj_num=2003 事件佐证'),
  (501,  'exempt_flag',       'X', '免考旗(无源)',      'yx',        null,  '免考', '考核', 1, '现场库不存在（核对报告发现③）；注册备对端实现，落盘前免考走三源判定')


-- 6、判定整定参数（CHECK 双层：行本地值域 + 锁定行钉值；1.0 D7 成熟模式）
drop table if exists vqms_judge_param;
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
  -- 蕴含式（非该键恒真，仅锁定键钉值/值域列）；跨行 t_fast<t_econ 由值域传导保证；
  -- CHECK 不拦 DELETE，必需行删除由 Service 层拦（1.0 D7 对抗验证结论）
  constraint ck_value_range check (value_min is null or value_max is null or param_value between value_min and value_max),
  constraint ck_locked_rows check (
    (param_key <> 't_econ' or (param_value <=> 5 and value_min <=> 5 and value_max <=> 5))
    and (param_key <> 'tier_threshold_fast' or (param_value <=> 1 and value_min <=> 1 and value_max <=> 1))
    and (param_key <> 'tier_threshold_econ' or (param_value <=> 5 and value_min <=> 5 and value_max <=> 5))
    and (param_key <> 't_fast' or (value_min <=> 1 and value_max <=> 4))
  )
) engine=innodb default charset=utf8mb4 collate=utf8mb4_0900_ai_ci comment='VQMS 判定整定参数';

-- 种子（1.0 Leo 2026-08-14 定，2.0 沿用）：t_fast=4 可整定[1,4]；t_econ=5 锁定（指令5分钟间隔）；
--   分档阈值 1/5 为外部依据待确认（附件6 原文无 5 分钟分档条款，VQMS 细化口径）
insert into vqms_judge_param (param_key, param_value, name, description, value_min, value_max) values
  ('t_fast',              4, '快速性档窗口(分钟)',     '快速性档扫描窗口 [1, t_fast]，整数可整定', 1, 4),
  ('t_econ',              5, '经济性档窗口上限(分钟)', '写死=5（指令 5 分钟间隔），锁定不可改',    5, 5),
  ('tier_threshold_fast', 1, '快速性档分档阈值(分钟)', '外部依据待确认（附件6 无 5 分钟分档条款）', 1, 1),
  ('tier_threshold_econ', 5, '经济性档分档阈值(分钟)', '外部依据待确认（附件6 无 5 分钟分档条款）', 5, 5),
  ('exempt_q_tol_kvar', 2000, '设备级免考顶满容差(kvar)', '设备Q距极限≤该值视为顶满（附件6§三无ε规定，现场整定）', 0, 100000),
  ('min_window_completeness_pct', 50, '档窗口最低完整度(%)', 'completeness低于该值的档判INVALID不硬判（数据公平性：缺数窗不罚电厂；1.0数据不可用策略A3/A4最小口径，0=关闭）', 0, 100);


-- 7、数据不可用策略参数（原子组合·戊路线唯一实现；Leo 2026-09-02 拍板"完全按照原子性设计实现"）
--    1.0 演进结论：甲乙丙丁四套预设已全部退役（2026-08-26 零残留），仅保留原子组合：
--    原子 A1 解码失败（MECE 三分：A1a 编码脏写/A1b 循环码非法/A1c 缺t0电压，A1 成立短路 A2~A4）、
--    A2 档不可判（跨档可与 A3 并存）、A3 窗口部分缺（0<completeness<1）、A4 可用度<阈值τ（依赖 A3）、
--    A5 免考旗读取失败（阶段三独立子规则表，结论覆写三选一，不与 A1~A4 混排）；
--    组合机制：有序规则表「表达式→动作」DSL，同层混用须一层括号、嵌套上限一层，求值首中即断、
--    全不中兜底 COUNT_NORMAL(ruleId=NULL)；动作域沿用四桶零新增；应用校验 fail-fast（不满足整体拒绝、原策略保持）。
--    完整定义：VQMS 1.0 docs/数据不可用处理策略.md §3.3
drop table if exists vqms_policy_param;
create table vqms_policy_param (
  param_id    bigint       not null auto_increment comment '主键',
  param_key   varchar(64)  not null                comment '参数键：规则行 freeform_rule_001..N（按序号有序）/ freeform_threshold_pct（A4 阈值 τ，默认 50 可整定）',
  param_value varchar(255) default null            comment '参数值（规则行=「表达式->动作」DSL 文本；τ=整数百分比；空表=策略未配置，管线只记不判）',
  name        varchar(64)  not null                comment '参数名称',
  description varchar(255) default null            comment '说明',
  create_by   varchar(64)  default ''              comment '创建者',
  create_time datetime     default current_timestamp comment '创建时间',
  update_by   varchar(64)  default ''              comment '更新者',
  update_time datetime     default null            comment '更新时间',
  primary key (param_id),
  unique key uk_policy_key (param_key)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_0900_ai_ci comment='VQMS 数据不可用策略参数表（原子组合规则行存储；零种子=未生效）';


-- ============================================================
-- 三、指令流水与判定明细
-- ============================================================

-- 8、AVC 指令流水账（原始事实，只增；存储切分铁律唯一有界例外 ~288行/天）
--    幂等：可空列经生成列归一（NULL→''/-1）进 uk，INSERT IGNORE 结构性拦截重复抓取；
--    millisecond 与源同宽 varchar(255) 防超宽脏值静默截断进 uk 致假碰撞丢行（1.0 D8 教训）
drop table if exists vqms_command_ledger;
create table vqms_command_ledger (
  id             bigint       not null auto_increment comment '主键',
  warn_time_raw  varchar(32)  not null                comment '指令时间原文（warn_info.warn_time 原样，忠实摘录；格式校验在读取层）',
  cmd_time       datetime     not null                comment '解析后指令时刻（北京历法；源 varchar 双格式解析、秒≥30进位到分钟由摄取层完成并保证确定性）',
  millisecond    varchar(255) default null            comment '毫秒原文（与源同宽防截断失真）',
  warn_type      int          not null default 5      comment '类型；电压指令=5（本账只收指令）',
  obj_num        bigint       default null            comment '对象编号（现场恒=2 厂级；不参与逻辑FK）',
  warn_content   varchar(255) default null            comment '指令文本原文（目标值/增量值编码在此文本内，判定层解码）',
  fetched_at     datetime     default current_timestamp comment '抓取入库时间',
  millisecond_uk varchar(255) generated always as (coalesce(millisecond, '')) stored comment 'uk 键列：NULL 归一空串（应用不读写）',
  obj_num_uk     bigint       generated always as (coalesce(obj_num, -1)) stored comment 'uk 键列：NULL 归一 -1（应用不读写）',
  primary key (id),
  unique key uk_cmd (warn_time_raw, millisecond_uk, obj_num_uk),
  key idx_cmd_time (cmd_time)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_0900_ai_ci comment='VQMS AVC 指令流水账（原始事实，只增）';


-- 9、指令级判定明细（每条入判指令一行：判定+免考+策略处置）
drop table if exists vqms_regulation_cmd;
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
  response_minutes   int          default null            comment '响应时长（分钟）：t0→电压入合格区间时刻；NULL=未入区间/窗口缺数据',
  t_fast_snapshot    int          not null                comment '判定时 t_fast 快照（跨整定期重算可复现）',
  fast_state         varchar(16)  not null                comment '快速档最终记账：QUALIFIED/PENALIZED/EXEMPTED/INVALID',
  econ_state         varchar(16)  not null                comment '经济档最终记账（两档平行互不隶属）',
  completeness       decimal(5,4) not null                comment '窗口完整度 [0,1]',
  invalid_tiers      varchar(16)  default null            comment '原始按档无效标记 FAST/ECON/FAST,ECON；NULL=无',
  undecodable_reason varchar(32)  default null            comment '解码失败归因 CYCLE_CODE_INVALID/MISSING_T0_VOLTAGE/CORRUPTED_ENCODING；NULL=成功',
  exempt_source      varchar(16)  default null            comment '免考来源：AUTO_YX(免考旗采样)/AUTO_DEVICE(设备级Q极限判定)/MANUAL(人工标注)；NULL=无免考',
  exempt_ref_id      bigint       default null            comment '免考溯源：MANUAL→vqms_exempt_annotation.annotation_id；AUTO_*→NULL（依据随行）',
  disposition        varchar(32)  default null            comment '策略处置桶 COUNT_NORMAL/EXCLUDE_REPORTED/COUNT_UNQUALIFIED/PEND_MARKED（原子组合规则表求值结果）；NULL=策略未生效（规则表未配置时只记不判）',
  hit_rule_id        varchar(8)   default null            comment '戊命中规则 ID（R001…；NULL=兜底/预设/未选套）',
  fetched_at         datetime     default current_timestamp comment '写入时间',
  obj_num_uk         bigint       generated always as (coalesce(obj_num, -1)) stored comment 'uk 键列（应用不读写）',
  millisecond_uk     varchar(255) generated always as (coalesce(millisecond, '')) stored comment 'uk 键列（应用不读写；修复 1.0 该表用原列入键的 NULL 旁路）',
  primary key (id),
  unique key uk_cmd_result (warn_time_raw, millisecond_uk, obj_num_uk),
  key idx_stat_date (stat_date, entity_id)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_0900_ai_ci comment='VQMS 调节合格率指令级明细';


-- ============================================================
-- 四、人工标注（两级复核）
-- ============================================================

-- 10、调节免考人工标注（拍板⑤：两级复核，重算只应用 APPROVED 行）
drop table if exists vqms_exempt_annotation;
create table vqms_exempt_annotation (
  annotation_id  bigint       not null auto_increment comment '主键',
  entity_id      bigint       not null                comment '考核主体（逻辑FK）',
  warn_time_raw  varchar(32)  not null                comment '指向指令（与 millisecond/obj_num 组成溯源键）',
  millisecond    varchar(255) default null            comment '毫秒原文（溯源键成分）',
  obj_num        bigint       default null            comment '对象编号（溯源键成分）',
  tier           varchar(8)   not null default 'BOTH' comment '免考档：FAST/ECON/BOTH',
  exempt_reason  varchar(255) not null                comment '免考依据（附件6§三：全部闭环无功设备正确方向顶满仍不达标 等）',
  evidence       varchar(512) default null            comment '佐证材料描述（设备Q曲线截图/调度电话记录等）',
  review_status  varchar(16)  not null default 'PENDING' comment '复核状态：PENDING=待复核 / APPROVED=已批准（生效） / REJECTED=已驳回',
  review_by      varchar(64)  default null            comment '复核人（≠标注人，Service 层校验）',
  review_time    datetime     default null            comment '复核时间',
  review_opinion varchar(255) default null            comment '复核意见（驳回原因等）',
  status         char(1)      not null default '0'    comment '状态：0=有效, 1=撤销',
  create_by      varchar(64)  default ''              comment '标注人',
  create_time    datetime     default current_timestamp comment '标注时间',
  update_by      varchar(64)  default ''              comment '更新者',
  update_time    datetime     default current_timestamp on update current_timestamp comment '更新时间',
  remark         varchar(255) default null            comment '备注',
  primary key (annotation_id),
  key idx_cmd (warn_time_raw, obj_num),
  key idx_review (review_status, entity_id)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_0900_ai_ci comment='VQMS 调节免考人工标注（两级复核生效）';


-- 11、AVC 退出原因标注（投运率免责输入；yc521/522 落盘前以人工为主）
drop table if exists vqms_exit_annotation;
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
) engine=innodb default charset=utf8mb4 collate=utf8mb4_0900_ai_ci comment='VQMS AVC 退出原因标注（投运率免责判定输入）';


-- ============================================================
-- 五、无功设备（附件6§三 设备级免考判定）
-- ============================================================

-- 12、无功设备台账
drop table if exists vqms_reactive_device;
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
) engine=innodb default charset=utf8mb4 collate=utf8mb4_0900_ai_ci comment='VQMS 无功设备台账（免考判定单元）';

-- 种子：两台机组（对端 GENERATOR 蓝本：2×300MW，max/minQ=+200000/−100000 kvar；P-Q 曲线待录入）
insert into vqms_reactive_device (entity_id, device_code, device_name, device_type, rated_q_up_kvar, rated_q_down_kvar, q_yc_num, p_yc_num, remark) values
  (1, 'GEN_01', '1号发电机组', 1, 200000.000, -100000.000, 217, 216, '额定值来自对端 GENERATOR 静态列；随P变化的精确极限待录 vqms_device_pq_limit'),
  (1, 'GEN_02', '2号发电机组', 1, 200000.000, -100000.000, 317, 316, '同 1号机');

-- P-Q 曲线种子（300MW 机组三点插值蓝本；端点与静态额定一致，现场实测后换版）
insert into vqms_device_pq_limit (device_id, p_kw, q_up_kvar, q_down_kvar, effective_from, remark)
select device_id, v.p_kw, v.q_up, v.q_down, '2020-01-01', '三点插值蓝本（0/150/300MW），自最早覆盖（历史回放同口径）；现场实测换版走新 effective_from'
from vqms_reactive_device d
join (
  select 0.000 p_kw, 250000.000 q_up, -150000.000 q_down
  union all select 150000.000, 225000.000, -120000.000
  union all select 300000.000, 200000.000, -100000.000
) v
where d.device_code in ('GEN_01', 'GEN_02')
  and not exists (select 1 from vqms_device_pq_limit l where l.device_id = d.device_id and l.effective_from = '2020-01-01' and l.p_kw = v.p_kw);


-- 13、P-Q 双向极限曲线（仅发电机类需要；蓝本：对端 GENERATOR_P_QLimit）
drop table if exists vqms_device_pq_limit;
create table vqms_device_pq_limit (
  id             bigint       not null auto_increment comment '主键',
  device_id      bigint       not null                comment '设备（逻辑FK → vqms_reactive_device）',
  p_kw           decimal(12,3) not null               comment '有功工况点 kW',
  q_up_kvar      decimal(12,3) not null               comment '该 P 下发出上限 kvar',
  q_down_kvar    decimal(12,3) not null               comment '该 P 下吸收下限 kvar（负值）',
  effective_from date         not null                comment '生效起始日（曲线换版不回溯）',
  effective_to   date         default null            comment '生效结束日；NULL=至今',
  create_time    datetime     default current_timestamp comment '创建时间',
  update_time    datetime     default current_timestamp on update current_timestamp comment '更新时间',
  remark         varchar(255) default null            comment '备注',
  primary key (id),
  unique key uk_device_p (device_id, p_kw, effective_from)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_0900_ai_ci comment='VQMS 设备 P-Q 双向极限曲线';
-- 免考判定（AUTO_DEVICE）：电压偏低→各闭环设备顶到 q_up_kvar（或逆变器 ±√(S²−P²)）；偏高→q_down_kvar；
--   方向错或留余力均不算尽力；电容/电抗单向、SVC/STATCOM 连续对称


-- ============================================================
-- 六、统计汇总（三粒度合一，拍板②）
-- ============================================================

-- 14、调节合格率汇总
drop table if exists vqms_regulation_stats;
create table vqms_regulation_stats (
  id                bigint       not null auto_increment comment '主键',
  stat_grain        char(1)      not null                comment '粒度：D=日 / M=月 / Y=年',
  stat_period       date         not null                comment '统计期（D=当日 / M=当月首日 / Y=当年1月1日）',
  entity_id         bigint       not null                comment '考核主体（逻辑FK）',
  algorithm_id      varchar(16)  default null            comment '周期内算法：单一=该 ID / 混合=MIXED',
  total_cmds        int          not null default 0      comment '发令总次数=分母（固定分母口径）',
  qualified_fast    int          not null default 0      comment '快速档合格数',
  penalized_fast    int          not null default 0      comment '快速档不合格-非免考（扣罚）',
  exempted_fast     int          not null default 0      comment '快速档不合格-免考（剔除法，不进分子不进罚）',
  invalid_fast      int          not null default 0      comment '快速档无效（数据不可用）',
  qualified_econ    int          not null default 0,
  penalized_econ    int          not null default 0,
  exempted_econ     int          not null default 0,
  invalid_econ      int          not null default 0,
  undecodable_count int          not null default 0      comment '解码失败指令数（归因分布看明细表）',
  pended_count      int          not null default 0      comment '丁档挂起数',
  excluded_count    int          not null default 0      comment '乙档剔除披露计数——现行拍板不剔分母，仅披露',
  completeness_sum  decimal(14,4) not null default 0     comment '完整度求和（均值=sum/total_cmds，绝不存平均率）',
  recompute_at      datetime     default current_timestamp on update current_timestamp comment '重算批次时间（幂等覆盖）',
  primary key (id),
  unique key uk_grain_period_entity (stat_grain, stat_period, entity_id)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_0900_ai_ci comment='VQMS 调节合格率汇总（rollup 只存计数）';
-- 率/罚款由查询层纯函数重算，分母口径分层：exempted=附件6明文豁免、剔除法天然出分母；
--   invalid/undecodable 是否参与减法由 vqms_policy_param 当前生效的原子组合规则表决定（固定分母口径=不减），非固定规则；
--   缺额罚款 = penalized 合计 × 容量(kW)/10⁴ × 0.02 分/万千瓦
-- 计数列分层（防误读）：invalid_fast/econ 按【判定状态四态】统计，pended/excluded_count 按【策略处置四桶】统计，
--   两维正交可同计一条指令（如 fast_state=INVALID 且处置=EXCLUDE_REPORTED），有意分层非重复计数


-- 15、投运率记账（四桶分钟计数）
drop table if exists vqms_runtime_stats;
create table vqms_runtime_stats (
  id                bigint       not null auto_increment comment '主键',
  stat_grain        char(1)      not null                comment '粒度：D/M/Y',
  stat_period       date         not null                comment '统计期（同 regulation_stats 约定）',
  entity_id         bigint       not null                comment '考核主体（逻辑FK）',
  in_service_min    int          not null default 0      comment '投运分钟',
  exit_grid_min     int          not null default 0      comment '电网原因退出分钟（免责，出分母）',
  exit_nongrid_min  int          not null default 0      comment '非电网退出分钟（扣罚，在分母）',
  offline_min       int          not null default 0      comment '未并网分钟（不计账，透传核对用）',
  rated_capacity_kw decimal(12,3) default null           comment '计算时额定容量快照 kW（来源 vqms_entity）',
  rate_pct          decimal(6,3) default null            comment '投运率快照 %（in_service/(in_service+exit_nongrid)×100）；NULL=零并网分钟（无基数，非真0%）',
  shortfall_pct     decimal(6,3) default null            comment '缺额 pp 快照 max(0, 99−率)',
  penalty_score     decimal(12,3) default null           comment '考核罚款快照 分（缺额×容量/10⁴×0.02）',
  recompute_at      datetime     default current_timestamp on update current_timestamp comment '重算批次时间',
  primary key (id),
  unique key uk_grain_period_entity (stat_grain, stat_period, entity_id)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_0900_ai_ci comment='VQMS AVC 投运率记账（率/罚款由纯函数写回快照）';


-- ============================================================
-- 七、摄取日志
-- ============================================================

-- 16、摄取批次日志（数据质量闸门留痕）
drop table if exists vqms_ingest_log;
create table vqms_ingest_log (
  id                 bigint       not null auto_increment comment '主键',
  batch_no           varchar(32)  not null                comment '批次号（如 20260902-0300-R）',
  source_table       varchar(64)  not null                comment '外部源表（warn_info/his_curve_sv/yc_history…）',
  range_start        datetime     not null                comment '抓取时段起',
  range_end          datetime     not null                comment '抓取时段止',
  rows_read          int          not null default 0      comment '读取行数',
  rows_accepted      int          not null default 0      comment '入库行数',
  rows_skipped_dirty int          not null default 0      comment '脏值跳过（0.0电压/plan_SV废值域/时间格式异常/未登记母线）',
  rows_skipped_dup   int          not null default 0      comment '重复跳过（uk 拦截）',
  skip_detail        varchar(512) default null            comment '跳过明细摘要（计数+原因分类）',
  status             char(1)      not null default '0'    comment '状态：0=成功, 1=部分失败, 2=失败',
  started_at         datetime     default current_timestamp comment '开始时间',
  finished_at        datetime     default null            comment '结束时间',
  remark             varchar(255) default null            comment '备注',
  primary key (id),
  unique key uk_batch (batch_no, source_table)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_0900_ai_ci comment='VQMS 摄取批次日志';


-- ============================================================
-- 八、字典（sys_dict，delete+insert 幂等可重执行）
-- ============================================================

-- 电压等级（编码与 vqms_busbar.v_grade 严格对齐勿改；2=66kV及以下为预留档）
delete from sys_dict_data where dict_type = 'vqms_v_grade';
delete from sys_dict_type where dict_type = 'vqms_v_grade';
insert into sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
values ('电压等级', 'vqms_v_grade', '0', 'admin', sysdate(), 'VQMS 电压等级（0=500kV,1=220kV,2=66kV及以下预留；容差 500kV±1.5kV/220kV±1kV/66kV±1%额定）');
insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
values (1, '500kV',      '0', 'vqms_v_grade', '', 'danger',  'N', '0', 'admin', sysdate(), ''),
       (2, '220kV',      '1', 'vqms_v_grade', '', 'primary', 'N', '0', 'admin', sysdate(), ''),
       (3, '66kV及以下', '2', 'vqms_v_grade', '', 'info',    'N', '0', 'admin', sysdate(), '预留档：现场出现 66kV 母线时启用；容差口径为 ±1% 额定电压，异于固定 kV 档');

-- 并网主体类型
delete from sys_dict_data where dict_type = 'vqms_entity_type';
delete from sys_dict_type where dict_type = 'vqms_entity_type';
insert into sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
values ('并网主体类型', 'vqms_entity_type', '0', 'admin', sysdate(), 'VQMS 并网主体类型');
insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
values (1, '火电',     '1', 'vqms_entity_type', '', 'primary', 'Y', '0', 'admin', sysdate(), ''),
       (2, '水电',     '2', 'vqms_entity_type', '', 'info',    'N', '0', 'admin', sysdate(), ''),
       (3, '核电',     '3', 'vqms_entity_type', '', 'danger',  'N', '0', 'admin', sysdate(), ''),
       (4, '风电',     '4', 'vqms_entity_type', '', 'success', 'N', '0', 'admin', sysdate(), ''),
       (5, '光伏',     '5', 'vqms_entity_type', '', 'success', 'N', '0', 'admin', sysdate(), ''),
       (6, '新型储能', '6', 'vqms_entity_type', '', 'warning', 'N', '0', 'admin', sysdate(), ''),
       (7, '光热',     '7', 'vqms_entity_type', '', 'warning', 'N', '0', 'admin', sysdate(), ''),
       (8, '其他',     '8', 'vqms_entity_type', '', 'info',    'N', '0', 'admin', sysdate(), '');

-- 无功设备类型
delete from sys_dict_data where dict_type = 'vqms_device_type';
delete from sys_dict_type where dict_type = 'vqms_device_type';
insert into sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
values ('无功设备类型', 'vqms_device_type', '0', 'admin', sysdate(), 'VQMS 无功设备类型（附件6§三 免考判定的极限建模方式随类型不同）');
insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
values (1, '同步发电机/调相机', '1', 'vqms_device_type', '', 'primary', 'Y', '0', 'admin', sysdate(), '极限随P变化，查 vqms_device_pq_limit 曲线'),
       (2, '逆变器型(风/光/储)', '2', 'vqms_device_type', '', 'success', 'N', '0', 'admin', sysdate(), '对称 ±√(S²−P²)，按 rated_s_kva 计算'),
       (3, 'SVC/STATCOM',       '3', 'vqms_device_type', '', 'warning', 'N', '0', 'admin', sysdate(), '连续对称，双向额定值直填'),
       (4, '电容器组',           '4', 'vqms_device_type', '', 'info',    'N', '0', 'admin', sysdate(), '单向只能发出'),
       (5, '电抗器',             '5', 'vqms_device_type', '', 'info',    'N', '0', 'admin', sysdate(), '单向只能吸收');


-- ============================================================
-- 九、部署定制与调度种子
-- ============================================================

-- 关闭登录验证码（1.0 定制沿用；不改 RuoYi 原生文件，末尾覆盖默认值）
UPDATE sys_config SET config_value = 'false' WHERE config_key = 'sys.account.captchaEnabled';

-- Quartz 统计任务种子：每日 03:00 缺口补算到昨日全链（Phase 1 上线 2026-09-03 默认启用）
insert into sys_job (job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, create_by, create_time, remark)
select 'VQMS 统计日重算（调节+投运率+rollup）', 'DEFAULT', 'vqmsStatsTask.recomputeYesterday()',
        '0 0 3 * * ?', '3', '1', '0', 'admin', sysdate(),
        '每日 03:00 缺口补算到昨日全链（停机/misfire 自愈，单次上限 92 天超限报错提示手动分批；幂等可重跑；空数据日自动跳过不记账）'
where not exists (select 1 from sys_job where invoke_target = 'vqmsStatsTask.recomputeYesterday()');

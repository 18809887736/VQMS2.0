-- 2026-09-03_02 现场接线配置化：vqms_yc_point_map 加语义键列 point_key + 六语义键回填启用 + 台账 sim 点号落库
-- 背景：管线改为按 point_key 消费注册表（现场换号只 UPDATE 不改代码）；140 为 sim 联调库，语义键行启用 sim 点号。
-- 幂等可重跑。

-- 1) 加列 + 唯一键（MySQL 8 无 add column if not exists，经 information_schema 判幂等）
set @c = (select count(*) from information_schema.columns
          where table_schema = database() and table_name = 'vqms_yc_point_map' and column_name = 'point_key');
set @s = if(@c = 0,
  'alter table vqms_yc_point_map add column point_key varchar(64) default null comment ''语义键（管线消费用，唯一：grid_signal_main/avc_onoff/exempt_flag 等；空=资料行不参与管线）'' after point_num, add unique key uk_point_key (point_key)',
  'select 1');
prepare st from @s; execute st; deallocate prepare st;

-- 2) 六语义键回填（sim/JS 派生号；gate_enabled=1 启用）
update vqms_yc_point_map set point_key = 'grid_signal_main', gate_enabled = 1
 where point_num = 511  and point_key is null;
update vqms_yc_point_map set point_key = 'grid_signal_aux', gate_enabled = 1
 where point_num = 512  and point_key is null;
update vqms_yc_point_map set point_key = 'exit_reason_main', gate_enabled = 1
 where point_num = 521  and point_key is null;
update vqms_yc_point_map set point_key = 'exit_reason_aux', gate_enabled = 1
 where point_num = 522  and point_key is null;
update vqms_yc_point_map set point_key = 'exempt_flag', gate_enabled = 1
 where point_num = 501  and point_key is null;

-- 3) AVC 投退 sim 行（3009 撞号占位；真实候选 yx1001 行已存在，现场核对后 UPDATE 本行 point_num）
insert into vqms_yc_point_map (point_num, point_key, point_kind, point_name, point_type, unit, state_1_label, state_0_label, gate_enabled, remark)
select 3009, 'avc_onoff', 'C', 'AVC投退(sim)', 'analog', null, '投入', '退出', 1,
       'sim 占位号：真实库 yc3009=四号机组下闭锁总信号（JS_DATA js109）撞号不同义；真实候选 yx1001（AVC_INFO.AVCStatusYxNum）——现场核对后 UPDATE 本行 point_num=1001'
where not exists (select 1 from vqms_yc_point_map where point_num = 3009);
update vqms_yc_point_map set point_key = 'avc_onoff', gate_enabled = 1
 where point_num = 3009 and point_key is null;

-- 4) 主母线 t0 实时电压点落库（sim 4002/4003；真实候选 yc8/yc14 现场核对后 UPDATE 台账）
update vqms_busbar set realtime_yc_num = 4002 where busbar_num = 0 and realtime_yc_num is null;
update vqms_busbar set realtime_yc_num = 4003 where busbar_num = 1 and realtime_yc_num is null;

-- 5) 判定组主母线号指示点落库（sim 4001；真实候选 yc3 现场核对后 UPDATE 台账）
update vqms_busbar_group set main_indicator_yc_num = 4001 where group_num = 0 and main_indicator_yc_num is null;

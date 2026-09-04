-- 2026-09-04_08 核实单口径原子化（Leo 拍板：全部口径界面可整定，外部确认后改库即生效）：
--  1) ck_locked_rows 放开 tier_threshold 两行钉值（t_econ 锁定保留——窗口口径非核实单项）
--  2) 新增两个口径开关种子：0值坏点拦截、免考复核模式
-- 幂等可重跑。
alter table vqms_judge_param drop check ck_locked_rows;
alter table vqms_judge_param
  add constraint ck_locked_rows check (
    (param_key <> 't_econ' or (param_value <=> 5 and value_min <=> 5 and value_max <=> 5))
  );

update vqms_judge_param set value_min = 1, value_max = 5,
       description = '外部依据待确认（附件6 无 5 分钟分档条款）；值域放开界面可整定（2026-09-04 原子化）'
 where param_key in ('tier_threshold_fast', 'tier_threshold_econ');

insert into vqms_judge_param (param_key, param_value, name, description, value_min, value_max)
select 'zero_badpoint_block_enabled', 1, '0值坏点拦截开关',
       '1=拦截（his_curve_sv high/low任一≤0视为坏点不采信，发现④默认）/ 0=放行采信（核实单§4口径，界面可整定）', 0, 1
where not exists (select 1 from vqms_judge_param where param_key = 'zero_badpoint_block_enabled');

insert into vqms_judge_param (param_key, param_value, name, description, value_min, value_max)
select 'exempt_review_two_level', 0, '免考复核模式',
       '0=单账户自批（默认，2026-09-04拍板）/ 1=两级复核（标注人≠复核人校验恢复，核实单§6口径，界面可整定）', 0, 1
where not exists (select 1 from vqms_judge_param where param_key = 'exempt_review_two_level');

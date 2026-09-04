-- 2026-09-04_09 数据不可用处置策略原子化（1.0 A1/A2/A5 原子 → 界面可整定开关；A3/A4 已由 τ 参数承载）；幂等
insert into vqms_judge_param (param_key, param_value, name, description, value_min, value_max)
select 'undecodable_action', 0, '解码失败处置(A1)', '0=剔除分母INVALID（默认，保守不罚）/ 1=计不合格PENALIZED（倒逼数据质量）；1.0数据不可用策略原子化', 0, 1
where not exists (select 1 from vqms_judge_param where param_key = 'undecodable_action');
insert into vqms_judge_param (param_key, param_value, name, description, value_min, value_max)
select 'window_missing_action', 0, '整窗缺处置(A2)', '0=剔除分母INVALID（默认）/ 1=计不合格PENALIZED', 0, 1
where not exists (select 1 from vqms_judge_param where param_key = 'window_missing_action');
insert into vqms_judge_param (param_key, param_value, name, description, value_min, value_max)
select 'band_inverted_action', 0, 'L>H异常行处置(A2)', '0=该档剔除INVALID（默认，S16口径）/ 1=计不合格PENALIZED', 0, 1
where not exists (select 1 from vqms_judge_param where param_key = 'band_inverted_action');
insert into vqms_judge_param (param_key, param_value, name, description, value_min, value_max)
select 'exempt_flag_missing_action', 0, '免考旗无源处置(A5)', '0=照罚（默认，AUTO_YX链停用走MANUAL/AUTO_DEVICE）/ 1=视为免考（从宽，慎用）', 0, 1
where not exists (select 1 from vqms_judge_param where param_key = 'exempt_flag_missing_action');

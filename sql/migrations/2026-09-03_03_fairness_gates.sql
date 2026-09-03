-- 2026-09-03_03 数据公平性闸门：完整度闸门 τ 参数种子（幂等可重跑）
-- 0 值坏点拦截在代码层（RegulationJudge.sanitizeBand，发现④），无 DDL；
-- τ：completeness 低于该值的档判 INVALID 不硬判（1.0 数据不可用策略 A3/A4 最小口径，0=关闭）。
insert into vqms_judge_param (param_key, param_value, name, description, value_min, value_max)
select 'min_window_completeness_pct', 50, '档窗口最低完整度(%)',
       'completeness低于该值的档判INVALID不硬判（数据公平性：缺数窗不罚电厂；1.0数据不可用策略A3/A4最小口径，0=关闭）', 0, 100
where not exists (select 1 from vqms_judge_param where param_key = 'min_window_completeness_pct');

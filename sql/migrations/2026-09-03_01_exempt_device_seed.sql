-- 2026-09-03_01 免考三源补全：设备 P-Q 曲线种子 + 免考顶满容差参数（幂等，可重执行）
-- 背景：vqms.sql 全量脚本自 Phase 1 落数据后不再重放（破坏性），140 走本增量迁移。

-- P-Q 曲线种子（GEN_01/GEN_02 三点插值蓝本；端点与台账静态额定一致）
insert into vqms_device_pq_limit (device_id, p_kw, q_up_kvar, q_down_kvar, effective_from, remark)
select device_id, v.p_kw, v.q_up, v.q_down, '2026-01-01', '三点插值蓝本（0/150/300MW）；现场实测换版走新 effective_from'
from vqms_reactive_device d
join (
  select 0.000 p_kw, 250000.000 q_up, -150000.000 q_down
  union all select 150000.000, 225000.000, -120000.000
  union all select 300000.000, 200000.000, -100000.000
) v
where d.device_code in ('GEN_01', 'GEN_02')
  and not exists (select 1 from vqms_device_pq_limit l
                  where l.device_id = d.device_id and l.effective_from = '2026-01-01' and l.p_kw = v.p_kw);

-- 免考顶满容差（附件6§三无 ε 规定，现场整定）
insert into vqms_judge_param (param_key, param_value, name, description, value_min, value_max)
select 'exempt_q_tol_kvar', 2000, '设备级免考顶满容差(kvar)', '设备Q距极限≤该值视为顶满（附件6§三无ε规定，现场整定）', 0, 100000
where not exists (select 1 from vqms_judge_param where param_key = 'exempt_q_tol_kvar');

-- 2026-09-03_04 种子描述勘误（幂等可重跑）：tier_threshold_fast/econ 的"附件6 政策值锁定"表述不实——
-- 附件6 原文无 5 分钟分档条款（VQMS 细化口径，外部依据待确认）；引用与政策原文不符本身是合规瑕疵。
update vqms_judge_param
   set description = '外部依据待确认（附件6 无 5 分钟分档条款）'
 where param_key in ('tier_threshold_fast', 'tier_threshold_econ');

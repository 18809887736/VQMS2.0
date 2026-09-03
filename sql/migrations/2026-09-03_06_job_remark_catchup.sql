-- 2026-09-03_06 工程加固：sys_job 备注更新（缺口补算语义）；幂等可重跑
update sys_job
   set remark = '每日 03:00 缺口补算到昨日全链（停机/misfire 自愈，单次上限 92 天超限报错提示手动分批；幂等可重跑；空数据日自动跳过不记账）'
 where invoke_target = 'vqmsStatsTask.recomputeYesterday()';

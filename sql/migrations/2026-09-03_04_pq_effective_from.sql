-- 2026-09-03_04 P-Q 曲线蓝本生效日提前 2026-01-01 → 2020-01-01（幂等）
-- 原因：回放区间早于 2026-01-01 时曲线版本不生效，回退静态额定 200000，
-- 使"留余力"场景（Q=220000 距插值极限 237500 差 17500）在静态口径下被误判顶满（回放 A6 断言抓出）。
-- 蓝本三点插值适用于机组全生命周期；现场实测换版仍走新 effective_from（区间化不变）。
update vqms_device_pq_limit
   set effective_from = '2020-01-01',
       remark = '三点插值蓝本（0/150/300MW），自最早覆盖（历史回放同口径）；现场实测换版走新 effective_from'
 where effective_from = '2026-01-01'
   and not exists (select 1 from (select 1 from vqms_device_pq_limit
                                  where effective_from = '2020-01-01') x);

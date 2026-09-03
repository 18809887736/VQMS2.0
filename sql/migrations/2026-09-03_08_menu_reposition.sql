-- 2026-09-03_08 菜单归位：2107(母线组)/2113(主母线台账)/2119(无功设备台账) 误挂若依"系统工具"(parent=3)
-- 统一归位 VQMS管理(2100)（与 vqms_menu.sql 全量沉淀版一致）；幂等可重跑
update sys_menu set parent_id = 2100 where menu_id in (2107, 2113, 2119) and parent_id = 3;

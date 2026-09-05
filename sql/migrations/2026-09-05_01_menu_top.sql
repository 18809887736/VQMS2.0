-- 2026-09-05_01 VQMS管理 置顶（系统管理之上）+ 换图标（与系统监控 monitor 重复 → dashboard）；幂等
update sys_menu set order_num = 0, icon = 'dashboard' where menu_id = 2100;

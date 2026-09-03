-- 2026-09-03_07 报表页菜单：投运率/调节合格率 D/M/Y 报表 + Excel 导出（菜单 ID 2169）；幂等可重跑
delete from sys_role_menu where menu_id = 2169;
delete from sys_menu where menu_id = 2169;

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values (2169, '考核报表', 2100, 10, 'reports', 'vqms/reports/index', '', 1, 0, 'C', '0', '0', 'vqms:judge:run', 'documentation', 'admin', sysdate(), '投运率/调节合格率 D/M/Y 报表 + Excel 导出');

insert ignore into sys_role_menu (role_id, menu_id) values (2, 2169);

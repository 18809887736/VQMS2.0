-- 2026-09-04_04 第26条对账页菜单（ID 2170 接 vqms 菜单段）；幂等可重跑
delete from sys_role_menu where menu_id = 2170;
delete from sys_menu where menu_id = 2170;

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values (2170, '第26条对账', 2100, 11, 'art26', 'vqms/art26/index', '', 1, 0, 'C', '0', '0', 'vqms:art26:list', 'example', 'admin', sysdate(), '季度电压曲线登记/导入 + AVC 闭环免考三桶对账');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values (2171, '对账查询', 2170, 1, '', '', '', 1, 0, 'F', '0', '0', 'vqms:art26:list', '#', 'admin', sysdate(), ''),
       (2172, '曲线登记', 2170, 2, '', '', '', 1, 0, 'F', '0', '0', 'vqms:art26:add', '#', 'admin', sysdate(), ''),
       (2173, '曲线修改', 2170, 3, '', '', '', 1, 0, 'F', '0', '0', 'vqms:art26:edit', '#', 'admin', sysdate(), ''),
       (2174, '曲线删除', 2170, 4, '', '', '', 1, 0, 'F', '0', '0', 'vqms:art26:remove', '#', 'admin', sysdate(), ''),
       (2175, '对账导出', 2170, 5, '', '', '', 1, 0, 'F', '0', '0', 'vqms:art26:export', '#', 'admin', sysdate(), '');

insert ignore into sys_role_menu (role_id, menu_id) values (2, 2170), (2, 2171), (2, 2172), (2, 2173), (2, 2174), (2, 2175);

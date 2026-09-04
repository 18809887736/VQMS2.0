-- 2026-09-04_10 配置库整定界面菜单（ID 2182-2183）；幂等可重跑
delete from sys_role_menu where menu_id between 2182 and 2183;
delete from sys_menu where menu_id between 2182 and 2183;

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values (2182, '配置库整定', 2100, 13, 'tuning', 'vqms/tuning/index', '', 1, 0, 'C', '0', '0', 'vqms:tuning:run', 'upload', 'admin', sysdate(), '上传对端 AVC 配置库 diff 预览 + 确认整定');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values (2183, '整定执行', 2182, 1, '', '', '', 1, 0, 'F', '0', '0', 'vqms:tuning:run', '#', 'admin', sysdate(), '');

insert ignore into sys_role_menu (role_id, menu_id) values (2, 2182), (2, 2183);

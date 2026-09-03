-- VQMS 考核看板菜单（挂 VQMS管理 下，复用 vqms:judge:run 权限；幂等可重执行）
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '考核看板', 2100, 0, 'dashboard', 'vqms/dashboard/index', 1, 0, 'C', '0', '0', 'vqms:judge:run', 'dashboard', 'admin', sysdate(), 'AVC 考核看板（调节合格率+投运率）'
where not exists (select 1 from sys_menu where component = 'vqms/dashboard/index');

-- role2 授权
insert into sys_role_menu (role_id, menu_id)
select 2, menu_id from sys_menu where component = 'vqms/dashboard/index'
on duplicate key update role_id = role_id;

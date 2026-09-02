-- 菜单 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('无功设备台账', '3', '1', 'device', 'vqms/device/index', 1, 0, 'C', '0', '0', 'vqms:device:list', '#', 'admin', sysdate(), '', null, '无功设备台账菜单');

-- 按钮父菜单ID
SELECT @parentId := LAST_INSERT_ID();

-- 按钮 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('无功设备台账查询', @parentId, '1',  '#', '', 1, 0, 'F', '0', '0', 'vqms:device:query',        '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('无功设备台账新增', @parentId, '2',  '#', '', 1, 0, 'F', '0', '0', 'vqms:device:add',          '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('无功设备台账修改', @parentId, '3',  '#', '', 1, 0, 'F', '0', '0', 'vqms:device:edit',         '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('无功设备台账删除', @parentId, '4',  '#', '', 1, 0, 'F', '0', '0', 'vqms:device:remove',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('无功设备台账导出', @parentId, '5',  '#', '', 1, 0, 'F', '0', '0', 'vqms:device:export',       '#', 'admin', sysdate(), '', null, '');
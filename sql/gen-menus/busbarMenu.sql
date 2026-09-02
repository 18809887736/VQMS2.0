-- 菜单 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('主母线台账', '3', '1', 'busbar', 'vqms/busbar/index', 1, 0, 'C', '0', '0', 'vqms:busbar:list', '#', 'admin', sysdate(), '', null, '主母线台账菜单');

-- 按钮父菜单ID
SELECT @parentId := LAST_INSERT_ID();

-- 按钮 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('主母线台账查询', @parentId, '1',  '#', '', 1, 0, 'F', '0', '0', 'vqms:busbar:query',        '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('主母线台账新增', @parentId, '2',  '#', '', 1, 0, 'F', '0', '0', 'vqms:busbar:add',          '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('主母线台账修改', @parentId, '3',  '#', '', 1, 0, 'F', '0', '0', 'vqms:busbar:edit',         '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('主母线台账删除', @parentId, '4',  '#', '', 1, 0, 'F', '0', '0', 'vqms:busbar:remove',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('主母线台账导出', @parentId, '5',  '#', '', 1, 0, 'F', '0', '0', 'vqms:busbar:export',       '#', 'admin', sysdate(), '', null, '');
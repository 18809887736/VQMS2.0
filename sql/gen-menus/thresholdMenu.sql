-- 菜单 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('母线电压阈值', '2100', '1', 'threshold', 'vqms/threshold/index', 1, 0, 'C', '0', '0', 'vqms:threshold:list', '#', 'admin', sysdate(), '', null, '母线电压阈值菜单');

-- 按钮父菜单ID
SELECT @parentId := LAST_INSERT_ID();

-- 按钮 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('母线电压阈值查询', @parentId, '1',  '#', '', 1, 0, 'F', '0', '0', 'vqms:threshold:query',        '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('母线电压阈值新增', @parentId, '2',  '#', '', 1, 0, 'F', '0', '0', 'vqms:threshold:add',          '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('母线电压阈值修改', @parentId, '3',  '#', '', 1, 0, 'F', '0', '0', 'vqms:threshold:edit',         '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('母线电压阈值删除', @parentId, '4',  '#', '', 1, 0, 'F', '0', '0', 'vqms:threshold:remove',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('母线电压阈值导出', @parentId, '5',  '#', '', 1, 0, 'F', '0', '0', 'vqms:threshold:export',       '#', 'admin', sysdate(), '', null, '');
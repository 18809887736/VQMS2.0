-- 菜单 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('判定整定参数', '2100', '1', 'judgeParam', 'vqms/judgeParam/index', 1, 0, 'C', '0', '0', 'vqms:judgeParam:list', '#', 'admin', sysdate(), '', null, '判定整定参数菜单');

-- 按钮父菜单ID
SELECT @parentId := LAST_INSERT_ID();

-- 按钮 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('判定整定参数查询', @parentId, '1',  '#', '', 1, 0, 'F', '0', '0', 'vqms:judgeParam:query',        '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('判定整定参数新增', @parentId, '2',  '#', '', 1, 0, 'F', '0', '0', 'vqms:judgeParam:add',          '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('判定整定参数修改', @parentId, '3',  '#', '', 1, 0, 'F', '0', '0', 'vqms:judgeParam:edit',         '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('判定整定参数删除', @parentId, '4',  '#', '', 1, 0, 'F', '0', '0', 'vqms:judgeParam:remove',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('判定整定参数导出', @parentId, '5',  '#', '', 1, 0, 'F', '0', '0', 'vqms:judgeParam:export',       '#', 'admin', sysdate(), '', null, '');
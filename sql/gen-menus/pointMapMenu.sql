-- 菜单 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('点号语义注册', '2100', '1', 'pointMap', 'vqms/pointMap/index', 1, 0, 'C', '0', '0', 'vqms:pointMap:list', '#', 'admin', sysdate(), '', null, '点号语义注册菜单');

-- 按钮父菜单ID
SELECT @parentId := LAST_INSERT_ID();

-- 按钮 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('点号语义注册查询', @parentId, '1',  '#', '', 1, 0, 'F', '0', '0', 'vqms:pointMap:query',        '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('点号语义注册新增', @parentId, '2',  '#', '', 1, 0, 'F', '0', '0', 'vqms:pointMap:add',          '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('点号语义注册修改', @parentId, '3',  '#', '', 1, 0, 'F', '0', '0', 'vqms:pointMap:edit',         '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('点号语义注册删除', @parentId, '4',  '#', '', 1, 0, 'F', '0', '0', 'vqms:pointMap:remove',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('点号语义注册导出', @parentId, '5',  '#', '', 1, 0, 'F', '0', '0', 'vqms:pointMap:export',       '#', 'admin', sysdate(), '', null, '');
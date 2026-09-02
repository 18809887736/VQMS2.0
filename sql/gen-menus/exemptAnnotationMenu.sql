-- 菜单 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('调节免考标注', '2100', '1', 'exemptAnnotation', 'vqms/exemptAnnotation/index', 1, 0, 'C', '0', '0', 'vqms:exemptAnnotation:list', '#', 'admin', sysdate(), '', null, '调节免考标注菜单');

-- 按钮父菜单ID
SELECT @parentId := LAST_INSERT_ID();

-- 按钮 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('调节免考标注查询', @parentId, '1',  '#', '', 1, 0, 'F', '0', '0', 'vqms:exemptAnnotation:query',        '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('调节免考标注新增', @parentId, '2',  '#', '', 1, 0, 'F', '0', '0', 'vqms:exemptAnnotation:add',          '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('调节免考标注修改', @parentId, '3',  '#', '', 1, 0, 'F', '0', '0', 'vqms:exemptAnnotation:edit',         '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('调节免考标注删除', @parentId, '4',  '#', '', 1, 0, 'F', '0', '0', 'vqms:exemptAnnotation:remove',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('调节免考标注导出', @parentId, '5',  '#', '', 1, 0, 'F', '0', '0', 'vqms:exemptAnnotation:export',       '#', 'admin', sysdate(), '', null, '');
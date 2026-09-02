-- 菜单 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('设备P-Q极限曲线', '2100', '1', 'devicePqLimit', 'vqms/devicePqLimit/index', 1, 0, 'C', '0', '0', 'vqms:devicePqLimit:list', '#', 'admin', sysdate(), '', null, '设备P-Q极限曲线菜单');

-- 按钮父菜单ID
SELECT @parentId := LAST_INSERT_ID();

-- 按钮 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('设备P-Q极限曲线查询', @parentId, '1',  '#', '', 1, 0, 'F', '0', '0', 'vqms:devicePqLimit:query',        '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('设备P-Q极限曲线新增', @parentId, '2',  '#', '', 1, 0, 'F', '0', '0', 'vqms:devicePqLimit:add',          '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('设备P-Q极限曲线修改', @parentId, '3',  '#', '', 1, 0, 'F', '0', '0', 'vqms:devicePqLimit:edit',         '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('设备P-Q极限曲线删除', @parentId, '4',  '#', '', 1, 0, 'F', '0', '0', 'vqms:devicePqLimit:remove',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('设备P-Q极限曲线导出', @parentId, '5',  '#', '', 1, 0, 'F', '0', '0', 'vqms:devicePqLimit:export',       '#', 'admin', sysdate(), '', null, '');
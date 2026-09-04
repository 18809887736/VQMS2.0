-- 2026-09-04_06 第27条对账页菜单（ID 2176-2181 接 vqms 菜单段）；幂等可重跑
delete from sys_role_menu where menu_id between 2176 and 2181;
delete from sys_menu where menu_id between 2176 and 2181;

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values (2176, '第27条对账', 2100, 12, 'art27', 'vqms/art27/index', '', 1, 0, 'C', '0', '0', 'vqms:art27:list', 'monitor', 'admin', sysdate(), '动态无功补偿装置台账 + 可用率/罚分月度对账');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values (2177, '对账查询', 2176, 1, '', '', '', 1, 0, 'F', '0', '0', 'vqms:art27:list', '#', 'admin', sysdate(), ''),
       (2178, '台账/登记', 2176, 2, '', '', '', 1, 0, 'F', '0', '0', 'vqms:art27:add', '#', 'admin', sysdate(), ''),
       (2179, '修改', 2176, 3, '', '', '', 1, 0, 'F', '0', '0', 'vqms:art27:edit', '#', 'admin', sysdate(), ''),
       (2180, '删除', 2176, 4, '', '', '', 1, 0, 'F', '0', '0', 'vqms:art27:remove', '#', 'admin', sysdate(), ''),
       (2181, '对账导出', 2176, 5, '', '', '', 1, 0, 'F', '0', '0', 'vqms:art27:export', '#', 'admin', sysdate(), '');

insert ignore into sys_role_menu (role_id, menu_id) values (2, 2176), (2, 2177), (2, 2178), (2, 2179), (2, 2180), (2, 2181);

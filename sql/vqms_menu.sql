-- ============================================================
-- VQMS2.0 菜单与权限种子（vqms_menu.sql，2026-09-02）
--
-- 配套 ruoyi-vqms 模块首条垂直切片：并网主体台账（/vqms/entity）
-- 执行顺序：vqms.sql 之后（须在 ry_*.sql 建好 sys_menu 后执行）
-- 幂等：先 delete 再 insert，可重复执行；菜单 ID 使用 2100+ 专段
-- ============================================================

-- 清理旧种子（菜单 + 角色授权）
delete from sys_role_menu where menu_id between 2100 and 2199;
delete from sys_menu where menu_id between 2100 and 2199;

-- 一级目录：VQMS 管理
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values (2100, 'VQMS管理', 0, 5, 'vqms', null, '', 1, 0, 'M', '0', '0', '', 'monitor', 'admin', sysdate(), 'VQMS 电压质量监测与考核');

-- 二级菜单：并网主体台账
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values (2101, '并网主体', 2100, 1, 'entity', 'vqms/entity/index', '', 1, 0, 'C', '0', '0', 'vqms:entity:list', 'peoples', 'admin', sysdate(), '并网主体台账（考核基数载体）');

-- 按钮权限
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values (2102, '主体查询', 2101, 1, '', '', '', 1, 0, 'F', '0', '0', 'vqms:entity:query',  '#', 'admin', sysdate(), ''),
       (2103, '主体新增', 2101, 2, '', '', '', 1, 0, 'F', '0', '0', 'vqms:entity:add',    '#', 'admin', sysdate(), ''),
       (2104, '主体修改', 2101, 3, '', '', '', 1, 0, 'F', '0', '0', 'vqms:entity:edit',   '#', 'admin', sysdate(), ''),
       (2105, '主体删除', 2101, 4, '', '', '', 1, 0, 'F', '0', '0', 'vqms:entity:remove', '#', 'admin', sysdate(), ''),
       (2106, '主体导出', 2101, 5, '', '', '', 1, 0, 'F', '0', '0', 'vqms:entity:export', '#', 'admin', sysdate(), '');

-- 授予普通角色（role_id=2；role_id=1 超管天然全权限）
insert into sys_role_menu (role_id, menu_id)
select 2, menu_id from sys_menu where menu_id between 2100 and 2106;

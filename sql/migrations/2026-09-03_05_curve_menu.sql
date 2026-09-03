-- 2026-09-03_05 免考人工运营链：电压曲线查询菜单（复核员看指令时段电压带；菜单 ID 2168 接现有 vqms 菜单段）
-- 幂等可重跑。
delete from sys_role_menu where menu_id = 2168;
delete from sys_menu where menu_id = 2168;

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values (2168, '电压曲线', 2100, 9, 'curve', 'vqms/curve/index', '', 1, 0, 'C', '0', '0', 'vqms:curve:list', 'chart', 'admin', sysdate(), '母线电压逐分钟曲线（免考复核支撑）');

-- 普通角色授权（role_id=2；超管天然全权限；role_menu 联合主键，INSERT IGNORE 幂等）
insert ignore into sys_role_menu (role_id, menu_id) values (2, 2168);

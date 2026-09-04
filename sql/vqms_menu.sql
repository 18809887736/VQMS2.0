-- ============================================================
-- VQMS2.0 菜单与权限种子（vqms_menu.sql 全量版，2026-09-04 沉淀）
--
-- 覆盖 2100-2199 专段全部 VQMS 菜单：目录 + 16 个功能页（台账/标注/参数/看板/曲线/报表）+ 按钮权限
-- 执行顺序：vqms.sql 之后（须在 ry_*.sql 建好 sys_menu 后执行）
-- 幂等：先 delete 再 insert，可重复执行（重放会重置菜单为脚本版——现场在 UI 上改过的菜单顺序/图标会回退）
-- 菜单 ID 2100-2199 为 VQMS 专段，新增菜单沿用此段
-- ============================================================

-- 清理旧种子（菜单 + 角色授权）
delete from sys_role_menu where menu_id between 2100 and 2199;
delete from sys_menu where menu_id between 2100 and 2199;

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) values
  (2100, 'VQMS管理', 0, 5, 'vqms', NULL, NULL, 1, 0, 'M', '0', '0', NULL, 'monitor', 'admin', sysdate(), 'VQMS 电压质量监测与考核'),
  (2101, '并网主体', 2100, 1, 'entity', 'vqms/entity/index', NULL, 1, 0, 'C', '0', '0', 'vqms:entity:list', 'peoples', 'admin', sysdate(), '并网主体台账（考核基数载体）'),
  (2102, '主体查询', 2101, 1, NULL, NULL, NULL, 1, 0, 'F', '0', '0', 'vqms:entity:query', '#', 'admin', sysdate(), NULL),
  (2103, '主体新增', 2101, 2, NULL, NULL, NULL, 1, 0, 'F', '0', '0', 'vqms:entity:add', '#', 'admin', sysdate(), NULL),
  (2104, '主体修改', 2101, 3, NULL, NULL, NULL, 1, 0, 'F', '0', '0', 'vqms:entity:edit', '#', 'admin', sysdate(), NULL),
  (2105, '主体删除', 2101, 4, NULL, NULL, NULL, 1, 0, 'F', '0', '0', 'vqms:entity:remove', '#', 'admin', sysdate(), NULL),
  (2106, '主体导出', 2101, 5, NULL, NULL, NULL, 1, 0, 'F', '0', '0', 'vqms:entity:export', '#', 'admin', sysdate(), NULL),
  (2107, '母线组', 2100, 1, 'busbarGroup', 'vqms/busbarGroup/index', NULL, 1, 0, 'C', '0', '0', 'vqms:busbarGroup:list', '#', 'admin', sysdate(), '母线组菜单'),
  (2108, '母线组查询', 2107, 1, '#', NULL, NULL, 1, 0, 'F', '0', '0', 'vqms:busbarGroup:query', '#', 'admin', sysdate(), NULL),
  (2109, '母线组新增', 2107, 2, '#', NULL, NULL, 1, 0, 'F', '0', '0', 'vqms:busbarGroup:add', '#', 'admin', sysdate(), NULL),
  (2110, '母线组修改', 2107, 3, '#', NULL, NULL, 1, 0, 'F', '0', '0', 'vqms:busbarGroup:edit', '#', 'admin', sysdate(), NULL),
  (2111, '母线组删除', 2107, 4, '#', NULL, NULL, 1, 0, 'F', '0', '0', 'vqms:busbarGroup:remove', '#', 'admin', sysdate(), NULL),
  (2112, '母线组导出', 2107, 5, '#', NULL, NULL, 1, 0, 'F', '0', '0', 'vqms:busbarGroup:export', '#', 'admin', sysdate(), NULL),
  (2113, '主母线台账', 2100, 1, 'busbar', 'vqms/busbar/index', NULL, 1, 0, 'C', '0', '0', 'vqms:busbar:list', '#', 'admin', sysdate(), '主母线台账菜单'),
  (2114, '主母线台账查询', 2113, 1, '#', NULL, NULL, 1, 0, 'F', '0', '0', 'vqms:busbar:query', '#', 'admin', sysdate(), NULL),
  (2115, '主母线台账新增', 2113, 2, '#', NULL, NULL, 1, 0, 'F', '0', '0', 'vqms:busbar:add', '#', 'admin', sysdate(), NULL),
  (2116, '主母线台账修改', 2113, 3, '#', NULL, NULL, 1, 0, 'F', '0', '0', 'vqms:busbar:edit', '#', 'admin', sysdate(), NULL),
  (2117, '主母线台账删除', 2113, 4, '#', NULL, NULL, 1, 0, 'F', '0', '0', 'vqms:busbar:remove', '#', 'admin', sysdate(), NULL),
  (2118, '主母线台账导出', 2113, 5, '#', NULL, NULL, 1, 0, 'F', '0', '0', 'vqms:busbar:export', '#', 'admin', sysdate(), NULL),
  (2119, '无功设备台账', 2100, 1, 'device', 'vqms/device/index', NULL, 1, 0, 'C', '0', '0', 'vqms:device:list', '#', 'admin', sysdate(), '无功设备台账菜单'),
  (2120, '无功设备台账查询', 2119, 1, '#', NULL, NULL, 1, 0, 'F', '0', '0', 'vqms:device:query', '#', 'admin', sysdate(), NULL),
  (2121, '无功设备台账新增', 2119, 2, '#', NULL, NULL, 1, 0, 'F', '0', '0', 'vqms:device:add', '#', 'admin', sysdate(), NULL),
  (2122, '无功设备台账修改', 2119, 3, '#', NULL, NULL, 1, 0, 'F', '0', '0', 'vqms:device:edit', '#', 'admin', sysdate(), NULL),
  (2123, '无功设备台账删除', 2119, 4, '#', NULL, NULL, 1, 0, 'F', '0', '0', 'vqms:device:remove', '#', 'admin', sysdate(), NULL),
  (2124, '无功设备台账导出', 2119, 5, '#', NULL, NULL, 1, 0, 'F', '0', '0', 'vqms:device:export', '#', 'admin', sysdate(), NULL),
  (2125, '设备P-Q极限曲线', 2100, 1, 'devicePqLimit', 'vqms/devicePqLimit/index', NULL, 1, 0, 'C', '0', '0', 'vqms:devicePqLimit:list', '#', 'admin', sysdate(), '设备P-Q极限曲线菜单'),
  (2126, '设备P-Q极限曲线查询', 2125, 1, '#', NULL, NULL, 1, 0, 'F', '0', '0', 'vqms:devicePqLimit:query', '#', 'admin', sysdate(), NULL),
  (2127, '设备P-Q极限曲线新增', 2125, 2, '#', NULL, NULL, 1, 0, 'F', '0', '0', 'vqms:devicePqLimit:add', '#', 'admin', sysdate(), NULL),
  (2128, '设备P-Q极限曲线修改', 2125, 3, '#', NULL, NULL, 1, 0, 'F', '0', '0', 'vqms:devicePqLimit:edit', '#', 'admin', sysdate(), NULL),
  (2129, '设备P-Q极限曲线删除', 2125, 4, '#', NULL, NULL, 1, 0, 'F', '0', '0', 'vqms:devicePqLimit:remove', '#', 'admin', sysdate(), NULL),
  (2130, '设备P-Q极限曲线导出', 2125, 5, '#', NULL, NULL, 1, 0, 'F', '0', '0', 'vqms:devicePqLimit:export', '#', 'admin', sysdate(), NULL),
  (2131, '调节免考标注', 2100, 1, 'exemptAnnotation', 'vqms/exemptAnnotation/index', NULL, 1, 0, 'C', '0', '0', 'vqms:exemptAnnotation:list', '#', 'admin', sysdate(), '调节免考标注菜单'),
  (2132, '调节免考标注查询', 2131, 1, '#', NULL, NULL, 1, 0, 'F', '0', '0', 'vqms:exemptAnnotation:query', '#', 'admin', sysdate(), NULL),
  (2133, '调节免考标注新增', 2131, 2, '#', NULL, NULL, 1, 0, 'F', '0', '0', 'vqms:exemptAnnotation:add', '#', 'admin', sysdate(), NULL),
  (2134, '调节免考标注修改', 2131, 3, '#', NULL, NULL, 1, 0, 'F', '0', '0', 'vqms:exemptAnnotation:edit', '#', 'admin', sysdate(), NULL),
  (2135, '调节免考标注删除', 2131, 4, '#', NULL, NULL, 1, 0, 'F', '0', '0', 'vqms:exemptAnnotation:remove', '#', 'admin', sysdate(), NULL),
  (2136, '调节免考标注导出', 2131, 5, '#', NULL, NULL, 1, 0, 'F', '0', '0', 'vqms:exemptAnnotation:export', '#', 'admin', sysdate(), NULL),
  (2137, 'AVC退出原因标注', 2100, 1, 'exitAnnotation', 'vqms/exitAnnotation/index', NULL, 1, 0, 'C', '0', '0', 'vqms:exitAnnotation:list', '#', 'admin', sysdate(), 'AVC退出原因标注菜单'),
  (2138, 'AVC退出原因标注查询', 2137, 1, '#', NULL, NULL, 1, 0, 'F', '0', '0', 'vqms:exitAnnotation:query', '#', 'admin', sysdate(), NULL),
  (2139, 'AVC退出原因标注新增', 2137, 2, '#', NULL, NULL, 1, 0, 'F', '0', '0', 'vqms:exitAnnotation:add', '#', 'admin', sysdate(), NULL),
  (2140, 'AVC退出原因标注修改', 2137, 3, '#', NULL, NULL, 1, 0, 'F', '0', '0', 'vqms:exitAnnotation:edit', '#', 'admin', sysdate(), NULL),
  (2141, 'AVC退出原因标注删除', 2137, 4, '#', NULL, NULL, 1, 0, 'F', '0', '0', 'vqms:exitAnnotation:remove', '#', 'admin', sysdate(), NULL),
  (2142, 'AVC退出原因标注导出', 2137, 5, '#', NULL, NULL, 1, 0, 'F', '0', '0', 'vqms:exitAnnotation:export', '#', 'admin', sysdate(), NULL),
  (2143, '判定整定参数', 2100, 1, 'judgeParam', 'vqms/judgeParam/index', NULL, 1, 0, 'C', '0', '0', 'vqms:judgeParam:list', '#', 'admin', sysdate(), '判定整定参数菜单'),
  (2144, '判定整定参数查询', 2143, 1, '#', NULL, NULL, 1, 0, 'F', '0', '0', 'vqms:judgeParam:query', '#', 'admin', sysdate(), NULL),
  (2145, '判定整定参数新增', 2143, 2, '#', NULL, NULL, 1, 0, 'F', '0', '0', 'vqms:judgeParam:add', '#', 'admin', sysdate(), NULL),
  (2146, '判定整定参数修改', 2143, 3, '#', NULL, NULL, 1, 0, 'F', '0', '0', 'vqms:judgeParam:edit', '#', 'admin', sysdate(), NULL),
  (2147, '判定整定参数删除', 2143, 4, '#', NULL, NULL, 1, 0, 'F', '0', '0', 'vqms:judgeParam:remove', '#', 'admin', sysdate(), NULL),
  (2148, '判定整定参数导出', 2143, 5, '#', NULL, NULL, 1, 0, 'F', '0', '0', 'vqms:judgeParam:export', '#', 'admin', sysdate(), NULL),
  (2149, '点号语义注册', 2100, 1, 'pointMap', 'vqms/pointMap/index', NULL, 1, 0, 'C', '0', '0', 'vqms:pointMap:list', '#', 'admin', sysdate(), '点号语义注册菜单'),
  (2150, '点号语义注册查询', 2149, 1, '#', NULL, NULL, 1, 0, 'F', '0', '0', 'vqms:pointMap:query', '#', 'admin', sysdate(), NULL),
  (2151, '点号语义注册新增', 2149, 2, '#', NULL, NULL, 1, 0, 'F', '0', '0', 'vqms:pointMap:add', '#', 'admin', sysdate(), NULL),
  (2152, '点号语义注册修改', 2149, 3, '#', NULL, NULL, 1, 0, 'F', '0', '0', 'vqms:pointMap:edit', '#', 'admin', sysdate(), NULL),
  (2153, '点号语义注册删除', 2149, 4, '#', NULL, NULL, 1, 0, 'F', '0', '0', 'vqms:pointMap:remove', '#', 'admin', sysdate(), NULL),
  (2154, '点号语义注册导出', 2149, 5, '#', NULL, NULL, 1, 0, 'F', '0', '0', 'vqms:pointMap:export', '#', 'admin', sysdate(), NULL),
  (2155, '数据不可用策略参数', 2100, 1, 'policyParam', 'vqms/policyParam/index', NULL, 1, 0, 'C', '0', '0', 'vqms:policyParam:list', '#', 'admin', sysdate(), '数据不可用策略参数菜单'),
  (2156, '数据不可用策略参数查询', 2155, 1, '#', NULL, NULL, 1, 0, 'F', '0', '0', 'vqms:policyParam:query', '#', 'admin', sysdate(), NULL),
  (2157, '数据不可用策略参数新增', 2155, 2, '#', NULL, NULL, 1, 0, 'F', '0', '0', 'vqms:policyParam:add', '#', 'admin', sysdate(), NULL),
  (2158, '数据不可用策略参数修改', 2155, 3, '#', NULL, NULL, 1, 0, 'F', '0', '0', 'vqms:policyParam:edit', '#', 'admin', sysdate(), NULL),
  (2159, '数据不可用策略参数删除', 2155, 4, '#', NULL, NULL, 1, 0, 'F', '0', '0', 'vqms:policyParam:remove', '#', 'admin', sysdate(), NULL),
  (2160, '数据不可用策略参数导出', 2155, 5, '#', NULL, NULL, 1, 0, 'F', '0', '0', 'vqms:policyParam:export', '#', 'admin', sysdate(), NULL),
  (2161, '母线电压阈值', 2100, 1, 'threshold', 'vqms/threshold/index', NULL, 1, 0, 'C', '0', '0', 'vqms:threshold:list', '#', 'admin', sysdate(), '母线电压阈值菜单'),
  (2162, '母线电压阈值查询', 2161, 1, '#', NULL, NULL, 1, 0, 'F', '0', '0', 'vqms:threshold:query', '#', 'admin', sysdate(), NULL),
  (2163, '母线电压阈值新增', 2161, 2, '#', NULL, NULL, 1, 0, 'F', '0', '0', 'vqms:threshold:add', '#', 'admin', sysdate(), NULL),
  (2164, '母线电压阈值修改', 2161, 3, '#', NULL, NULL, 1, 0, 'F', '0', '0', 'vqms:threshold:edit', '#', 'admin', sysdate(), NULL),
  (2165, '母线电压阈值删除', 2161, 4, '#', NULL, NULL, 1, 0, 'F', '0', '0', 'vqms:threshold:remove', '#', 'admin', sysdate(), NULL),
  (2166, '母线电压阈值导出', 2161, 5, '#', NULL, NULL, 1, 0, 'F', '0', '0', 'vqms:threshold:export', '#', 'admin', sysdate(), NULL),
  (2167, '考核看板', 2100, 0, 'dashboard', 'vqms/dashboard/index', NULL, 1, 0, 'C', '0', '0', 'vqms:judge:run', 'dashboard', 'admin', sysdate(), 'AVC 考核看板（调节合格率+投运率）'),
  (2168, '电压曲线', 2100, 9, 'curve', 'vqms/curve/index', NULL, 1, 0, 'C', '0', '0', 'vqms:curve:list', 'chart', 'admin', sysdate(), '母线电压逐分钟曲线（免考复核支撑）'),
  (2169, '考核报表', 2100, 10, 'reports', 'vqms/reports/index', NULL, 1, 0, 'C', '0', '0', 'vqms:judge:run', 'documentation', 'admin', sysdate(), '投运率/调节合格率 D/M/Y 报表 + Excel 导出');

-- 授予普通角色（role_id=2；role_id=1 超管天然全权限）
insert into sys_role_menu (role_id, menu_id)
select 2, menu_id from sys_menu where menu_id between 2100 and 2199;

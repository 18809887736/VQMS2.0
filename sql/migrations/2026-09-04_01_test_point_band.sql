-- 2026-09-04_01 测试号段统一 4000+（Leo 拍板：测试开发阶段信号点全部 VQMS 自定）
-- 根治撞号：旧占位 3009 与真实库"四号机组下闭锁总信号"撞号不同义；511/521/501 等避开真实库号段。
-- 幂等：带旧号条件，换过即 no-op。资料行（真实候选 3/8/14/216/317/1001 等）不动。
update vqms_yc_point_map set point_num = 4005, point_name = '并网编码·正母单元' where point_key = 'grid_signal_main' and point_num = 511;
update vqms_yc_point_map set point_num = 4006, point_name = '并网编码·副母单元' where point_key = 'grid_signal_aux'  and point_num = 512;
update vqms_yc_point_map set point_num = 4007, point_name = 'AVC投退(测试)',    point_kind = 'C', point_type = 'analog', state_1_label = '投入', state_0_label = '退出',
       remark = '测试号段 4000+（Leo 2026-09-04 拍板：测试开发阶段全部自定）；真实候选 yx1001（AVC_INFO.AVCStatusYxNum）——现场接入时 UPDATE 本行换号'
 where point_key = 'avc_onoff' and point_num = 3009;
update vqms_yc_point_map set point_num = 4008, point_name = 'AVC退出原因·正母' where point_key = 'exit_reason_main' and point_num = 521;
update vqms_yc_point_map set point_num = 4009, point_name = 'AVC退出原因·副母' where point_key = 'exit_reason_aux'  and point_num = 522;
update vqms_yc_point_map set point_num = 4010, point_name = '免考旗(无源)'    where point_key = 'exempt_flag'      and point_num = 501;

-- 设备台账 P/Q 点号换 4000+ 段
update vqms_reactive_device set q_yc_num = 4012, p_yc_num = 4011 where device_code = 'GEN_01' and q_yc_num = 217;
update vqms_reactive_device set q_yc_num = 4014, p_yc_num = 4013 where device_code = 'GEN_02' and q_yc_num = 317;

-- 2026-09-04_03 第26条对账能力：季度母线电压考核曲线表（新表，幂等）
create table if not exists vqms_art26_curve (
  curve_id     bigint       not null auto_increment comment '主键',
  busbar_num   bigint       not null                comment '考核母线（对齐 his_curve_sv.busbar_num）',
  quarter      varchar(8)   not null                comment '季度标签（如 2026Q1）',
  period_start datetime     not null                comment '时段起（含）',
  period_end   datetime     not null                comment '时段止（含）',
  limit_up_kv  decimal(10,3) not null               comment '考核上限 kV',
  limit_down_kv decimal(10,3) not null              comment '考核下限 kV',
  source       varchar(128) default null            comment '下发来源（文件名/通知单号）',
  create_by    varchar(64)  default ''              comment '创建者',
  create_time  datetime     default current_timestamp comment '创建时间',
  update_by    varchar(64)  default ''              comment '更新者',
  update_time  datetime     default current_timestamp on update current_timestamp comment '更新时间',
  remark       varchar(255) default null            comment '备注',
  primary key (curve_id),
  key idx_bus_quarter (busbar_num, quarter)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_0900_ai_ci comment='VQMS 第26条季度母线电压考核曲线（对账基准）';

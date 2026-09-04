-- 2026-09-04_05 第27条对账能力：装置台账 + 月度登记两表（新表，幂等）
create table if not exists vqms_art27_device (
  device_id        bigint       not null auto_increment comment '主键',
  entity_id        bigint       not null                comment '考核主体（逻辑FK）',
  device_code      varchar(32)  not null                comment '装置编号',
  device_name      varchar(64)  not null                comment '装置名称',
  device_type      tinyint      not null                comment '1=SVG 2=SVC 3=调相机（字典 vqms_art27_type）',
  rated_capacity_kw decimal(12,3) not null              comment '额定容量 kW（考核基数：缺额pp×容量/10000×0.1分，上限×5分）',
  auto_yx_num      bigint       default null            comment '投入自动运行信号点（自动采集预留；占位 null=人工登记）',
  energized_yx_num bigint       default null            comment '所在升压变带电信号点（自动采集预留）',
  status           char(1)      not null default '0'    comment '状态：0=正常, 1=停用',
  create_by        varchar(64)  default ''              comment '创建者',
  create_time      datetime     default current_timestamp comment '创建时间',
  update_by        varchar(64)  default ''              comment '更新者',
  update_time      datetime     default current_timestamp on update current_timestamp comment '更新时间',
  remark           varchar(255) default null            comment '备注',
  primary key (device_id),
  unique key uk_device_code (device_code)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_0900_ai_ci comment='VQMS 第27条动态无功补偿装置台账';

create table if not exists vqms_art27_month (
  id                bigint       not null auto_increment comment '主键',
  stat_month        varchar(7)   not null                comment '统计月（yyyy-MM）',
  device_id         bigint       not null                comment '装置（逻辑FK → vqms_art27_device）',
  auto_hours        decimal(10,3) default null           comment 'Σ投入自动可用小时（第1款分子）',
  energized_hours   decimal(10,3) default null           comment 'Σ升压变带电小时（第1款分母）',
  rate_penalty_days int          default 0               comment '调节速率不符天数（第2款）',
  nameplate_days    int          default 0               comment '铭牌能力不符天数（第3款）',
  regulator_rate    decimal(6,3) default null            comment '监管通知单上报可用率(%)——对账比对列',
  regulator_penalty decimal(10,3) default null           comment '监管通知单考核分——对账比对列',
  source            varchar(8)   not null default 'MANUAL' comment '来源：MANUAL=人工登记 / AUTO=信号自动（预留）',
  create_by         varchar(64)  default ''              comment '创建者',
  create_time       datetime     default current_timestamp comment '创建时间',
  update_by         varchar(64)  default ''              comment '更新者',
  update_time       datetime     default current_timestamp on update current_timestamp comment '更新时间',
  remark            varchar(255) default null            comment '备注',
  primary key (id),
  unique key uk_month_device (stat_month, device_id)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_0900_ai_ci comment='VQMS 第27条月度对账登记';

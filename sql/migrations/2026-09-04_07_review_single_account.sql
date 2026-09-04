-- 2026-09-04_07 免考标注复核改单账户口径（Leo 拍板简化：标注人可自批，撤销两级复核"标注人≠复核人"校验——代码层已删，本脚本同步列注释）；幂等
alter table vqms_exempt_annotation
  modify column review_by varchar(64) default null comment '复核人（单账户口径：标注人可自批，2026-09-04 拍板简化）';

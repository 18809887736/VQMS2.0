# VQMS 台账整定工具（对端 AVC 配置库导入）

读对端 AVC 配置库（`QHeatAvcRtdb.db`，SQLite）→ 与 VQMS 现台账比对 → **diff 报告 + 幂等 migration SQL**。
人工审阅后执行——不一键落库（改的是考核口径基数，多一道人工确认）。

## 用法（三步）

### 1) 导出现台账 current.json

```bash
python export_current.py --host ubuntu@43.155.156.140 --key ~/.ssh/id_ed25519_140 --out current.json
```

### 2) 跑比对

```bash
python import_rtdb.py --db QHeatAvcRtdb.db --current current.json --out out/ --stamp 2026-XX-XX_XX
```

### 3) 审阅执行

- `out/tuning_diff.md`：换号项 / 额定校核（容量差异 ⚠️ 只警示不落库，拍板④需监管确认）/ 阈值参考 / 无源点清单
- `out/<stamp>_rtdb_tuning.sql`：人工审后挪入 `sql/migrations/` 执行
- 执行后重跑全链（ingest→judge→runtime→rollup）验证等价

## 整定范围

| 配置库源 | VQMS 目标 |
|---|---|
| AVC_INFO.AVCStatusYxNum | vqms_yc_point_map `avc_onoff` 语义键行 |
| BUSBAR_GROUP.MainBarYcNum | vqms_busbar_group(group 0).main_indicator_yc_num |
| BUSBAR.realVYcNum | vqms_busbar(0/1).realtime_yc_num |
| GENERATOR.pYcNum/qYcNum | vqms_reactive_device(GEN_01/02).p/q_yc_num |
| GENERATOR.maxQPower/minQPower/ratingPPower | Q 额定与容量**校核**（不自动落库） |
| BUSBAR.TargetMAX/MIN、vUpUp/vDownDown | 阈值参考（人工核对 vqms_busbar_threshold） |

并网编码（grid_signal_*）、退出原因（exit_reason_*）、免考旗（exempt_flag）为 JS 派生设想点，配置库无源——维持测试号段或人工整定（见 docs/对外/致对端_考核信号点号落盘需求.md）。

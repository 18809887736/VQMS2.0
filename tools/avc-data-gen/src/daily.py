# -*- coding: utf-8 -*-
"""每日数据注入器：生成指定日（默认昨日）数据并幂等写入 qheatavchisdb。

用途：docker compose 常驻部署在测试机（140），每天凌晨（默认 01:00，早于 VQMS 03:00 夜任务）
为"昨日"灌入 背景数据 + 轮转场景 数据，使每日定时重算有新鲜数据可算——夜任务全链天天有活干。

幂等：写入前按日删除三表当日行（his_curve_sv/warn_info/yc_history 为无主键 varchar 老库，
delete-then-insert 是唯一幂等路径）；重跑同一天结果一致。

场景轮转：date.toordinal() % len(ALL_SCENARIOS) 日期锚定选场景（S/U 全集轮转）——
同一天永远同场景（可复现），逐日轮换保证每天演练不同判定分支（免考/缺数/异常/投退……）。

点号配置：环境变量 POINTS_FILE（默认 config/points.yaml）——容器部署挂载与
vqms_yc_point_map 注册表一致的 profile（测试号段 points.yaml / 真实号段 points-real.yaml）。
"""
from __future__ import annotations

import argparse
import os
import sys
from copy import copy
from datetime import datetime, timedelta
from pathlib import Path

import pymysql
import yaml

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from src.background import emit_background_rows  # noqa: E402
from src.emitters.his_curve_sv import emit_his_curve_sv_rows  # noqa: E402
from src.emitters.warn_info import emit_warn_info_rows  # noqa: E402
from src.emitters.yc_history import emit_yc_history_rows  # noqa: E402
from src.scenarios.base import ScenarioConfig  # noqa: E402
from src.scenarios.regulation import REGULATION_SCENARIOS  # noqa: E402
from src.scenarios.uptime import UPTIME_SCENARIOS  # noqa: E402
from src.timeutil import round_to_minute  # noqa: E402

_BATCH = 1000


def load_cfg(points_file: str) -> ScenarioConfig:
    base = Path(__file__).resolve().parents[1] / "config"
    points = yaml.safe_load(Path(points_file).read_text(encoding="utf-8"))
    thresholds = yaml.safe_load((base / "thresholds.yaml").read_text(encoding="utf-8"))
    return ScenarioConfig(points=points, thresholds=thresholds,
                          base_date=datetime.strptime(thresholds["base_date"], "%Y-%m-%d"))


def build_day_rows(day: datetime, cfg: ScenarioConfig):
    """复用 range 命令的日装配逻辑：场景（日期锚定轮转）+ 背景全覆盖。"""
    ALL_SCENARIOS = REGULATION_SCENARIOS + UPTIME_SCENARIOS
    scenario = ALL_SCENARIOS[day.toordinal() % len(ALL_SCENARIOS)]
    cfg_day = copy(cfg)
    cfg_day.base_date = day
    bundle = scenario.build(cfg_day)

    occ = {"curve": set(), "cmd": set(), "yc": set()}
    scenario_points = set()
    rows_c = emit_his_curve_sv_rows(bundle)
    rows_w = emit_warn_info_rows(bundle)
    rows_y = emit_yc_history_rows(bundle, exempt_point=cfg.points["exempt_flag"])
    for r in rows_c:
        occ["curve"].add((r["busbar_num"], round_to_minute(datetime.strptime(r["save_time"], "%Y-%m-%d %H:%M:%S.%f"))))
    for r in rows_w:
        occ["cmd"].add((r["warn_time"], r["obj_num"]))
    for r in rows_y:
        occ["yc"].add((r["yc_num"], round_to_minute(datetime.strptime(r["yc_time"], "%Y-%m-%d %H:%M:%S.%f"))))
        scenario_points.add(r["yc_num"])

    bg = emit_background_rows(day.toordinal(), day, occ, is_scenario_day=True,
                              scenario_points=scenario_points, points=cfg.points)
    return scenario, bundle, {"his_curve_sv": rows_c + bg["his_curve_sv"],
                      "warn_info": rows_w + bg["warn_info"],
                      "yc_history": rows_y + bg["yc_history"]}


def inject_day(day: datetime, cfg: ScenarioConfig, conn_params: dict) -> dict:
    scenario, bundle, rows = build_day_rows(day, cfg)
    day_start = day.strftime("%Y-%m-%d 00:00:00")
    day_end = (day + timedelta(days=1)).strftime("%Y-%m-%d 00:00:00")

    conn = pymysql.connect(charset="utf8mb4", autocommit=False, **conn_params)
    try:
        with conn.cursor() as cur:
            # 幂等：按日 delete-then-insert（varchar 时间字典序=时间序）
            cur.execute("delete from his_curve_sv where save_time >= %s and save_time < %s", (day_start, day_end))
            cur.execute("delete from warn_info where warn_time >= %s and warn_time < %s", (day_start, day_end))
            cur.execute("delete from yc_history where yc_time >= %s and yc_time < %s", (day_start, day_end))

            for table, cols in (("his_curve_sv", ("save_time", "busbar_num", "high_SV", "low_SV", "average_SV", "plan_SV")),
                                ("warn_info", ("warn_time", "millisecond", "warn_type", "obj_num", "warn_info")),
                                ("yc_history", ("yc_num", "yc_time", "yc_data"))):
                data = [tuple(r[c] for c in cols) for r in rows[table]]
                sql = "insert into {} ({}) values ({})".format(table, ",".join(cols), ",".join(["%s"] * len(cols)))
                for i in range(0, len(data), _BATCH):
                    cur.executemany(sql, data[i:i + _BATCH])
        conn.commit()
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()
    return {"scenario": f"{scenario.id} {bundle.description}",
            "rows": {t: len(v) for t, v in rows.items()}}


def conn_from_env() -> dict:
    return {"host": os.environ.get("DB_HOST", "qheat-sim-mysql57"),
            "port": int(os.environ.get("DB_PORT", "3306")),
            "user": os.environ.get("DB_USER", "root"),
            "password": os.environ["DB_PASSWORD"],
            "database": os.environ.get("DB_NAME", "qheatavchisdb")}


def main(argv=None) -> int:
    ap = argparse.ArgumentParser(prog="avc-data-gen-daily", description="单日生成并幂等注入 qheatavchisdb")
    ap.add_argument("--date", default=None, help="YYYY-MM-DD；默认昨日（北京时区，容器 TZ 保证）")
    ap.add_argument("--points-file", default=os.environ.get("POINTS_FILE", "config/points.yaml"))
    args = ap.parse_args(argv)

    day = (datetime.strptime(args.date, "%Y-%m-%d") if args.date
           else datetime.now() - timedelta(days=1)).replace(hour=0, minute=0, second=0, microsecond=0)
    cfg = load_cfg(args.points_file)
    result = inject_day(day, cfg, conn_from_env())
    print(f"[OK] {day:%Y-%m-%d} <- {result['scenario']} | rows {result['rows']}")
    return 0


if __name__ == "__main__":
    sys.exit(main())

"""his_curve_sv 行发射器：ScenarioBundle.curve -> 行字典列表。

时间戳故意偏离整点（仿真实 '15:27:57.556' 模式）：save_time 写前一分 57/58 秒 + 毫秒，
判定侧就近取整（秒 ≥ 30 进位）后才回到目标分钟。双写母线 0/1 毫秒略不同（仿真实采集）。
"""
from __future__ import annotations

from ..scenarios.base import ScenarioBundle
from ..timeutil import jitter_save_time, format_sv_save_time


def emit_his_curve_sv_rows(bundle: ScenarioBundle) -> list[dict]:
    rows = []
    for cp in bundle.curve:
        # jitter：写入时间偏离目标分钟（57/58 秒），取整后才落回目标分钟
        raw = jitter_save_time(cp.t, variant=int(cp.busbar_num))
        # average_SV/plan_SV 恒空（Leo 2026-09-05 拍板防扰乱视听）：判定链只消费 high/low，
        # 这两列历史上是废值/干扰项口径，写实值只会误导排查（S15 场景的 plan 干扰语义随之退役）
        rows.append({
            "save_time": format_sv_save_time(raw),
            "busbar_num": cp.busbar_num,
            "high_SV": cp.high_sv,
            "low_SV": cp.low_sv,
            "average_SV": None,
            "plan_SV": None,
        })
    return rows

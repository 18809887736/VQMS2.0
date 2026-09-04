"""背景日数据发射器：全天正常运行的曲线/指令/遥测，避开当日场景已占用的键。

用途：`cli range` 区间模式——场景切片（判定分支）之外，铺满全天背景数据，
使月度数据具备真实体量与节奏：
  - his_curve_sv：分钟级双母线双写（jitter .100/.237 双写节奏，整数 kV）
  - warn_info：每 5 分钟一条目标值指令，轮转码 {1,2,3} 均匀轮转，目标值 ~231kV 带小数
  - yc_history：15 分钟周期信号点（4001/4002/4003/4004/511/512/521/522/2003/3009/501）

确定性：所有"随机"量由 (day_idx, minute, 点号) 哈希推导——同参数重跑结果一致。
"""
from __future__ import annotations

from datetime import datetime, timedelta

from .timeutil import jitter_save_time, format_sv_save_time, format_warn_time, format_millisecond, format_yc_time
from .decode import encode_target_value


def _h(*seeds: int, mod: int) -> int:
    """确定性散列 → [0, mod)。"""
    x = 2166136261
    for s in seeds:
        x = ((x ^ (s & 0xFFFFFFFF)) * 16777619) & 0xFFFFFFFF
    return x % mod


def emit_background_rows(day_idx: int, day: datetime, occupied: dict, is_scenario_day: bool = False,
                         scenario_points: set | None = None, points: dict | None = None) -> dict[str, list[dict]]:
    """生成 day（00:00~23:59）背景三表行。

    occupied: {"curve": {(busbar, minute_dt)}, "cmd": {(warn_time_str, obj)},
               "yc": {(yc_num, minute_dt)}} —— 当日场景已写入的键，背景避让。
    yc 占用按【分钟粒度】（场景 jitter 秒形态 .100/.237 与背景整点字符串不同，
    字符串精确匹配拦不住背景整点补写——U02/U04 manifest 验收 2026-09-02 抓出的根因）。
    is_scenario_day: 场景日启用"保护区"——09:30~10:30 完全不写背景
    （场景的 yx501 阶跃/增量 t0 实时电压/窗口留白等语义不被背景网格点覆盖，
    manifest 验收 2026-09-02 抓出的背景-场景冲突根因）。
    scenario_points: 当日场景写过的 yc 点号集合——阶跃点 {3009,521,522,501} 仅当
    场景自带该点时间线时跳过（U 场景日独占投退/原因；S 场景日不写 3009，背景补写
    投运=1 保持全天，否则 S 日全天被误判退出——rollup 月度验收 2026-09-02 抓出）。
    """
    day0 = day.replace(hour=0, minute=0, second=0, microsecond=0)
    curve, cmd, yc = [], [], []

    def protected(minute: int) -> bool:
        """场景保护区：场景日 09:30~10:29 不写任何背景（场景语义独占）。"""
        return is_scenario_day and 570 <= minute < 630

    # ── his_curve_sv：分钟级双母线 ──
    for minute in range(1440):
        if protected(minute):
            continue
        t = day0 + timedelta(minutes=minute)
        base = 231 + (_h(day_idx, minute, 1, mod=7) - 3) // 2          # 230~231 波动基线
        high = base + (1 if _h(day_idx, minute, 2, mod=10) >= 7 else 0)  # 偶发 +1
        low = base - 1 - (1 if _h(day_idx, minute, 3, mod=12) >= 10 else 0)
        for busbar in (0, 1):
            if (busbar, t) in occupied["curve"]:
                continue
            raw = jitter_save_time(t, variant=busbar)
            curve.append({
                "save_time": format_sv_save_time(raw),
                "busbar_num": busbar,
                "high_SV": high,
                "low_SV": low,
                "average_SV": high,
                "plan_SV": 10245,  # 废值口径（算法不读）
            })

    # ── warn_info：5 分钟一条目标值指令，轮转码均匀轮转 ──
    rot_cycle = ("1", "2", "3")
    slot = 0
    for minute in range(0, 1440, 5):
        if protected(minute):
            continue
        t = day0 + timedelta(minutes=minute)
        wtime = format_warn_time(t)  # :00.000 整秒形态
        if (wtime, 0) in occupied["cmd"]:
            continue
        target = 230.8 + _h(day_idx, minute, 4, mod=9) * 0.1           # 230.8~231.6
        rot = rot_cycle[(day_idx * 288 + slot) % 3]
        cmd.append({
            "warn_time": wtime,
            "millisecond": format_millisecond(t),
            "warn_type": 5,
            "obj_num": 0,
            "warn_info": f"收到远方遥调执行指令:220KV目标值,{encode_target_value(target, rot)}.",
        })
        slot += 1

    # ── yc_history：15 分钟周期信号点 ──
    v_base = 231.0 + (_h(day_idx, 99, mod=5)) * 0.1                     # 当日电压基线 231.0~231.4
    for minute in range(0, 1440, 15):
        if protected(minute):
            continue
        t = day0 + timedelta(minutes=minute)
        ytime = format_yc_time(t)
        # 点号全部走 points.yaml（4000+ 测试号段，现场换号只改配置）
        values = {
            points["main_busbar_num"]: 0,              # 主母线号（0=东）
            points["realtime_v_busbar0"]: round(v_base + _h(day_idx, minute, 5, mod=5) * 0.1 - 0.2, 2),
            points["realtime_v_busbar1"]: round(v_base + _h(day_idx, minute, 6, mod=5) * 0.1 - 0.2, 2),
            points["active_power"]: 114800 + _h(day_idx, minute, 7, mod=40) * 250,  # 有功波动 ±5000kW 内
            points["grid_signal_main"]: 11,            # 并网编码：带电1×10+机数1
            points["grid_signal_aux"]: 11,
            points["exit_reason_main"]: 0,             # 退出原因：未退出
            points["exit_reason_aux"]: 0,
            points["remote_local"]: 1,                 # 远方
            points["avc_onoff"]: 1,                    # AVC 投入（背景默认投运）
            points["exempt_flag"]: 0,                  # 免考旗：未顶满
            # 设备级 P/Q：背景永远中段运行（远离曲线极限），设备级免考判定不触发——顶满语义只由 S20+ 场景构造
            points["p_gen1"]: 57400 + _h(day_idx, minute, 8, mod=5) * 100,    # 单机有功 ≈57.4MW（双机≈114800 对应总有功）
            points["q_gen1"]: 20000 + _h(day_idx, minute, 9, mod=21) * 1000,  # 单机无功 2~4万 kvar（极限≈24万，差一个数量级）
            points["p_gen2"]: 57400 + _h(day_idx, minute, 10, mod=5) * 100,
            points["q_gen2"]: 20000 + _h(day_idx, minute, 11, mod=21) * 1000,
        }
        step_owned = scenario_points or set()
        if is_scenario_day:
            # 阶跃保持类信号（AVC投退/退出原因/免考旗）：仅当场景自带时间线时由场景独占
            # （U 场景日；S 场景日不写投退，背景照常补写保持全天投运）
            for k in ("avc_onoff", "exit_reason_main", "exit_reason_aux", "exempt_flag"):
                if points[k] in step_owned:
                    values.pop(points[k], None)
        for yc_num, val in values.items():
            if (yc_num, t) in occupied["yc"]:
                continue
            yc.append({"yc_num": yc_num, "yc_time": ytime, "yc_data": val})

    return {"his_curve_sv": curve, "warn_info": cmd, "yc_history": yc}

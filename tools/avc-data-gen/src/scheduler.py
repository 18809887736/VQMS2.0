# -*- coding: utf-8 -*-
"""每日调度循环（容器入口）：默认 01:00 为"昨日"灌数（早于 VQMS 03:00 夜任务）。

环境变量：DAILY_TIME（默认 01:00）、RUN_ON_STARTUP（默认 1——启动即补灌昨日，幂等）。
时区：容器 TZ=Asia/Shanghai，本地时间即北京时间。
"""
from __future__ import annotations

import os
import time
from datetime import datetime, timedelta

from src.daily import conn_from_env, inject_day, load_cfg


def seconds_until(hour: int, minute: int) -> float:
    now = datetime.now()
    target = now.replace(hour=hour, minute=minute, second=0, microsecond=0)
    if target <= now:
        target += timedelta(days=1)
    return (target - now).total_seconds()


def main() -> int:
    hh, mm = (int(x) for x in os.environ.get("DAILY_TIME", "01:00").split(":"))
    cfg = load_cfg(os.environ.get("POINTS_FILE", "config/points.yaml"))
    params = conn_from_env()

    if os.environ.get("RUN_ON_STARTUP", "1") == "1":
        yesterday = (datetime.now() - timedelta(days=1)).replace(hour=0, minute=0, second=0, microsecond=0)
        print(f"[startup] 补灌 {yesterday:%Y-%m-%d}", flush=True)
        print("[startup] " + str(inject_day(yesterday, cfg, params)), flush=True)

    while True:
        wait = seconds_until(hh, mm)
        print(f"[loop] 下次灌数 {hh:02d}:{mm:02d}（{wait/3600:.1f}h 后），目标=执行日的前一天", flush=True)
        time.sleep(max(wait, 1))
        # 触发时刻已是 T 日凌晨 → 灌 T-1 日
        yesterday = (datetime.now() - timedelta(days=1)).replace(hour=0, minute=0, second=0, microsecond=0)
        try:
            print("[daily] " + str(inject_day(yesterday, cfg, params)), flush=True)
        except Exception as e:  # 单日失败不退出，次日重试（缺口由 VQMS 缺口补算兜底）
            print(f"[daily] 失败: {e}", flush=True)


if __name__ == "__main__":
    main()

"""指令解码参考实现（契约级稳定）。

口径：ROT10_V1 —— 现场核对报告发现①（阻断级）结论，2026-09-02 VQMS2.0 权威口径。
参照样本（10.0.0.9/qheatavchisdb 实数）：
  '收到远方遥调执行指令:220KV目标值,12315.4.'  → 231.54 kV
  '收到远方遥调执行指令:220KV目标值,12340.'    → 234.0  kV
  '收到远方遥调执行指令:辽宁母线电压增量指令编码值处理,2202.' → 增量 +0.2kV

warn_info.warn_info 文本两种形态：
- 目标值：',<编码>.' 其中编码 = 首位轮转码(1/2/3 均匀轮转，不参与数值) + 余值(可含一位小数) ÷ 10 = kV
- 增量值：4 位整数编码：第1位方向(2=加/1=减) · 第2位循环码 · 第3-4位幅值(×100V)
          → V_target = t₀ 实时电压 ± 幅值（需外部提供 realtime_v_kv）

生成器内部反向使用：先 decode 出 V_target，再据此排布 high/low 让场景落到目标分支。
"""
from __future__ import annotations

import re

# 抓取 warn_info 文本末尾 ",<数字或脏值>." 里的编码
_TRAILING_CODE = re.compile(r",([\w.]+)\.\s*$")

# 轮转码（首位，均匀轮转，不参与数值）
_ROTATE_CODES = ("1", "2", "3")


def _extract_code(text: str) -> str | None:
    m = _TRAILING_CODE.search(text.strip())
    return m.group(1) if m else None


def decode_target_value(text: str) -> float | None:
    """目标值形态（ROT10_V1）：首位轮转码 + 余值÷10 → kV。

    实例：'12315.4' → 去首位'1' → 2315.4 ÷ 10 = 231.54；'12340' → 2340 ÷ 10 = 234.0。
    无法识别（脏值/空余值/轮转码非法）返回 None（调用方按解码失败跳过）。
    """
    code = _extract_code(text)
    if code is None or len(code) < 2:
        return None
    rot, rest = code[0], code[1:]
    if rot not in _ROTATE_CODES:
        return None
    try:
        return round(float(rest) / 10.0, 2)
    except ValueError:
        return None


def encode_target_value(v_kv: float, rot: str = "1") -> str:
    """目标值编码（ROT10_V1 反向）：kV ×10 → 余值，前置轮转码。

    实例：231.54 → '12315.4'；234.0 → '12340'（整值不带小数点，仿实数 12340. 形态）。
    rot ∈ {'1','2','3'}；v_kv 两位小数内（×10 后至多一位小数）。
    """
    if rot not in _ROTATE_CODES:
        raise ValueError(f"非法轮转码: {rot}")
    scaled = round(v_kv * 10, 1)
    if abs(scaled - round(scaled)) < 1e-9:
        return f"{rot}{int(round(scaled))}"
    return f"{rot}{scaled:.1f}"


def decode_increment(text: str, realtime_v_kv: float | None) -> float | None:
    """增量值形态：4 位编码 → V_target = realtime_v ± 幅值(kV)。

    编码：第1位方向(2=加/1=减) · 第2位循环码 · 第3-4位幅值(每单位 100V = 0.1kV)。
    需外部提供 realtime_v_kv（t₀ 时刻实时母线电压）；缺失返回 None（缺实时电压跳过）。
    """
    code = _extract_code(text)
    if code is None or len(code) != 4 or "." in code:
        return None
    if realtime_v_kv is None:
        return None
    try:
        direction = int(code[0])
        magnitude_units = int(code[2:4])  # 第3-4位
    except ValueError:
        return None
    delta_kv = magnitude_units * 0.1  # 每单位 100V = 0.1 kV
    if direction == 2:       # 加
        return round(realtime_v_kv + delta_kv, 2)
    elif direction == 1:     # 减
        return round(realtime_v_kv - delta_kv, 2)
    return None


def decode_any(text: str, realtime_v_kv: float | None) -> float | None:
    """自动识别两种形态。目标值优先（文本含"目标值"），否则按增量解码。

    失败返回 None。第2位循环码语义不依赖（增量）。
    """
    if "目标值" in text:
        return decode_target_value(text)
    return decode_increment(text, realtime_v_kv)

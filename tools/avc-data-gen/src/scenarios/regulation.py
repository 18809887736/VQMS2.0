"""调节合格率场景（R 系列，S01-S19）。

每个场景一条主指令（warn_type=5）+ 窗口内逐分钟 his_curve_sv（每分钟双写 busbar 0/1）
+ 对应 yc_history 点 + yx501。用 decode 反向算 V_target，再据此排布 high/low 落到目标分支。

覆盖草稿 §2.4-2.8 两档平行全部分支：
- S01-S04 四档组合（快/经 各 合格/不合格）
- S05-S07 免考逐档（yx501 阶跃变位，验证免考结论不跨档）
- S08 偏低边界（L=V_target 测 ≤ 闭区间）
- S09/S10 增量指令 加/减
- S11 缺实时电压跳过 / S12 解码失败跳过
- S13 部分缺分钟 / S14 整窗全缺 / S15 plan_SV 干扰 / S16 L>H 异常
- S17 双指令分通道 / S18 :29 舍 / S19 :30 进
- S20-S23 设备级免考（附件6 §三：闭环无功设备正确方向顶满仍不达标）
  免考源字段 exempt_source：AUTO_YX（S05-S07 yx501）/ AUTO_DEVICE（S20/S23 设备顶满）/ 无（不免考）
"""
from __future__ import annotations

from datetime import timedelta

from .base import (
    Command, CurvePoint, YcPoint, QUAL, PEN, EXEMPT, SKIP,
    ScenarioBundle, ScenarioConfig,
)
from ..timeutil import at_minute, round_to_minute


# ────────────────────────── 辅助 ──────────────────────────

PLAN_SV_DISCARD = 10245  # plan_SV 废值干扰项（验证算法不读它）


def _cmd(t0, obj_num, text, realtime_v_kv=None, raw_warn_time=None):
    """构造 Command；raw_warn_time 默认 = t0，亚秒边界场景另行指定。"""
    return Command(t0=t0, raw_warn_time=raw_warn_time or t0, obj_num=obj_num,
                   warn_info_text=text, realtime_v_kv=realtime_v_kv)


def _win_curve(cfg, t0, *, fast_pts, econ_pts, outside=(224, 222),
               plan_sv=PLAN_SV_DISCARD, missing_main=()):
    """生成 [-2, T_econ+2] 窗口的双写曲线。

    窗口布局读 config/thresholds.yaml（t_fast_min/t_econ_min）——2026-08-22 与 D7 锁定值
    (t_fast=4, t_econ=5) 对齐（原 5/30 为拍板前旧口径，见测试方案 §前置 勘误）。
    fast_pts/econ_pts: list[(high, low)]，下标对应窗口内 minute_offset（fast 从1、econ 从 T_fast+1）。
    outside: 窗外余量 (high, low)。missing_main: 主母线(bn=0)缺失的 minute_offset 集合。
    返回 CurvePoint 列表（已双写）。
    """
    busbars = cfg.thresholds["default_busbar_pair"]
    t_fast = int(cfg.thresholds["t_fast_min"])
    t_econ = int(cfg.thresholds["t_econ_min"])
    pts = []
    for m in range(-2, t_econ + 3):
        t = t0 + timedelta(minutes=m)
        if 1 <= m <= t_fast:
            hi, lo = fast_pts[m - 1]
        elif t_fast + 1 <= m <= t_econ:
            hi, lo = econ_pts[m - t_fast - 1]
        else:
            hi, lo = outside
        for bn in busbars:
            if bn == 0 and m in missing_main:
                continue  # 主母线该分钟缺数据
            pts.append(CurvePoint(t=t, busbar_num=bn, high_sv=hi, low_sv=lo,
                                  average_sv=hi, plan_sv=plan_sv))
    return pts


def _realtime_meta(cfg, t0, *, realtime_kv=234.25, busbar=0):
    """通用 yc 点：主母线号(默认0) + 双母线实时电压 + 总有功。返回 YcPoint 列表。"""
    p = cfg.points
    return [
        YcPoint(yc_num=p["main_busbar_num"], t=t0, value=float(busbar)),
        YcPoint(yc_num=p["realtime_v_busbar0"], t=t0, value=float(realtime_kv)),
        YcPoint(yc_num=p["realtime_v_busbar1"], t=t0, value=float(realtime_kv)),
        YcPoint(yc_num=p["active_power"], t=t0, value=114800.0),
    ]


# 通用 fast/econ 窗口数据（5 / 25 个点）——夹住 223.15：L≤223.15≤H
_HOLD_FAST = [(224, 222), (224, 223), (225, 222), (224, 223)]  # L=222,H=225 夹（fast 窗 [1..4] 4 分钟）
_HOLD_ECON = [(224, 222)]                                     # L=222,H=224 夹（econ 窗 [5..5] 1 分钟）
# 全低于目标（不夹，偏高不到位）：H 全 < 223.15
_MISS_BELOW_FAST = [(222, 221)] * 4     # H=222 < 223.15 → 不夹
_MISS_BELOW_ECON = [(222, 221)]
# 全高于目标（不夹，偏低不到位）：L 全 > 223.15
_MISS_ABOVE_FAST = [(226, 225)] * 4     # L=225 > 223.15 → 不夹
_MISS_ABOVE_ECON = [(226, 225)]


# ────────────────────────── S01-S04 四档组合 ──────────────────────────

class S01FastQualEconQual:
    """快合+经合。目标值 12231.5→223.15kV，两窗都夹住。期望 {QUAL, QUAL}。"""
    id = "S01"
    def build(self, cfg):
        t0 = at_minute(cfg.base_date, hour=10, minute=0)
        curve = _win_curve(cfg, t0,
                           fast_pts=_HOLD_FAST, econ_pts=_HOLD_ECON)
        return ScenarioBundle(self.id, "快合+经合（目标值，夹住）", cfg.base_date,
            commands=[_cmd(t0, 0, "收到远方遥调执行指令:220KV目标值,12231.5.")],
            curve=curve, yc_points=_realtime_meta(cfg, t0), yx501_timeline=[(t0, 0)],
            expected={"fast": QUAL, "econ": QUAL, "v_target": 223.15})


class S02FastPenEconQual:
    """快不合+经合（调得慢最终调到）。快速窗全低不夹，经济窗夹住。期望 {PEN, QUAL}。"""
    id = "S02"
    def build(self, cfg):
        t0 = at_minute(cfg.base_date, hour=10, minute=0)
        curve = _win_curve(cfg, t0,
                           fast_pts=_MISS_BELOW_FAST, econ_pts=_HOLD_ECON)
        return ScenarioBundle(self.id, "快不合+经合（调得慢最终调到）", cfg.base_date,
            commands=[_cmd(t0, 0, "收到远方遥调执行指令:220KV目标值,12231.5.")],
            curve=curve, yc_points=_realtime_meta(cfg, t0), yx501_timeline=[(t0, 0)],
            expected={"fast": PEN, "econ": QUAL, "v_target": 223.15})


class S03FastQualEconPen:
    """快合+经不合（短期夹住、长期漂走）。期望 {QUAL, PEN}。"""
    id = "S03"
    def build(self, cfg):
        t0 = at_minute(cfg.base_date, hour=10, minute=0)
        curve = _win_curve(cfg, t0,
                           fast_pts=_HOLD_FAST, econ_pts=_MISS_BELOW_ECON)
        return ScenarioBundle(self.id, "快合+经不合（短期夹住长期漂走）", cfg.base_date,
            commands=[_cmd(t0, 0, "收到远方遥调执行指令:220KV目标值,12231.5.")],
            curve=curve, yc_points=_realtime_meta(cfg, t0), yx501_timeline=[(t0, 0)],
            expected={"fast": QUAL, "econ": PEN, "v_target": 223.15})


class S04FastPenEconPen:
    """两档都不合·非免考。期望 {PEN, PEN}。"""
    id = "S04"
    def build(self, cfg):
        t0 = at_minute(cfg.base_date, hour=10, minute=0)
        curve = _win_curve(cfg, t0,
                           fast_pts=_MISS_BELOW_FAST, econ_pts=_MISS_BELOW_ECON)
        return ScenarioBundle(self.id, "两档都不合·非免考", cfg.base_date,
            commands=[_cmd(t0, 0, "收到远方遥调执行指令:220KV目标值,12231.5.")],
            curve=curve, yc_points=_realtime_meta(cfg, t0), yx501_timeline=[(t0, 0)],
            expected={"fast": PEN, "econ": PEN, "v_target": 223.15})


# ────────────────────────── S05-S07 免考逐档 ──────────────────────────

class S05FastExemptEconPen:
    """两档都不合·快速性档免考（yx501 在快速窗全程=1）、经济性档不免考(=0)。
    yx501 阶跃：t0..t0+5=1（快速窗 [1,5] 全程1），t0+6 切 0（经济窗=0）。
    验证免考逐档、结论不跨档。期望 {EXEMPT, PEN}。"""
    id = "S05"
    def build(self, cfg):
        t0 = at_minute(cfg.base_date, hour=10, minute=0)
        curve = _win_curve(cfg, t0,
                           fast_pts=_MISS_BELOW_FAST, econ_pts=_MISS_BELOW_ECON)
        # 阶跃在 t0+6（快速窗结束之后），保证快速窗 [1,5] 全程 yx501=1
        t_cut = t0 + timedelta(minutes=6)
        return ScenarioBundle(self.id, "两档都不合·快免考+经不免考（yx501 阶跃）", cfg.base_date,
            commands=[_cmd(t0, 0, "收到远方遥调执行指令:220KV目标值,12231.5.")],
            curve=curve, yc_points=_realtime_meta(cfg, t0),
            yx501_timeline=[(t0, 1), (t_cut, 0)],
            expected={"fast": EXEMPT, "econ": PEN, "v_target": 223.15, "exempt_source": "AUTO_YX"})


class S06FastPenEconExempt:
    """两档都不合·快速性档不免考(=0)、经济性档免考(yx501 在经济窗=1)。
    yx501：t0..t0+5=0（快速窗），t0+6 切 1（覆盖经济窗 [6,30]）。期望 {PEN, EXEMPT}。"""
    id = "S06"
    def build(self, cfg):
        t0 = at_minute(cfg.base_date, hour=10, minute=0)
        curve = _win_curve(cfg, t0,
                           fast_pts=_MISS_BELOW_FAST, econ_pts=_MISS_BELOW_ECON)
        t_cut = t0 + timedelta(minutes=6)   # 经济窗起始
        return ScenarioBundle(self.id, "两档都不合·快不免考+经免考（yx501 阶跃）", cfg.base_date,
            commands=[_cmd(t0, 0, "收到远方遥调执行指令:220KV目标值,12231.5.")],
            curve=curve, yc_points=_realtime_meta(cfg, t0),
            yx501_timeline=[(t0, 0), (t_cut, 1)],
            expected={"fast": PEN, "econ": EXEMPT, "v_target": 223.15, "exempt_source": "AUTO_YX"})


class S07AllExempt:
    """两档都不合·全免考（yx501 全程=1）。期望 {EXEMPT, EXEMPT}。"""
    id = "S07"
    def build(self, cfg):
        t0 = at_minute(cfg.base_date, hour=10, minute=0)
        curve = _win_curve(cfg, t0,
                           fast_pts=_MISS_BELOW_FAST, econ_pts=_MISS_BELOW_ECON)
        return ScenarioBundle(self.id, "两档都不合·全免考（yx501 全程1）", cfg.base_date,
            commands=[_cmd(t0, 0, "收到远方遥调执行指令:220KV目标值,12231.5.")],
            curve=curve, yc_points=_realtime_meta(cfg, t0),
            yx501_timeline=[(t0, 1)],
            expected={"fast": EXEMPT, "econ": EXEMPT, "v_target": 223.15, "exempt_source": "AUTO_YX"})


# ────────────────────────── S08 偏低边界 ──────────────────────────

class S08LowBoundary:
    """偏低边界：目标值 12250→225.0kV，快速窗 L=225=V_target（测 ≤ 闭区间，边界算合格）。
    fast: low 全=225 → L=225，V_target=225，L≤V_target 边界算合格。
    econ: 不夹（漂高）。期望 {QUAL, PEN}。"""
    id = "S08"
    def build(self, cfg):
        t0 = at_minute(cfg.base_date, hour=10, minute=0)
        fast = [(226, 225)] * 4     # L=225, H=226，V_target=225 落 L 边界 → 合格
        # ⚠ 勿复用 _MISS_ABOVE_ECON(226,225)：本场景 V_target=225，该向量恰好边界夹住（2026-08-24 IT 准入抓出）
        econ = [(228, 227)]        # 漂高不夹：L=227 > V_target=225
        curve = _win_curve(cfg, t0,
                           fast_pts=fast, econ_pts=econ)
        return ScenarioBundle(self.id, "偏低边界（L=V_target 测≤闭区间）", cfg.base_date,
            commands=[_cmd(t0, 0, "收到远方遥调执行指令:220KV目标值,12250.")],
            curve=curve, yc_points=_realtime_meta(cfg, t0), yx501_timeline=[(t0, 0)],
            expected={"fast": QUAL, "econ": PEN, "v_target": 225.0})


# ────────────────────────── S09-S10 增量指令 ──────────────────────────

class S09IncrementUp:
    """增量加：编码 2202 @ 实时 234.25 → +0.2 → 234.45kV。构造 high/low 跨 234.45。
    整数 kV 无法精确夹 234.45，用 H=235/L=234 夹住（234≤234.45≤235）。期望 {QUAL, QUAL}。"""
    id = "S09"
    def build(self, cfg):
        t0 = at_minute(cfg.base_date, hour=10, minute=0)
        rt = 234.25
        v_target = 234.45  # rt + 0.2
        hold_fast = [(235, 234)] * 4     # 234 ≤ 234.45 ≤ 235 夹
        hold_econ = [(235, 234)]
        curve = _win_curve(cfg, t0,
                           fast_pts=hold_fast, econ_pts=hold_econ, outside=(235, 234))
        return ScenarioBundle(self.id, "增量加（2202@234.25→234.45kV）", cfg.base_date,
            commands=[_cmd(t0, 0, "收到远方遥调执行指令:辽宁母线电压增量指令编码值处理,2202.",
                           realtime_v_kv=rt)],
            curve=curve, yc_points=_realtime_meta(cfg, t0, realtime_kv=rt),
            yx501_timeline=[(t0, 0)],
            expected={"fast": QUAL, "econ": QUAL, "v_target": v_target})


class S10IncrementDown:
    """增量减：编码 1202 @ 实时 234.25 → -0.2 → 234.05kV。期望 {QUAL, QUAL}。"""
    id = "S10"
    def build(self, cfg):
        t0 = at_minute(cfg.base_date, hour=10, minute=0)
        rt = 234.25
        v_target = 234.05  # rt - 0.2
        hold_fast = [(235, 234)] * 4     # 234 ≤ 234.05 ≤ 235 夹
        hold_econ = [(235, 234)]
        curve = _win_curve(cfg, t0,
                           fast_pts=hold_fast, econ_pts=hold_econ, outside=(235, 234))
        return ScenarioBundle(self.id, "增量减（1202@234.25→234.05kV）", cfg.base_date,
            commands=[_cmd(t0, 0, "收到远方遥调执行指令:辽宁母线电压增量指令编码值处理,1202.",
                           realtime_v_kv=rt)],
            curve=curve, yc_points=_realtime_meta(cfg, t0, realtime_kv=rt),
            yx501_timeline=[(t0, 0)],
            expected={"fast": QUAL, "econ": QUAL, "v_target": v_target})


# ────────────────────────── S11-S12 指令跳过 ──────────────────────────

class S11IncrementNoRealtimeV:
    """增量指令·缺实时电压 → 指令跳过（不计发令次数）。yc_history 缺 t0 实时电压点。期望 SKIP。"""
    id = "S11"
    def build(self, cfg):
        t0 = at_minute(cfg.base_date, hour=10, minute=0)
        p = cfg.points
        curve = _win_curve(cfg, t0,
                           fast_pts=_HOLD_FAST, econ_pts=_HOLD_ECON)
        # 故意不写 realtime_v_busbar0/1（缺实时电压）
        yc = [
            YcPoint(yc_num=p["main_busbar_num"], t=t0, value=0.0),
            YcPoint(yc_num=p["active_power"], t=t0, value=114800.0),
        ]
        return ScenarioBundle(self.id, "增量指令·缺实时电压→跳过", cfg.base_date,
            commands=[_cmd(t0, 0, "收到远方遥调执行指令:辽宁母线电压增量指令编码值处理,2202.",
                           realtime_v_kv=None)],
            curve=curve, yc_points=yc, yx501_timeline=[(t0, 0)],
            expected={"fast": SKIP, "econ": SKIP, "v_target": None})


class S12DecodeFail:
    """解码失败（文本 ,abc. 无法解码）→ 指令跳过。期望 SKIP。"""
    id = "S12"
    def build(self, cfg):
        t0 = at_minute(cfg.base_date, hour=10, minute=0)
        curve = _win_curve(cfg, t0,
                           fast_pts=_HOLD_FAST, econ_pts=_HOLD_ECON)
        return ScenarioBundle(self.id, "解码失败（,abc.）→跳过", cfg.base_date,
            commands=[_cmd(t0, 0, "收到远方遥调执行指令:主省220KV目标值,abc.")],
            curve=curve, yc_points=_realtime_meta(cfg, t0), yx501_timeline=[(t0, 0)],
            expected={"fast": SKIP, "econ": SKIP, "v_target": None})


# ────────────────────────── S13-S16 数据异常 ──────────────────────────

class S13PartialMissingMinutes:
    """部分缺分钟：min3 主母线缺 his_curve_sv，其余夹住。缺的不影响聚合。期望 {QUAL, QUAL}。"""
    id = "S13"
    def build(self, cfg):
        t0 = at_minute(cfg.base_date, hour=10, minute=0)
        curve = _win_curve(cfg, t0,
                           fast_pts=_HOLD_FAST, econ_pts=_HOLD_ECON, missing_main={3})
        return ScenarioBundle(self.id, "部分缺分钟（min3 主母线缺，不影响）", cfg.base_date,
            commands=[_cmd(t0, 0, "收到远方遥调执行指令:220KV目标值,12231.5.")],
            curve=curve, yc_points=_realtime_meta(cfg, t0), yx501_timeline=[(t0, 0)],
            expected={"fast": QUAL, "econ": QUAL, "v_target": 223.15})


class S14WholeWindowMissing:
    """整窗全缺：快速窗 min1-5 主母线全缺（只生成副母线），经济窗正常。期望快速档失效剔除 SKIP。"""
    id = "S14"
    def build(self, cfg):
        t0 = at_minute(cfg.base_date, hour=10, minute=0)
        fast_missing = {1, 2, 3, 4}   # 快速窗 [1..t_fast=4] 整档全缺（对齐后 econ 窗=[5..5] 不受影响）
        curve = _win_curve(cfg, t0,
                           fast_pts=_HOLD_FAST, econ_pts=_HOLD_ECON, missing_main=fast_missing)
        return ScenarioBundle(self.id, "整窗全缺（快速窗主母线全缺→该档剔除）", cfg.base_date,
            commands=[_cmd(t0, 0, "收到远方遥调执行指令:220KV目标值,12231.5.")],
            curve=curve, yc_points=_realtime_meta(cfg, t0), yx501_timeline=[(t0, 0)],
            expected={"fast": SKIP, "econ": QUAL, "v_target": 223.15})


class S15PlanSvDiscard:
    """plan_SV 废值干扰：正常夹住 + plan_SV 全写 10245。验证算法不读 plan_SV。期望 {QUAL, QUAL}。"""
    id = "S15"
    def build(self, cfg):
        t0 = at_minute(cfg.base_date, hour=10, minute=0)
        curve = _win_curve(cfg, t0,
                           fast_pts=_HOLD_FAST, econ_pts=_HOLD_ECON, plan_sv=PLAN_SV_DISCARD)
        return ScenarioBundle(self.id, "plan_SV=10245 废值干扰（算法应不读）", cfg.base_date,
            commands=[_cmd(t0, 0, "收到远方遥调执行指令:220KV目标值,12231.5.")],
            curve=curve, yc_points=_realtime_meta(cfg, t0), yx501_timeline=[(t0, 0)],
            expected={"fast": QUAL, "econ": QUAL, "v_target": 223.15})


class S16IntervalInverted:
    """区间 L>H 异常：快速窗 low=225/high=224（反转）。该窗无效。期望快速档 SKIP（窗无效）。"""
    id = "S16"
    def build(self, cfg):
        t0 = at_minute(cfg.base_date, hour=10, minute=0)
        inverted_fast = [(224, 225)] * 4   # high=224 < low=225 → L=225 > H=224 异常（fast 窗 4 分钟全反转）
        curve = _win_curve(cfg, t0,
                           fast_pts=inverted_fast, econ_pts=_HOLD_ECON)
        return ScenarioBundle(self.id, "区间 L>H 异常（快速窗反转→无效）", cfg.base_date,
            commands=[_cmd(t0, 0, "收到远方遥调执行指令:220KV目标值,12231.5.")],
            curve=curve, yc_points=_realtime_meta(cfg, t0), yx501_timeline=[(t0, 0)],
            expected={"fast": SKIP, "econ": QUAL, "v_target": 223.15})


# ────────────────────────── S17 双指令分通道 ──────────────────────────

class S17TwoCommandsSplit:
    """双指令时间重叠·分 obj_num 通道：obj_num=0 夹住、obj_num=1 不夹。各独立结论。
    两条指令同 t0、不同 obj_num；曲线整体夹住（obj_num=0 通道），obj_num=1 用同一曲线但不夹
    （演示分通道——实际算法按 obj_num/主母线路由，这里用不同 obj_num 各自期望）。"""
    id = "S17"
    def build(self, cfg):
        t0 = at_minute(cfg.base_date, hour=10, minute=0)
        # 专用平顶带（H 恒 224）：obj0 的 223.15 夹住；obj1 的 225.0 恒高于 H 不夹。
        # 勿复用 _HOLD_FAST（其第 3 分钟 H=225，包络 max(high)=225 会把 225.0 边界含入 → obj1 误 QUAL；
        # manifest 验收抓出，2026-09-02 修正）
        curve = _win_curve(cfg, t0,
                           fast_pts=[(224, 222)] * 4, econ_pts=[(224, 222)])
        # obj_num=0 夹住；obj_num=1 目标更高（12250→225）不夹（H 全 224）
        return ScenarioBundle(self.id, "双指令分通道（obj0夹/obj1不夹）", cfg.base_date,
            commands=[
                _cmd(t0, 0, "收到远方遥调执行指令:220KV目标值,12231.5."),
                _cmd(t0, 1, "收到远方遥调执行指令:220KV目标值,12250."),
            ],
            curve=curve, yc_points=_realtime_meta(cfg, t0), yx501_timeline=[(t0, 0)],
            expected={"per_command": [
                {"obj_num": 0, "fast": QUAL, "econ": QUAL, "v_target": 223.15},
                {"obj_num": 1, "fast": PEN, "econ": PEN, "v_target": 225.0},
            ]})


# ────────────────────────── S18-S19 亚秒就近取整 ──────────────────────────

class S18RoundDown29:
    """亚秒就近取整·秒=29 舍：warn_time 10:00:29.447 → t0=10:00，曲线从 10:00 起。期望 t0 落 10:00。"""
    id = "S18"
    def build(self, cfg):
        t0 = at_minute(cfg.base_date, hour=10, minute=0)
        raw = at_minute(cfg.base_date, hour=10, minute=0, second=29, microsecond=447000)
        assert round_to_minute(raw) == t0
        curve = _win_curve(cfg, t0,
                           fast_pts=_HOLD_FAST, econ_pts=_HOLD_ECON)
        return ScenarioBundle(self.id, "亚秒:29舍（warn_time 10:00:29→t0=10:00）", cfg.base_date,
            commands=[_cmd(t0, 0, "收到远方遥调执行指令:220KV目标值,12231.5.", raw_warn_time=raw)],
            curve=curve, yc_points=_realtime_meta(cfg, t0), yx501_timeline=[(t0, 0)],
            expected={"fast": QUAL, "econ": QUAL, "v_target": 223.15, "t0_minute": "10:00"})


class S19RoundUp30:
    """亚秒就近取整·秒=30 进：warn_time 10:00:30.447 → t0=10:01，曲线从 10:01 起。期望 t0 落 10:01。"""
    id = "S19"
    def build(self, cfg):
        t0 = at_minute(cfg.base_date, hour=10, minute=1)
        raw = at_minute(cfg.base_date, hour=10, minute=0, second=30, microsecond=447000)
        assert round_to_minute(raw) == t0
        curve = _win_curve(cfg, t0,
                           fast_pts=_HOLD_FAST, econ_pts=_HOLD_ECON)
        return ScenarioBundle(self.id, "亚秒:30进（warn_time 10:00:30→t0=10:01）", cfg.base_date,
            commands=[_cmd(t0, 0, "收到远方遥调执行指令:220KV目标值,12231.5.", raw_warn_time=raw)],
            curve=curve, yc_points=_realtime_meta(cfg, t0), yx501_timeline=[(t0, 0)],
            expected={"fast": QUAL, "econ": QUAL, "v_target": 223.15, "t0_minute": "10:01"})


# ────────────────────────── S20-S23 设备级免考（附件6 §三）──────────────────────────
# 台账：GEN_01 216/217、GEN_02 316/317，均 type=1 闭环；P-Q 曲线种子 (0,250k,-150k)/(150k,225k,-120k)/(300k,200k,-100k)
# 判定采样时刻：fast @ t0+4、econ @ t0+6（floor 保持读）；免考源优先级 MANUAL > AUTO_YX > AUTO_DEVICE


def _device_pq(cfg, t0, p1_kw, q1_mid, q1_hold, p2_kw, q2_mid, q2_hold):
    """双机 P/Q 遥测：t0 写 P；t0+1 写中段 Q；t0+3 起顶到 hold 值并保持（t0+7 再写一次锚住窗口尾）。

    判定侧 floor 保持读：t0+4/t0+6 采样时刻读到 t0+3 的 hold 值；t0+1 中段值证明"逐步顶满"的台阶。
    """
    p = cfg.points
    pts = []
    for pn, p_kw in ((p["p_gen1"], p1_kw), (p["p_gen2"], p2_kw)):
        pts.append(YcPoint(yc_num=pn, t=t0, value=float(p_kw)))
    for qn, mid, hold in ((p["q_gen1"], q1_mid, q1_hold), (p["q_gen2"], q2_mid, q2_hold)):
        pts.append(YcPoint(yc_num=qn, t=t0 + timedelta(minutes=1), value=float(mid)))
        pts.append(YcPoint(yc_num=qn, t=t0 + timedelta(minutes=3), value=float(hold)))
        pts.append(YcPoint(yc_num=qn, t=t0 + timedelta(minutes=7), value=float(hold)))
    return pts


class S20DeviceExemptInject:
    """设备级免考·电压偏低·双机顶发出曲线极限 → AUTO_DEVICE 免考。
    目标 223.15（带 [222.15,224.15]），曲线全窗 H=222 低于带下沿 → 需 INJECT；
    P=75MW → 曲线插值 qUp=237500，双机 Q=236000（容差 2000 内顶满），t0+3 起保持；
    yx501=0（非 yx 免考，隔离免考源）。期望 {EXEMPT, EXEMPT, AUTO_DEVICE}。
    注：窗外(outside)也保持越限——econ 采样时刻 t0+tEcon+1=t0+6 在窗口外，
    若窗外回带内则方向不可判定（首版 2026-09-03 验收抓出，econ 档漏免）。"""
    id = "S20"
    def build(self, cfg):
        t0 = at_minute(cfg.base_date, hour=10, minute=0)
        curve = _win_curve(cfg, t0, fast_pts=_MISS_BELOW_FAST, econ_pts=_MISS_BELOW_ECON,
                           outside=(222, 221))
        dev = _device_pq(cfg, t0, 75000, 30000, 236000, 75000, 30000, 236000)
        return ScenarioBundle(self.id, "设备免考·偏低双机顶发出极限（AUTO_DEVICE）", cfg.base_date,
            commands=[_cmd(t0, 0, "收到远方遥调执行指令:220KV目标值,12231.5.")],
            curve=curve, yc_points=_realtime_meta(cfg, t0) + dev, yx501_timeline=[(t0, 0)],
            expected={"fast": EXEMPT, "econ": EXEMPT, "v_target": 223.15,
                      "exempt_source": "AUTO_DEVICE"})


class S21DeviceSlackNotExempt:
    """设备级·一台留余力 → 不免考（全部闭环设备尽力才免）。
    GEN_01 顶满 236000；GEN_02 仅 180000（距插值极限 237500 差 57500、距静态回退 200000 差 20000，
    均超容差——曲线生效/未生效两口径都不免；回放 A6 断言 2026-09-03 抓出静态口径误免后加固）。
    yx501=0。期望 {PEN, PEN}（免考源空）。"""
    id = "S21"
    def build(self, cfg):
        t0 = at_minute(cfg.base_date, hour=10, minute=0)
        curve = _win_curve(cfg, t0, fast_pts=_MISS_BELOW_FAST, econ_pts=_MISS_BELOW_ECON,
                           outside=(222, 221))
        dev = _device_pq(cfg, t0, 75000, 30000, 236000, 75000, 30000, 180000)
        return ScenarioBundle(self.id, "设备级·一台留余力→不免考", cfg.base_date,
            commands=[_cmd(t0, 0, "收到远方遥调执行指令:220KV目标值,12231.5.")],
            curve=curve, yc_points=_realtime_meta(cfg, t0) + dev, yx501_timeline=[(t0, 0)],
            expected={"fast": PEN, "econ": PEN, "v_target": 223.15})


class S22DeviceWrongDirection:
    """设备级·方向错 → 不免考。电压偏低需发出，双机却在吸收（Q=-50000 远离发出极限）。
    yx501=0。期望 {PEN, PEN}。"""
    id = "S22"
    def build(self, cfg):
        t0 = at_minute(cfg.base_date, hour=10, minute=0)
        curve = _win_curve(cfg, t0, fast_pts=_MISS_BELOW_FAST, econ_pts=_MISS_BELOW_ECON,
                           outside=(222, 221))
        dev = _device_pq(cfg, t0, 75000, 30000, -50000, 75000, 30000, -50000)
        return ScenarioBundle(self.id, "设备级·方向错（偏低却吸收）→不免考", cfg.base_date,
            commands=[_cmd(t0, 0, "收到远方遥调执行指令:220KV目标值,12231.5.")],
            curve=curve, yc_points=_realtime_meta(cfg, t0) + dev, yx501_timeline=[(t0, 0)],
            expected={"fast": PEN, "econ": PEN, "v_target": 223.15})


class S23DeviceExemptAbsorb:
    """设备级免考·电压偏高·双机顶吸收下限 → AUTO_DEVICE 免考。
    目标 223.15，曲线全窗 L=225 高于带上沿 → 需 ABSORB；
    P=150MW → 曲线种子点 qDown=-120000，双机 Q=-119000（容差 2000 内顶满）。
    yx501=0。窗外保持越限（同 S20，econ 采样在窗外）。期望 {EXEMPT, EXEMPT, AUTO_DEVICE}。"""
    id = "S23"
    def build(self, cfg):
        t0 = at_minute(cfg.base_date, hour=10, minute=0)
        curve = _win_curve(cfg, t0, fast_pts=_MISS_ABOVE_FAST, econ_pts=_MISS_ABOVE_ECON,
                           outside=(226, 225))
        dev = _device_pq(cfg, t0, 150000, 30000, -119000, 150000, 30000, -119000)
        return ScenarioBundle(self.id, "设备免考·偏高双机顶吸收下限（AUTO_DEVICE）", cfg.base_date,
            commands=[_cmd(t0, 0, "收到远方遥调执行指令:220KV目标值,12231.5.")],
            curve=curve, yc_points=_realtime_meta(cfg, t0) + dev, yx501_timeline=[(t0, 0)],
            expected={"fast": EXEMPT, "econ": EXEMPT, "v_target": 223.15,
                      "exempt_source": "AUTO_DEVICE"})


# ────────────────────────── S24-S25 数据公平性闸门 ──────────────────────────
# τ = vqms_judge_param.min_window_completeness_pct（默认 50，0=关闭）：completeness < τ 的档判 INVALID 不硬判；
# 0 值坏点拦截 = RegulationJudge.sanitizeBand（现场库核对报告发现④）：high/low 任一 ≤0 的行不采信，等同缺数


class S24TauGateBelowThreshold:
    """完整度闸门·快窗缺 3/4（completeness 0.25 < τ=50%）→ 快档 INVALID（缺数窗不罚电厂，A3/A4 最小口径）；
    经窗 1/1 正常 → QUAL。期望 {SKIP, QUAL}。"""
    id = "S24"
    def build(self, cfg):
        t0 = at_minute(cfg.base_date, hour=10, minute=0)
        curve = _win_curve(cfg, t0,
                           fast_pts=_HOLD_FAST, econ_pts=_HOLD_ECON, missing_main={1, 2, 3})
        return ScenarioBundle(self.id, "τ闸门·快窗缺3/4（0.25<τ=50%）→快档INVALID", cfg.base_date,
            commands=[_cmd(t0, 0, "收到远方遥调执行指令:220KV目标值,12231.5.")],
            curve=curve, yc_points=_realtime_meta(cfg, t0), yx501_timeline=[(t0, 0)],
            expected={"fast": SKIP, "econ": QUAL, "v_target": 223.15})


class S25ZeroBadPointBlocked:
    """脏值拦截·low_SV=0 坏点（发现④）：偏高不到位（L=225>223.15 应 PEN），min1 行 low=0；
    不拦截则包络 [0,226] 误夹目标 → 误 QUAL；拦截后该分钟缺数（快窗 completeness 0.75≥τ）→ 仍 PEN。
    期望 {PEN, PEN}。"""
    id = "S25"
    def build(self, cfg):
        t0 = at_minute(cfg.base_date, hour=10, minute=0)
        # fast 窗 [1..4]：min1 = (high=226, low=0) 坏点行；min2-4 = 正常偏高不夹
        fast_pts = [(226, 0)] + [(226, 225)] * 3
        curve = _win_curve(cfg, t0, fast_pts=fast_pts, econ_pts=_MISS_ABOVE_ECON,
                           outside=(226, 225))
        return ScenarioBundle(self.id, "脏值拦截·low=0坏点剔除（不毒化包络）", cfg.base_date,
            commands=[_cmd(t0, 0, "收到远方遥调执行指令:220KV目标值,12231.5.")],
            curve=curve, yc_points=_realtime_meta(cfg, t0), yx501_timeline=[(t0, 0)],
            expected={"fast": PEN, "econ": PEN, "v_target": 223.15})


# 场景注册表
REGULATION_SCENARIOS = [
    S01FastQualEconQual(), S02FastPenEconQual(), S03FastQualEconPen(), S04FastPenEconPen(),
    S05FastExemptEconPen(), S06FastPenEconExempt(), S07AllExempt(),
    S08LowBoundary(),
    S09IncrementUp(), S10IncrementDown(),
    S11IncrementNoRealtimeV(), S12DecodeFail(),
    S13PartialMissingMinutes(), S14WholeWindowMissing(), S15PlanSvDiscard(), S16IntervalInverted(),
    S17TwoCommandsSplit(),
    S18RoundDown29(), S19RoundUp30(),
    S20DeviceExemptInject(), S21DeviceSlackNotExempt(), S22DeviceWrongDirection(), S23DeviceExemptAbsorb(),
    S24TauGateBelowThreshold(), S25ZeroBadPointBlocked(),
]

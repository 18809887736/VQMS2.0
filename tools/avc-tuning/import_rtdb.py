# -*- coding: utf-8 -*-
"""VQMS 台账整定工具：读对端 AVC 配置库（QHeatAvcRtdb.db, SQLite）→ 与现台账比对 → diff 报告 + 幂等 migration SQL。

用法：
    python import_rtdb.py --db QHeatAvcRtdb.db --current current.json --out out/ --stamp 2026-09-04

current.json 为 VQMS 现台账导出（导出命令见 README.md），形态：
    {"point_map": {"avc_onoff": 4007, ...},
     "busbar": {"0": 4002, "1": 4003},
     "busbar_group": {"0": 4001},
     "device": {"GEN_01": {"q": 4012, "p": 4011, "q_up": 200000, "q_down": -100000}, ...},
     "entity": {"capacity_kw": 600000}}

整定范围（只动配置库真实存在的映射；并网编码/退出原因/免考旗为 JS 派生设想点，无配置库源，不自动整定）：
    AVC_INFO.AVCStatusYxNum      → point_key='avc_onoff'
    BUSBAR_GROUP.MainBarYcNum    → vqms_busbar_group(group 0).main_indicator_yc_num
    BUSBAR.realVYcNum            → vqms_busbar(busbar_num 0/1).realtime_yc_num
    GENERATOR.pYcNum/qYcNum      → vqms_reactive_device(GEN_01/02).p_yc_num/q_yc_num
    GENERATOR.maxQPower/minQPower/ratingPPower → 额定校核（容量差异不自动落库，拍板④需监管确认）
"""
import argparse
import json
import sqlite3
from pathlib import Path


def gbk(v):
    """配置库中文名按 GBK 存储被 sqlite 读成乱码时尝试修复（仅报告展示用）。"""
    if not isinstance(v, str):
        return v
    try:
        return v.encode('latin-1').decode('gbk')
    except (UnicodeEncodeError, UnicodeDecodeError):
        return v


def load_rtdb(db_path: str) -> dict:
    db = sqlite3.connect(f'file:{db_path}?mode=ro', uri=True)
    db.row_factory = sqlite3.Row
    cur = db.cursor()
    info = dict(cur.execute('select * from AVC_INFO').fetchone())
    group = dict(cur.execute('select * from BUSBAR_GROUP').fetchone())
    bars = [dict(r) for r in cur.execute('select * from BUSBAR order by busbarNum')]
    gens = [dict(r) for r in cur.execute('select * from GENERATOR order by generatorNum')]
    db.close()
    return {'avc': info, 'group': group, 'busbars': bars, 'generators': gens}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--db', required=True, help='QHeatAvcRtdb.db 路径')
    ap.add_argument('--current', required=True, help='现台账导出 current.json')
    ap.add_argument('--out', required=True, help='输出目录')
    ap.add_argument('--stamp', default='manual', help='migration 日期戳（如 2026-09-04_01）')
    args = ap.parse_args()

    rtdb = load_rtdb(args.db)
    cur = json.loads(Path(args.current).read_text(encoding='utf-8'))
    out_dir = Path(args.out)
    out_dir.mkdir(parents=True, exist_ok=True)

    md = ['# VQMS 台账整定 diff 报告', '',
          f'- 配置库：`{args.db}`',
          f'- 现台账：`{args.current}`', '']
    sql = [f'-- {args.stamp} 对端 AVC 配置库整定（import_rtdb.py 生成，人工审后执行；幂等）', '']
    n_change = n_same = n_warn = 0

    # ── 1. 点号整定（vqms_yc_point_map 语义键 + 台账列）──
    md += ['## 1. 点号整定', '', '| 项 | 现值 | 配置库值 | 动作 | 备注 |', '|---|---|---|---|---|']
    avc_yx = rtdb['avc'].get('AVCStatusYxNum')
    old = cur['point_map'].get('avc_onoff')
    if old != avc_yx:
        n_change += 1
        md.append(f'| avc_onoff（AVC投退） | {old} | **{avc_yx}** | 换号 | AVC_INFO.AVCStatusYxNum |')
        sql.append(f"update vqms_yc_point_map set point_num = {avc_yx}, point_kind='X', point_type='yx' "
                   f"where point_key = 'avc_onoff' and point_num = {old};")
    else:
        n_same += 1
        md.append(f'| avc_onoff（AVC投退） | {old} | {avc_yx} | = | |')

    main_yc = rtdb['group'].get('MainBarYcNum')
    old = cur['busbar_group'].get('0')
    if old != main_yc:
        n_change += 1
        md.append(f'| 主母线号指示点（group 0） | {old} | **{main_yc}** | 换号 | BUSBAR_GROUP.MainBarYcNum |')
        sql.append(f"update vqms_busbar_group set main_indicator_yc_num = {main_yc} "
                   f"where group_num = 0 and main_indicator_yc_num = {old};")
    else:
        n_same += 1
        md.append(f'| 主母线号指示点（group 0） | {old} | {main_yc} | = | |')

    for bar in rtdb['busbars']:
        bn = bar['busbarNum']
        real_yc = bar.get('realVYcNum')
        old = cur['busbar'].get(str(bn))
        if old != real_yc:
            n_change += 1
            md.append(f'| 母线 {bn} 实时电压点 | {old} | **{real_yc}** | 换号 | BUSBAR.realVYcNum（{gbk(bar["busbarName"])}） |')
            sql.append(f"update vqms_busbar set realtime_yc_num = {real_yc} "
                       f"where busbar_num = {bn} and realtime_yc_num = {old};")
        else:
            n_same += 1
            md.append(f'| 母线 {bn} 实时电压点 | {old} | {real_yc} | = | |')

    for i, gen in enumerate(rtdb['generators'], start=1):
        code = f'GEN_{i:02d}'
        dev = cur['device'].get(code)
        if not dev:
            n_warn += 1
            md.append(f'| {code} | 无台账行 | p={gen["pYcNum"]}/q={gen["qYcNum"]} | ⚠️ 台账缺机组 | 需人工补录 vqms_reactive_device |')
            continue
        for key, new, col in (('p', gen['pYcNum'], 'p_yc_num'), ('q', gen['qYcNum'], 'q_yc_num')):
            old = dev.get(key)
            if old != new:
                n_change += 1
                md.append(f'| {code} {key.upper()} 点 | {old} | **{new}** | 换号 | GENERATOR.{col} |')
                sql.append(f"update vqms_reactive_device set {col} = {new} "
                           f"where device_code = '{code}' and {col} = {old};")
            else:
                n_same += 1
                md.append(f'| {code} {key.upper()} 点 | {old} | {new} | = | |')

    # ── 2. 额定校核（不自动落库）──
    md += ['', '## 2. 额定值校核（差异不自动落库——容量拍板④需监管确认，Q 额定走 P-Q 曲线换版流程）', '',
           '| 项 | 现台账 | 配置库 | 差异 |', '|---|---|---|---|']
    for i, gen in enumerate(rtdb['generators'], start=1):
        code = f'GEN_{i:02d}'
        dev = cur['device'].get(code, {})
        for label, old_v, new_v in (
                (f'{code} Q 上限 kvar', dev.get('q_up'), gen['maxQPower']),
                (f'{code} Q 下限 kvar', dev.get('q_down'), gen['minQPower'])):
            if old_v != new_v:
                n_warn += 1
                md.append(f'| {label} | {old_v} | {new_v} | ⚠️ 不一致 |')
            else:
                n_same += 1
                md.append(f'| {label} | {old_v} | {new_v} | = |')

    cap_cfg = sum(g['ratingPPower'] or 0 for g in rtdb['generators'])
    cap_cur = cur['entity'].get('capacity_kw')
    if cap_cur != cap_cfg:
        n_warn += 1
        md.append(f'| **主体容量 kW（Σ ratingPPower）** | {cap_cur} | {cap_cfg} | ⚠️ **不一致**——'
                  f'结算口径以监管确认为准（清单2 §1），确认后 UPDATE vqms_entity |')
        sql.append(f'-- ⚠️ 容量差异 {cap_cur} -> {cap_cfg}：待监管确认后手工执行')
        sql.append(f'-- update vqms_entity set rated_capacity_kw = {cap_cfg} where entity_id = 1;')
    else:
        n_same += 1
        md.append(f'| 主体容量 kW | {cap_cur} | {cap_cfg} | = |')

    # ── 3. 阈值参考（人工核对，不生成 SQL）──
    md += ['', '## 3. 电压限值参考（人工核对 vqms_busbar_threshold，不自动整定）', '',
           '| 母线 | TargetMAX/MIN | vUpUp/vDownDown |', '|---|---|---|']
    for bar in rtdb['busbars']:
        md.append(f"| {bar['busbarNum']}（{gbk(bar['busbarName'])}） | {bar['TargetMAX']}/{bar['TargetMIN']} "
                  f"| {bar['vUpUpLimit']}/{bar['vDownDownLimit']} |")

    # ── 4. 无配置库源的点（维持 4000+ 或等对端落盘）──
    md += ['', '## 4. 无配置库源的点（维持测试号段/人工整定）', '',
           '| point_key | 现值 | 说明 |', '|---|---|---|']
    for key, note in (('grid_signal_main', '并网编码·正母：JS 派生设想点，配置库无源'),
                      ('grid_signal_aux', '并网编码·副母：同上'),
                      ('exit_reason_main', '退出原因·正母：对端接口已定待落盘'),
                      ('exit_reason_aux', '退出原因·副母：同上'),
                      ('exempt_flag', '免考旗：现场库不存在，落盘前三源判定兜底')):
        md.append(f'| {key} | {cur["point_map"].get(key)} | {note} |')

    md += ['', '---', f'**汇总：换号 {n_change} 项，一致 {n_same} 项，警示 {n_warn} 项。**',
           '执行顺序：人工审本报告 → 审 SQL → 挪入 sql/migrations/ 执行 → 重跑全链验证等价。']

    (out_dir / 'tuning_diff.md').write_text('\n'.join(md), encoding='utf-8')
    (out_dir / f'{args.stamp}_rtdb_tuning.sql').write_text('\n'.join(sql) + '\n', encoding='utf-8')
    print(f'[OK] 换号 {n_change} / 一致 {n_same} / 警示 {n_warn} -> {out_dir}/tuning_diff.md, {args.stamp}_rtdb_tuning.sql')


if __name__ == '__main__':
    main()

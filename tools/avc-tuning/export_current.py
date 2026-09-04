# -*- coding: utf-8 -*-
"""从部署机（140）导出 VQMS 现台账 current.json（供 import_rtdb.py 比对）。

用法：
    python export_current.py --host ubuntu@43.155.156.140 --key ~/.ssh/id_ed25519_140 --out current.json
"""
import argparse
import json
import subprocess
from pathlib import Path

QUERIES = {
    'point_map': 'select point_key, point_num from vqms_yc_point_map where point_key is not null',
    'busbar': 'select busbar_num, realtime_yc_num from vqms_busbar',
    'busbar_group': 'select group_num, main_indicator_yc_num from vqms_busbar_group',
    'device': 'select device_code, q_yc_num, p_yc_num, rated_q_up_kvar, rated_q_down_kvar from vqms_reactive_device',
    'entity': 'select rated_capacity_kw from vqms_entity limit 1',
}


def ssh_sql(host, key, sql):
    inner = (f'PW=$(grep "^MYSQL_ROOT_PASSWORD=" ~/work/myRuoYi-Vue-springboot3/docker/.env | cut -d= -f2); '
             f'docker exec ruoyi-mysql mysql -uroot -p"$PW" ry-vue -N -B -e "{sql}" 2>/dev/null')
    cmd = ['ssh', '-i', key, '-o', 'BatchMode=yes', host, inner]
    out = subprocess.run(cmd, capture_output=True, text=True, check=True).stdout.strip()
    return [ln.split('\t') for ln in out.splitlines() if ln]


def to_f(v):
    try:
        f = float(v)
        return int(f) if f == int(f) else f
    except (TypeError, ValueError):
        return None


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--host', default='ubuntu@43.155.156.140')
    ap.add_argument('--key', default='~/.ssh/id_ed25519_140')
    ap.add_argument('--out', default='current.json')
    args = ap.parse_args()
    key = str(Path(args.key).expanduser())

    cur = {'point_map': {}, 'busbar': {}, 'busbar_group': {}, 'device': {}, 'entity': {}}
    for r in ssh_sql(args.host, key, QUERIES['point_map']):
        cur['point_map'][r[0]] = to_f(r[1])
    for r in ssh_sql(args.host, key, QUERIES['busbar']):
        cur['busbar'][r[0]] = to_f(r[1])
    for r in ssh_sql(args.host, key, QUERIES['busbar_group']):
        cur['busbar_group'][r[0]] = to_f(r[1])
    for r in ssh_sql(args.host, key, QUERIES['device']):
        cur['device'][r[0]] = {'q': to_f(r[1]), 'p': to_f(r[2]), 'q_up': to_f(r[3]), 'q_down': to_f(r[4])}
    rows = ssh_sql(args.host, key, QUERIES['entity'])
    cur['entity'] = {'capacity_kw': to_f(rows[0][0]) if rows else None}

    Path(args.out).write_text(json.dumps(cur, ensure_ascii=False, indent=1), encoding='utf-8')
    print(f'[OK] -> {args.out}: point_map {len(cur["point_map"])} keys, device {len(cur["device"])}')


if __name__ == '__main__':
    main()

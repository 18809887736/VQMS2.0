# -*- coding: utf-8 -*-
"""VQMS t_fast 定标扫描：对同区间用 t_fast ∈ {1..4} 各重判一遍，对比响应分布与合格率。

方法论：t_fast 是快速性档窗口上界（vqms_judge_param，可整定 [1,4]，默认 4）。
定标依据 = 指令响应时长分布（response_minutes：首个夹住目标值的分钟）：
  - 若绝大多数指令 1~2 分钟内夹住，t_fast 收紧对合格率影响小、且能更早定性 → 可收紧
  - 若分布在 3~4 分钟有实质质量，t_fast=4 保留（收窄会把慢响应误判不合格）
sim 数据分布由造数场景决定（演示机制）；真实定标在真实样本回放时执行同一命令（见回放Runbook）。

用法：
  python tfast_scan.py --api http://localhost:18081/prod-api --user admin --pass admin123 \
                       --start 2025-09-01 --end 2026-03-31 --tfast 1,2,3,4
"""
import argparse
import json
import urllib.request
import urllib.parse
from collections import Counter

from replay import Api


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--api', required=True)
    ap.add_argument('--user', default='admin')
    ap.add_argument('--pass', dest='password', default='admin123')
    ap.add_argument('--start', required=True)
    ap.add_argument('--end', required=True)
    ap.add_argument('--tfast', default='1,2,3,4')
    ap.add_argument('--report', default=None)
    args = ap.parse_args()

    api = Api(args.api, args.user, args.password)
    results = []
    for tf in [int(x) for x in args.tfast.split(',')]:
        row = api.get('/vqms/judgeParam/list', {'paramKey': 't_fast'})
        item = next(r for r in row if r['paramKey'] == 't_fast')
        item['paramValue'] = tf
        api._req('PUT', '/vqms/judgeParam', item)
        jdg = api.post('/vqms/judge/regulation', {'start': args.start, 'end': args.end})
        cmds = api.get('/vqms/stats/commands', {'start': args.start, 'end': args.end})
        hist = Counter(c['responseMinutes'] for c in cmds)
        qual_rate = jdg['fastQUALIFIED'] + jdg['fastEXEMPTED'] if 'fastQUALIFIED' in jdg else None
        # API 返回键大小写双写（HashMap 大小写敏感双轨），按大写读
        fq = jdg.get('fastQUALIFIED', jdg.get('fastQualified', 0))
        fe = jdg.get('fastEXEMPTED', jdg.get('fastExempted', 0))
        fi = jdg.get('fastINVALID', jdg.get('fastInvalid', 0))
        fp = jdg.get('fastPENALIZED', jdg.get('fastPenalized', 0))
        total = jdg['judged']
        results.append({
            'tFast': tf, 'judged': total,
            'fastQual': fq, 'fastExempt': fe, 'fastPen': fp, 'fastInvalid': fi,
            'qualPct': round((fq + fe) * 100.0 / total, 3) if total else None,
            'respHist': {str(k): hist.get(k, 0) for k in (1, 2, 3, 4, 5)},
            'respNull': hist.get(None, 0),
        })
        print(f"t_fast={tf}: judged {total} 合格率 {results[-1]['qualPct']}% "
              f"(Q{fq}/E{fe}/P{fp}/I{fi}) 响应分布 {results[-1]['respHist']} 未夹 {results[-1]['respNull']}")

    # 恢复默认
    row = api.get('/vqms/judgeParam/list', {'paramKey': 't_fast'})
    item = next(r for r in row if r['paramKey'] == 't_fast')
    item['paramValue'] = 4
    api._req('PUT', '/vqms/judgeParam', item)
    api.post('/vqms/judge/regulation', {'start': args.start, 'end': args.end})
    print('t_fast 已恢复 4 并重判')

    import datetime, os
    stamp = datetime.datetime.now().strftime('%Y%m%d-%H%M%S')
    report_path = args.report or f'reports/tfast定标_{stamp}.md'
    os.makedirs(os.path.dirname(report_path) or '.', exist_ok=True)
    with open(report_path, 'w', encoding='utf-8') as f:
        f.write(f'# t_fast 定标扫描 {stamp}\n\n区间 {args.start}~{args.end}\n\n'
                '| t_fast | 分母 | 合格 | 免考 | 不合格 | 无效 | 合格率 | 响应1分 | 2分 | 3分 | 4分 | 5分 | 未夹 |\n'
                '|---|---|---|---|---|---|---|---|---|---|---|---|---|\n')
        for r in results:
            h = r['respHist']
            f.write(f"| {r['tFast']} | {r['judged']} | {r['fastQual']} | {r['fastExempt']} | {r['fastPen']} | "
                    f"{r['fastInvalid']} | {r['qualPct']}% | {h['1']} | {h['2']} | {h['3']} | {h['4']} | {h['5']} | {r['respNull']} |\n")
        f.write('\n## 定标建议\n\n'
                '- 合格率对 t_fast 的敏感度 + response_minutes 分布质量（3~4 分钟占比）决定收紧与否；\n'
                '- sim 数据分布由造数场景构造（背景指令即时夹住=1 分钟占绝对多数），仅验证扫描机制；\n'
                '- 真实定标：真实样本回放时执行同一命令，按真实分布给出建议（回放Runbook §3）。\n')
    print(f'报告 -> {report_path}')


if __name__ == '__main__':
    main()

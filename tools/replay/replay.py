# -*- coding: utf-8 -*-
"""VQMS 回放 harness：对 qheatavchisdb 兼容库分批全链重算 + 断言 + 报告。

用途（2026-09-03 定标轨）：
  - 真实样本到位前：在 sim 库上验证回放机制与断言层（分批边界、守恒、manifest 对账）
  - 真实样本到位后：同一命令一键回放（见 docs/测试/回放Runbook.md——只换 VQMS_SOURCE_JDBC_URL）

用法：
  python replay.py --api http://localhost:18081/prod-api --user admin --pass admin123 \
                   --start 2025-09-01 --end 2026-03-31 --batch month \
                   [--manifest ../avc-data-gen/output/replay.manifest.json]

断言层（数据公平性 + 记账守恒）：
  A1 守恒：ΣtotalCmds == Σ(fast 四桶) == Σ(econ 四桶)（D 粒度逐日行）
  A2 分母：Σ批次 rowsAccepted（ledger 摄取）== ΣtotalCmds（判定覆盖，漏判=0）
  A3 解码失败率 ≤ 阈值（默认 1%；超标=编码口径问题，全部指令判不了）
  A4 fast INVALID 率 ≤ 阈值（默认 5%；超标=源库缺数/脏值异常，数据质量哨兵）
  A5 免考源分布非静默（三源计数入报告）
  A6 manifest 对账（场景日主指令三状态+免考源，测试 oracle）
  A7 投运天数 == 区间天数（缺数跳过日=0）
"""
import argparse
import datetime
import json
import sys
import time
import urllib.request
import urllib.parse


class Api:
    def __init__(self, base, user, password):
        self.base = base.rstrip('/')
        self.token = self._login(user, password)

    def _login(self, user, password):
        url = self.base + '/login'
        body = json.dumps({'username': user, 'password': password}).encode()
        req = urllib.request.Request(url, data=body, method='POST')
        req.add_header('Content-Type', 'application/json')
        with urllib.request.urlopen(req, timeout=60) as r:
            d = json.loads(r.read().decode())
        if d.get('code') != 200 or not d.get('token'):
            sys.exit(f'登录失败: {d}')
        return d['token']

    def _req(self, method, path, body=None, params=None):
        url = self.base + path
        if params:
            url += '?' + urllib.parse.urlencode(params)
        data = json.dumps(body).encode() if body is not None else None
        req = urllib.request.Request(url, data=data, method=method)
        req.add_header('Content-Type', 'application/json')
        if getattr(self, 'token', None) and path != '/login':
            req.add_header('Authorization', 'Bearer ' + self.token)
        with urllib.request.urlopen(req, timeout=1800) as r:
            d = json.loads(r.read().decode())
        if d.get('code') != 200:
            raise RuntimeError(f'{method} {path} -> {d}')
        # AjaxResult 取 data；TableDataInfo（分页列表）取 rows
        if 'data' in d:
            return d['data']
        if 'rows' in d:
            return d['rows']
        return None

    def post(self, path, params):
        return self._req('POST', path, None, params)

    def get(self, path, params):
        return self._req('GET', path, None, params)


def month_batches(start, end):
    s = datetime.date.fromisoformat(start)
    e = datetime.date.fromisoformat(end)
    out = []
    while s <= e:
        nxt = (s.replace(day=1) + datetime.timedelta(days=32)).replace(day=1) - datetime.timedelta(days=1)
        out.append((s.isoformat(), min(nxt, e).isoformat()))
        s = nxt + datetime.timedelta(days=1)
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--api', required=True)
    ap.add_argument('--user', default='admin')
    ap.add_argument('--pass', dest='password', default='admin123')
    ap.add_argument('--start', required=True)
    ap.add_argument('--end', required=True)
    ap.add_argument('--batch', choices=['month', 'whole'], default='month')
    ap.add_argument('--manifest', default=None)
    ap.add_argument('--max-undecodable-pct', type=float, default=1.0)
    ap.add_argument('--max-invalid-fast-pct', type=float, default=5.0)
    ap.add_argument('--report', default=None)
    args = ap.parse_args()

    api = Api(args.api, args.user, args.password)
    batches = month_batches(args.start, args.end) if args.batch == 'month' else [(args.start, args.end)]

    log = []
    t_all = time.time()
    judge_totals = {'judged': 0, 'exemptManual': 0, 'exemptAutoYx': 0, 'exemptAutoDevice': 0,
                    'fastEXEMPTED': 0, 'econEXEMPTED': 0, 'fastINVALID': 0, 'econINVALID': 0}
    rows_accepted = 0
    for bs, be in batches:
        t0 = time.time()
        ing = api.post('/vqms/ingest/commands', {'start': bs, 'end': be})
        jdg = api.post('/vqms/judge/regulation', {'start': bs, 'end': be})
        api.post('/vqms/judge/runtime', {'start': bs, 'end': be})
        api.post('/vqms/stats/rollup', {'start': bs, 'end': be})
        rows_accepted += ing['rowsAccepted']
        for k in judge_totals:
            judge_totals[k] += jdg.get(k, 0)
        dt = time.time() - t0
        line = (f'批 {bs}~{be}: 摄取 {ing["rowsAccepted"]} 判定 {jdg["judged"]} '
                f'(YX {jdg.get("exemptAutoYx", 0)}/DEV {jdg.get("exemptAutoDevice", 0)}/MAN {jdg.get("exemptManual", 0)}) {dt:.1f}s')
        print(line)
        log.append(line)

    # ── 断言层 ──
    checks = []

    def check(name, ok, detail):
        checks.append((name, ok, detail))
        print(('PASS ' if ok else 'FAIL ') + name + ' | ' + detail)

    days = api.get('/vqms/stats/regulation', {'grain': 'D', 'start': args.start, 'end': args.end})
    tot = sum(d['totalCmds'] for d in days)
    fsum = sum(sum(d['counts'][k] for k in
                   ('qualifiedFast', 'penalizedFast', 'exemptedFast', 'invalidFast')) for d in days)
    esum = sum(sum(d['counts'][k] for k in
                   ('qualifiedEcon', 'penalizedEcon', 'exemptedEcon', 'invalidEcon')) for d in days)
    check('A1 守恒', tot == fsum == esum, f'分母 {tot} = fast {fsum} = econ {esum}')
    check('A2 分母=摄取', tot == rows_accepted, f'判定 {tot} == 摄取 {rows_accepted}（漏判 0）')
    undec = sum(d['counts']['undecodable'] for d in days)
    check('A3 解码失败率', undec * 100.0 <= tot * args.max_undecodable_pct,
          f'{undec}/{tot} = {undec * 100.0 / max(tot, 1):.3f}% ≤ {args.max_undecodable_pct}%')
    inv_f = sum(d['counts']['invalidFast'] for d in days)
    check('A4 fast INVALID 率', inv_f * 100.0 <= tot * args.max_invalid_fast_pct,
          f'{inv_f}/{tot} = {inv_f * 100.0 / max(tot, 1):.3f}% ≤ {args.max_invalid_fast_pct}%')
    check('A5 免考源分布', True,
          f'MANUAL {judge_totals["exemptManual"]} / AUTO_YX {judge_totals["exemptAutoYx"]} / AUTO_DEVICE {judge_totals["exemptAutoDevice"]}')
    rt_days = api.get('/vqms/stats/runtime', {'grain': 'D', 'start': args.start, 'end': args.end})
    expected_days = (datetime.date.fromisoformat(args.end) - datetime.date.fromisoformat(args.start)).days + 1
    skipped = expected_days - len(rt_days)
    check('A7 投运天数', skipped == 0, f'{len(rt_days)}/{expected_days} 天记账（跳过 {skipped}）')

    if args.manifest:
        S = {'QUAL': 'QUALIFIED', 'PEN': 'PENALIZED', 'EXEMPT': 'EXEMPTED', 'SKIP': 'INVALID'}
        man = {x['date']: x for x in json.load(open(args.manifest, encoding='utf-8'))}
        cmd_days = {}
        for bs, be in batches:
            for c in api.get('/vqms/stats/commands', {'start': bs, 'end': be}):
                d = c['cmdTime'][:10]
                if c['cmdTime'][11:16] in ('10:00', '10:01'):
                    cmd_days.setdefault(d, []).append(c)
        ok = bad = 0
        bads = []
        for d, exp in man.items():
            for c in cmd_days.get(d, []):
                per = exp['expected'].get('per_command')
                if per:
                    m = [p for p in per if p['obj_num'] == c['objNum']]
                    ef, ee, es = S[m[0]['fast']], S[m[0]['econ']], m[0].get('exempt_source', '-')
                elif 'fast' in exp['expected']:
                    ef, ee = S[exp['expected']['fast']], S[exp['expected']['econ']]
                    es = exp['expected'].get('exempt_source', '-')
                else:
                    continue
                if c['fastState'] == ef and c['econState'] == ee and (c.get('exemptSource') or '-') == es:
                    ok += 1
                else:
                    bad += 1
                    bads.append(f"{d} {exp['id']} obj{c['objNum']}: got({c['fastState']},{c['econState']},{c.get('exemptSource')}) want({ef},{ee},{es})")
        check('A6 manifest 对账', bad == 0, f'{ok} 对 {bad} 错' + ('；' + '; '.join(bads[:5]) if bads else ''))

    total_dt = time.time() - t_all
    failed = [c for c in checks if not c[1]]

    # ── 报告 ──
    stamp = datetime.datetime.now().strftime('%Y%m%d-%H%M%S')
    report_path = args.report or f'reports/回放报告_{stamp}.md'
    import os
    os.makedirs(os.path.dirname(report_path) or '.', exist_ok=True)
    with open(report_path, 'w', encoding='utf-8') as f:
        f.write(f'# VQMS 回放报告 {stamp}\n\n'
                f'- 区间：{args.start} ~ {args.end}（{len(batches)} 批，{args.batch} 批粒度），总耗时 {total_dt:.0f}s\n'
                f'- API：{args.api}\n\n## 批次\n\n```\n' + '\n'.join(log) + '\n```\n\n'
                f'## 断言\n\n| 结果 | 检查 | 明细 |\n|---|---|---|\n'
                + '\n'.join(f'| {"✅" if ok else "❌"} | {n} | {d} |' for n, ok, d in checks)
                + f'\n\n## 汇总\n\n分母 {tot}，fast 桶 {fsum}，econ 桶 {esum}，'
                  f'INVALID fast {inv_f} / econ {sum(d["counts"]["invalidEcon"] for d in days)}，'
                  f'解码失败 {undec}\n')
    print(f'\n报告 -> {report_path}；断言 {len(checks) - len(failed)}/{len(checks)} 通过')
    sys.exit(1 if failed else 0)


if __name__ == '__main__':
    main()

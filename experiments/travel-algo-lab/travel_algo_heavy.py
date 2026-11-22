#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
travel-algo-heavy：GA-TSP 暴力参数基准 + 规模扩展压测
=====================================================
设计目标（2026-11-22）：
- 多核并行（multiprocessing.Pool，吃满 CPU）
- 增量落盘 + 断点续跑（每 job 完成立即 append+flush 到 results.csv，中断后自动跳过已完成的）
- 四档求解器：GA / NN+2opt / NN+or-opt+2opt / 分层(聚类+GA)
- 邻接候选表 2-opt（k-NN candidate list，文献：localized search 是大规模的决定性设计）
- 时间预算护栏（--budget-hours），到点优雅收尾并汇总

用法（DSW）：
  nohup python3 travel_algo_heavy.py --stage all --budget 7 > heavy.log 2>&1 &
  tail -f heavy.log
"""
import argparse
import csv
import math
import os
import random
import time
from itertools import product

import numpy as np

try:
    from multiprocessing import Pool, cpu_count
except ImportError:
    Pool, cpu_count = None, 1

HERE = os.path.dirname(os.path.abspath(__file__))
RESULTS = os.path.join(HERE, "results.csv")
FIELDS = ["stage", "n", "algo", "pop", "gen", "cx", "mut", "elite",
          "nn_seed", "early_stop", "two_opt", "k_nn", "or_opt",
          "cluster_size", "seed", "dist", "gens", "evals", "sec", "improve_vs_nn_pct"]

# ---------------------------------------------------------------- 数据
def gen_data(n, n_clusters, spread, seed):
    """真实感合成数据：中国范围内 n_clusters 个地理簇（模拟同城/邻城景点分布）"""
    rng = np.random.default_rng(seed)
    centers = np.column_stack([rng.uniform(100, 122, n_clusters),
                               rng.uniform(22, 41, n_clusters)])
    idx = rng.integers(0, n_clusters, n)
    pts = centers[idx] + rng.normal(0, spread, (n, 2))
    return pts


def load_real(n_max=None):
    """真实 20 景点（若 attractions.csv 在场）"""
    import csv as _csv
    p = os.path.join(HERE, "attractions.csv")
    if not os.path.exists(p):
        return None
    with open(p, encoding="utf-8-sig") as f:
        rows = list(_csv.DictReader(f))
    pts = np.array([[float(r["lng"]), float(r["lat"])] for r in rows])
    return pts[:n_max] if n_max else pts


def dist_matrix(pts):
    lon = np.radians(pts[:, 0]); lat = np.radians(pts[:, 1])
    dlat = lat[:, None] - lat[None, :]
    dlon = lon[:, None] - lon[None, :]
    a = np.sin(dlat / 2) ** 2 + np.cos(lat)[:, None] * np.cos(lat)[None, :] * np.sin(dlon / 2) ** 2
    return 6371.0 * 2 * np.arcsin(np.sqrt(np.clip(a, 0, 1)))


def knn_list(dist, k):
    """每个点的 k 近邻候选表（2-opt/or-opt 的邻域限制，大规模关键设计）"""
    n = len(dist)
    np.fill_diagonal(dist, np.inf)
    nn = np.argsort(dist, axis=1)[:, :k]
    np.fill_diagonal(dist, 0)
    return nn


# ---------------------------------------------------------------- 求解器
def tour_len(tour, dist):
    t = np.asarray(tour)
    idx = np.arange(len(t) - 1)
    return float(dist[t[idx], t[idx + 1]].sum())


def solve_nn(dist):
    n = len(dist); rng = random.Random(0)
    cur = rng.randrange(n); visited = [False] * n; tour = [cur]; visited[cur] = True
    for _ in range(n - 1):
        row = dist[cur].copy()
        row[visited] = np.inf
        cur = int(row.argmin()); visited[cur] = True; tour.append(cur)
    return tour


def two_opt_nn(tour, dist, nn, deadline=None, max_pass=50):
    """邻接候选表 2-opt（first-improvement）。开放路径：j+1 必须有效。"""
    n = len(tour)
    pos = [0] * n
    for i, t in enumerate(tour): pos[t] = i
    improved = True; passes = 0
    while improved and passes < max_pass:
        improved = False; passes += 1
        for i in range(n - 2):
            a, b = tour[i], tour[i + 1]
            dab = dist[a][b]
            for c in nn[a]:
                p_c = pos[c]
                if p_c <= i + 1 or p_c + 1 >= n: continue
                d = tour[p_c + 1]
                if dist[a][c] + dist[b][d] < dab + dist[c][d] - 1e-9:
                    tour[i + 1:p_c + 1] = tour[i + 1:p_c + 1][::-1]
                    for j in range(i + 1, p_c + 1): pos[tour[j]] = j
                    improved = True
                    dab = dist[a][tour[i + 1]]
            if deadline and time.time() > deadline: return tour
    return tour


def or_opt_nn(tour, dist, nn, deadline=None):
    """段迁移 or-opt（段长 1-3）：把段拔出重插到更优位置（开放路径语义）"""
    n = len(tour)
    pos = [0] * n
    for i, t in enumerate(tour): pos[t] = i
    improved = True
    while improved:
        improved = False
        for seg in (1, 2, 3):
            i = 0
            while i + seg <= n - 1:
                s = tour[i:i + seg]
                prev = tour[i - 1] if i > 0 else None
                nxt = tour[i + seg] if i + seg < n else None
                gain_rm = ((dist[prev][s[0]] if prev is not None else 0)
                           + (dist[s[-1]][nxt] if nxt is not None else 0)
                           - dist[s[0]][s[-1]])
                if gain_rm <= 1e-9:
                    i += 1; continue
                anchors = set()
                if prev is not None: anchors.add(prev)
                for x in s: anchors.update(int(c) for c in nn[x])
                best_gain, best_a = 1e-9, -1
                for a in anchors:
                    j = pos[a]
                    if i <= j < i + seg: continue
                    nj = j + 1
                    a_next = tour[nj] if nj < n else None
                    add_in = dist[a][s[0]] + (dist[s[-1]][a_next] if a_next is not None else 0) \
                             - (dist[a][a_next] if a_next is not None else 0)
                    gain = gain_rm - add_in
                    if gain > best_gain:
                        best_gain, best_a = gain, a
                if best_a >= 0:
                    del tour[i:i + seg]
                    j = pos[best_a]
                    if j >= i: j -= seg  # 删除段后锚点若在段右侧需左移
                    tour[j + 1:j + 1] = s
                    for k2 in range(len(tour)): pos[tour[k2]] = k2
                    improved = True
                i += 1  # accept 后也推进（外层 while 会再扫），防止同位置微增益死循环
            if deadline and time.time() > deadline: return tour
    return tour


def ga_solve(dist, pop, gen, cx, mut, elite, nn_seed, early_stop, seed,
             deadline=None, k_nn=10):
    """GA 主干（与 Java 版语义一致）：NN 种子 + 锦标赛 + OX + 交换变异 + 精英 + 早停；收尾 2-opt(邻接表)"""
    n = len(dist); rng = random.Random(seed)
    evals = 0

    def tl(t):
        nonlocal evals; evals += 1
        t = np.asarray(t); idx = np.arange(n - 1)
        return float(dist[t[idx], t[idx + 1]].sum())

    population = [rng.sample(range(n), n) for _ in range(pop)]
    if nn_seed:
        cur = rng.randrange(n); visited = {cur}; tour = [cur]
        for _ in range(n - 1):
            row = dist[cur].copy(); row[list(visited)] = np.inf
            cur = int(row.argmin()); visited.add(cur); tour.append(cur)
        population[0] = tour
    best, best_f, stag, pre = None, -1.0, 0, -1.0
    g = 0
    for g in range(gen):
        if deadline and time.time() > deadline: break
        fits = [1 / (1 + tl(t)) for t in population]
        gb = max(fits)
        for t, f in zip(population, fits):
            if f > best_f: best_f, best = f, t[:]
        if early_stop and g >= gen // 2:
            stag = 0 if best_f > pre else stag + 1
            if stag >= early_stop: break
        pre = gb
        order = sorted(range(pop), key=lambda i: -fits[i])
        new = [population[i][:] for i in order[:elite]]
        def tsel():
            c = [rng.randrange(pop) for _ in range(5)]
            return population[max(c, key=lambda i: fits[i])][:]
        def ox(p1, p2):
            a, b = sorted(rng.sample(range(n), 2))
            child = [-1] * n; child[a:b + 1] = p1[a:b + 1]; ptr = (b + 1) % n
            for i in range(n):
                gene = p2[(b + 1 + i) % n]
                if gene not in child:
                    child[ptr] = gene; ptr = (ptr + 1) % n
            return child
        while len(new) < pop:
            p1, p2 = tsel(), tsel()
            child = ox(p1, p2) if rng.random() < cx else p1[:]
            if rng.random() < mut:
                i, j = rng.sample(range(n), 2); child[i], child[j] = child[j], child[i]
            new.append(child)
        population = new
    if best is None: best = population[0]
    # 收尾局部搜索
    nn = knn_list(dist.copy(), k_nn)
    best = two_opt_nn(best, dist, nn, deadline)
    return best, tl(best), g + 1, evals


def solve_hier(pts, dist, cfg, seed, deadline=None, cluster_size=80):
    """分层：kmeans 均衡聚类（簇≤cluster_size）→ 簇内 GA → 簇间最近端点拼接"""
    n = len(pts)
    k = max(1, math.ceil(n / cluster_size))
    rng = np.random.default_rng(seed)
    centers = pts[rng.choice(n, k, replace=False)]
    for _ in range(30):
        d = ((pts[:, None, :] - centers[None, :, :]) ** 2).sum(-1)
        assign = d.argmin(1)
        newc = np.array([pts[assign == c].mean(0) if (assign == c).any() else centers[c]
                         for c in range(k)])
        if np.allclose(newc, centers): break
        centers = newc
    order = []
    for c in range(k):
        members = np.where(assign == c)[0].tolist()
        if not members: continue
        sub = pts[members]
        if len(members) <= 3:
            order.extend(members); continue
        sd = dist_matrix(sub)
        t, _, _, _ = ga_solve(sd, min(cfg["pop"], 100), min(cfg["gen"], 150),
                              cfg["cx"], cfg["mut"], cfg["elite"], True, 30,
                              seed + c, deadline)
        order.extend(members[i] for i in t)
    # 簇间排序：贪心最近端点
    seq = []; used = set()
    groups = [order[i:i + cluster_size] for i in range(0, len(order), cluster_size)]
    cur_end = groups[0][0]
    while groups:
        bestg, bestrev, bestd = 0, False, float("inf")
        for gi, gpath in enumerate(groups):
            for rev in (False, True):
                dd = dist[cur_end][gpath[0] if not rev else gpath[-1]]
                if dd < bestd: bestd, bestg, bestrev = dd, gi, rev
        gpath = groups.pop(bestg)
        if bestrev: gpath = gpath[::-1]
        seq.extend(gpath); cur_end = gpath[-1]
    nn = knn_list(dist.copy(), 10)
    seq = two_opt_nn(seq, dist, nn, deadline)
    return seq


def run_job(job):
    """worker：执行单个 (配置,数据,种子) 组合，返回结果行"""
    t0 = time.time()
    n, seed, data_cfg = job["n"], job["seed"], job["data_cfg"]
    pts = gen_data(n, data_cfg["clusters"], data_cfg["spread"], data_cfg["seed"] + seed)
    dist = dist_matrix(pts)
    nn_k = job.get("k_nn", 10)
    nn = knn_list(dist.copy(), nn_k)
    nn_tour = solve_nn(dist)
    nn_d = tour_len(nn_tour, dist)
    deadline = time.time() + job.get("time_limit", 600) if job.get("time_limit") else None
    algo = job["algo"]
    gens = evals = 0
    if algo == "nn2opt":
        tour = two_opt_nn(nn_tour[:], dist, nn, deadline)
        if job.get("or_opt"):
            tour = or_opt_nn(tour, dist, nn, deadline)
            tour = two_opt_nn(tour, dist, nn, deadline)
    elif algo == "ga":
        tour, _, gens, evals = ga_solve(
            dist, job["pop"], job["gen"], job["cx"], job["mut"], job["elite"],
            job["nn_seed"], job["early_stop"], seed * 1000 + job["cfg_id"], deadline, nn_k)
        tour = two_opt_nn(tour, dist, nn, deadline) if job.get("two_opt", True) else tour
    elif algo == "hier":
        tour = solve_hier(pts, dist, job, seed, deadline, job.get("cluster_size", 80))
    else:
        raise ValueError(algo)
    d = tour_len(tour, dist)
    row = {k: job.get(k) for k in FIELDS}
    row.update({"dist": round(d, 2), "gens": gens, "evals": evals,
                "sec": round(time.time() - t0, 3),
                "improve_vs_nn_pct": round((nn_d - d) / nn_d * 100, 2)})
    return row


# ---------------------------------------------------------------- 任务编排
def append_rows(rows):
    new = not os.path.exists(RESULTS)
    with open(RESULTS, "a", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=FIELDS)
        if new: w.writeheader()
        w.writerows(rows); f.flush(); os.fsync(f.fileno())


def done_keys():
    if not os.path.exists(RESULTS): return set()
    with open(RESULTS, encoding="utf-8") as f:
        return {(r["stage"], r["n"], r["algo"], r["cfg_id"], r["seed"])
                for r in csv.DictReader(f)}


def build_jobs(stage, budget_deadline):
    jobs = []
    dc = lambda n: {"clusters": max(2, n // 15), "spread": 0.3, "seed": 42}
    if stage in ("screen", "all"):
        cfg_id = 0
        for pop, gen, cx, mut, nn_s, to in product(
                [50, 100, 150], [100, 200], [0.6, 0.8],
                [0.02, 0.05, 0.10], [True, False], [True, False]):
            cfg_id += 1
            jobs.append({"stage": "screen", "n": 50, "algo": "ga", "cfg_id": cfg_id,
                         "seed": 0, "data_cfg": dc(50), "pop": pop, "gen": gen,
                         "cx": cx, "mut": mut, "elite": max(2, pop // 20),
                         "nn_seed": nn_s, "early_stop": 30, "two_opt": to,
                         "k_nn": 10, "time_limit": 120})
    if stage in ("refine", "all"):
        top = [(150, 200, 0.8, 0.05, 1), (150, 400, 0.6, 0.02, 2), (100, 200, 0.8, 0.05, 3),
               (300, 200, 0.8, 0.05, 4), (150, 200, 0.6, 0.02, 5)]
        cfg_id = 100
        for pop, gen, cx, mut, base in top:
            for n in (25, 50, 100, 200):
                for seed in range(10):
                    cfg_id += 1
                    jobs.append({"stage": "refine", "n": n, "algo": "ga", "cfg_id": cfg_id,
                                 "seed": seed, "data_cfg": dc(n), "pop": pop, "gen": gen,
                                 "cx": cx, "mut": mut, "elite": max(2, pop // 20),
                                 "nn_seed": True, "early_stop": 30, "two_opt": True,
                                 "k_nn": 10, "time_limit": 300})
    if stage in ("scale", "all"):
        cfg_id = 500
        for n in (100, 200, 400, 800, 1600, 3200):
            for seed in range(3):
                cfg_id += 1
                jobs.append({"stage": "scale", "n": n, "algo": "nn2opt", "cfg_id": cfg_id,
                             "seed": seed, "data_cfg": dc(n), "k_nn": 10,
                             "or_opt": False, "time_limit": 600})
                jobs.append({"stage": "scale", "n": n, "algo": "nn2opt", "cfg_id": cfg_id + 10000,
                             "seed": seed, "data_cfg": dc(n), "k_nn": 10,
                             "or_opt": True, "time_limit": 90})
                if n <= 800:
                    jobs.append({"stage": "scale", "n": n, "algo": "ga", "cfg_id": cfg_id + 20000,
                                 "seed": seed, "data_cfg": dc(n), "pop": 150, "gen": 200,
                                 "cx": 0.8, "mut": 0.05, "elite": 7, "nn_seed": True,
                                 "early_stop": 30, "two_opt": True, "k_nn": 10,
                                 "time_limit": 600})
                jobs.append({"stage": "scale", "n": n, "algo": "hier", "cfg_id": cfg_id + 30000,
                             "seed": seed, "data_cfg": dc(n), "pop": 150, "gen": 200,
                             "cx": 0.8, "mut": 0.05, "elite": 7, "cluster_size": 80,
                             "k_nn": 10, "time_limit": 900})
    return jobs


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--stage", default="all", choices=["screen", "refine", "scale", "all"])
    ap.add_argument("--budget", type=float, default=7.0, help="小时")
    ap.add_argument("--workers", type=int, default=0, help="0=自动(cpu数)")
    args = ap.parse_args()
    deadline = time.time() + args.budget * 3600
    jobs = build_jobs(args.stage, deadline)
    done = done_keys()
    jobs = [j for j in jobs if (j["stage"], str(j["n"]), j["algo"], str(j["cfg_id"]), str(j["seed"])) not in done]
    total = len(jobs)
    print(f"[plan] stage={args.stage} 待跑={total} 已跳过={len(done)} workers={args.workers or 'auto'}")
    if not total: print("全部已完成"); return
    workers = args.workers or (cpu_count() or 1)
    done_cnt = 0
    if workers <= 1 or Pool is None:
        for j in jobs:
            if time.time() > deadline: print("[budget] 到点停止"); break
            append_rows([run_job(j)]); done_cnt += 1
            if done_cnt % 10 == 0 or done_cnt == total:
                print(f"[{done_cnt}/{total}] {j['stage']} n={j['n']} {j['algo']}")
    else:
        with Pool(workers) as pool:
            for row in pool.imap_unordered(run_job, jobs, chunksize=1):
                append_rows([row]); done_cnt += 1
                if done_cnt % 10 == 0 or done_cnt == total:
                    eta = (time.time() - (deadline - args.budget * 3600)) / done_cnt * (total - done_cnt) / 60
                    print(f"[{done_cnt}/{total}] 最近: {row['stage']} n={row['n']} {row['algo']} "
                          f"dist={row['dist']} sec={row['sec']} ETA≈{eta:.0f}min")
                if time.time() > deadline:
                    print("[budget] 到点，等待在跑任务收尾"); pool.terminate(); break
    print(f"[done] 结果在 {RESULTS}")


if __name__ == "__main__":
    main()

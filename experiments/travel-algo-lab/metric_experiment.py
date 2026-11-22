#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
metric_experiment.py —— 街道级"度量误差"裁决实验
==================================================
问题：粗排用坐标近似距离（Haversine/欧氏），精排用路网真距离，
      近似度量的排序质量损失有多大？值不值得把真路网提到粗排层？

模拟世界：
- 网格城市：景点落在网格街上（真实"真值"= 曼哈顿距离，反映单行道/禁左）
- 高架捷径：随机加少量"对角快速边"（速度 2x），近似高架/快速路
- 修正系数：网格城市的欧氏→路径换算 ≈ 1.27-1.41（PIU，路网绕行指数）

实验：同一批点，分别用 [近似度量] 和 [真值度量] 跑 NN+2-opt，
     两个顺序都用 [真值度量] 评估 → 报告劣化%
结论解读：
- 劣化 < 3%  → 坐标近似够用，粗排不必接路网（分层架构零成本成立）
- 劣化 > 10% → 老城单行道密集场景必须把 GraphHopper 提到粗排层
"""
import argparse
import json
import math
import random
import time

import numpy as np


def manhattan_true(a, b, shortcuts, cell=0.01):
    """真值：曼哈顿路网距离（km），若有捷径边且两端在捷径上则对角段按 0.5 折算"""
    dx, dy = abs(a[0] - b[0]), abs(a[1] - b[1])
    d = (dx + dy) * 111.0
    for (x1, y1), (x2, y2) in shortcuts:
        # 若两点连线大致沿捷径方向且投影落在捷径范围内，走捷径
        if (abs(x1 - x2) < 1e-9 and abs(a[0] - b[0]) < 1e-9 and
                min(y1, y2) - 1e-9 <= min(a[1], b[1]) and max(a[1], b[1]) <= max(y1, y2) + 1e-9):
            d = min(d, abs(a[1] - b[1]) * 111.0 * 0.5)
        elif (abs(y1 - y2) < 1e-9 and abs(a[1] - b[1]) < 1e-9 and
              min(x1, x2) - 1e-9 <= min(a[0], b[0]) and max(a[0], b[0]) <= max(x1, x2) + 1e-9):
            d = min(d, abs(a[0] - b[0]) * 111.0 * 0.5)
    return d


def true_matrix(pts, shortcuts):
    n = len(pts)
    m = np.zeros((n, n))
    for i in range(n):
        for j in range(i + 1, n):
            d = manhattan_true(pts[i], pts[j], shortcuts)
            m[i, j] = m[j, i] = d
    return m


def approx_matrix(pts):
    """近似度量：Haversine（当前 Java 粗排用的）"""
    lon = np.radians(pts[:, 0]); lat = np.radians(pts[:, 1])
    dlat = lat[:, None] - lat[None, :]
    dlon = lon[:, None] - lon[None, :]
    a = np.sin(dlat / 2) ** 2 + np.cos(lat)[:, None] * np.cos(lat)[None, :] * np.sin(dlon / 2) ** 2
    return 6371.0 * 2 * np.arcsin(np.sqrt(np.clip(a, 0, 1)))


def nn_order(dist, start=0):
    n = len(dist); vis = np.zeros(n, bool); tour = [start]; vis[start] = True
    cur = start
    for _ in range(n - 1):
        row = dist[cur].copy(); row[vis] = np.inf
        cur = int(row.argmin()); vis[cur] = True; tour.append(cur)
    return tour


def two_opt(tour, dist, max_pass=40):
    n = len(tour)
    for _ in range(max_pass):
        improved = False
        for i in range(n - 2):
            for j in range(i + 2, n - 1):
                d_before = dist[tour[i], tour[i + 1]] + dist[tour[j], tour[j + 1]]
                d_after = dist[tour[i], tour[j]] + dist[tour[i + 1], tour[j + 1]]
                if d_after < d_before - 1e-9:
                    tour[i + 1:j + 1] = tour[i + 1:j + 1][::-1]; improved = True
        if not improved: break
    return tour


def path_len(tour, dist):
    t = np.asarray(tour)
    return float(dist[t[:-1], t[1:]].sum())


def gen_grid_city(n, size, shortcuts, seed):
    """网格城市：景点吸附到网格街口（size×size），加 shortcuts 条快速对角线"""
    rng = np.random.default_rng(seed)
    cells = rng.choice(size * size, size=n, replace=False)
    pts = np.column_stack([(cells % size) * (1.0 / size), (cells // size) * (1.0 / size)])
    # 坐标变换到真实经纬跨度（城市 ~0.5 度）
    pts = pts * 0.5 + np.array([104.0, 30.0])
    sc = []
    for _ in range(shortcuts):
        vert = rng.random() < 0.5
        pos = rng.uniform(104.0, 104.5); lo, hi = rng.uniform(30.0, 30.4, 2)
        sc.append(((pos, min(lo, hi)) if not vert else (min(lo, hi), pos),
                   (pos, max(lo, hi)) if not vert else (max(lo, hi), pos)))
    return pts, sc


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--sizes", default="20,50,100")
    ap.add_argument("--seeds", type=int, default=20)
    ap.add_argument("--shortcuts", type=int, default=2)
    ap.add_argument("--out", default="metric_results.csv")
    args = ap.parse_args()

    import csv
    with open(args.out, "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["n", "seed", "true_opt_len", "approx_order_true_len",
                    "true_order_true_len", "approx_degradation_pct", "sec"])
        for n in [int(x) for x in args.sizes.split(",")]:
            degr = []
            for seed in range(args.seeds):
                t0 = time.time()
                pts, sc = gen_grid_city(n, 10, args.shortcuts, seed)
                true_d = true_matrix(pts, sc)
                app_d = approx_matrix(pts)
                # 两个世界各自求解
                order_approx = two_opt(nn_order(app_d), app_d)   # 现状：近似度量下求序
                order_true = two_opt(nn_order(true_d), true_d)   # 理想：真值度量下求序
                # 统一用真值评估
                l_true_opt = path_len(order_true, true_d)
                l_approx = path_len(order_approx, true_d)
                deg = (l_approx - l_true_opt) / l_true_opt * 100
                degr.append(deg)
                w.writerow([n, seed, round(l_true_opt, 2), round(l_approx, 2),
                            round(l_true_opt, 2), round(deg, 2), round(time.time() - t0, 2)])
            print(f"n={n}: 近似排序劣化 均值={np.mean(degr):.2f}% "
                  f"中位={np.median(degr):.2f}% P90={np.percentile(degr, 90):.2f}% "
                  f"最差={max(degr):.2f}%")
    print(f"结果已写入 {args.out}")


if __name__ == "__main__":
    main()

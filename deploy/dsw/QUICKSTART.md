# 魔搭 DSW 暴力运算 Quickstart

## 目标

在魔搭（ModelScope）CPU 服务器上跑 `travel_algo_heavy.py` 的满血参数暴力基准，验证 GA-TSP 算法在不同参数组合下的表现。

## 数据流

```
GitHub master (事实源)
  └─ .github/workflows/sync-gitee.yml (push 时自动镜像)
       └─ Gitee 私有仓库 (travel-regulations/site-project)
            └─ DSW 实例通过 Gitee API v5 (base64) 拉取数据
                 └─ travel_algo_heavy.py 多核并行暴力运算
                      └─ results.csv 增量落盘 + 断点续跑
```

## 步骤

### 1. 创建 DSW 实例

- 登录 [modelscope.com](https://www.modelscope.com)
- 进入「实验室」→「DSW」（Data Science Workshop）
- 创建实例：
  - 规格：CPU 型，8 核 16GB+（推荐），或 4 核 8GB（最低）
  - 镜像：Python 3.9+ / 3.11
  - 存储：至少 20GB（结果文件 + 仓库副本）
  - 网络：公网访问（需要拉取 GitHub/Gitee）

### 2. 配置环境变量

在 DSW 控制台 → 环境变量中添加：

```
GITEE_TOKEN=<你的 Gitee 私人令牌>
```

令牌权限：只需勾选 `projects`（read），从 Gitee 设置 → 私人令牌 生成。

### 3. 启动运算

方式一：一键脚本（推荐）

```bash
# 进入 DSW Terminal
bash run_heavy.sh --stage all --budget 8
```

方式二：手动

```bash
# 克隆
git clone https://github.com/888newstep/travel-website.git
cd travel-website/experiments/travel-algo-lab

# 设置 token（如果环境变量没设）
export GITEE_TOKEN=***



# 启动（8 小时预算，自动吃满 CPU 核心）
nohup python3 travel_algo_heavy.py --stage all --budget 8 --workers 0 > heavy.log 2>&1 &

# 监控
tail -f heavy.log
```

### 4. 监控进度

```bash
# 实时日志
tail -f heavy.log

# 已完成任务数
wc -l results.csv

# 结果预览
tail -5 results.csv
```

### 5. 推回结果

运算完成后，把 `results.csv` 推回 GitHub：

```bash
cd travel-website
git add experiments/travel-algo-lab/results.csv
git commit -m "algo: brute-force results (DSW, stage=all)"
git push origin master
```

推送后 GitHub Actions 自动镜像到 Gitee。

## 实验阶段说明

| 阶段 | 任务数 | 内容 | 预计时间 |
|---|---|---|---|
| `screen` | 576 | 参数网格初筛（pop/gen/cx/mut/nn_seed/2-opt） | 2-4h |
| `refine` | 500 | Top5 配置 × 4 规模 × 10 种子 | 4-8h |
| `scale` | 63 | 大规模压测 n=100-3200（nn2opt/ga/hier） | 2-4h |
| `brute` | 2230 | 满血网格 × 3 种子 + Top 配置 × 30 种子 + 超大规模 | 12-24h |
| `all` | 3459 | 全部阶段顺序执行 | 24-48h |

## 断点续跑

脚本自动检查 `results.csv` 中已完成的 `(stage, n, algo, cfg_id, seed)` 组合，中断后重新运行自动跳过已完成的，从断点继续。

## 依赖

- Python 3.9+
- numpy（DSW 预装）
- pandas（DSW 预装）
- multiprocessing（标准库）

## 常见问题

**Q：DSW 实例访问 GitHub/Gitee 被墙？**

A：DSW 实例有公网 IP，通常可直接访问。如不行，在 DSW 控制台配置代理。

**Q：GITEE_TOKEN 权限？**

A：只需 `projects` read 权限。从 Gitee 设置 → 私人令牌 → 生成新令牌，勾选「项目」即可。

**Q：结果文件太大？**

A：`results.csv` 约 3459 行 × 22 列，约 500KB，完全没问题。如果跑 `brute` 阶段，约 2230 行，也是几百 KB。
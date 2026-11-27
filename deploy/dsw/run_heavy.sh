#!/bin/bash
# =============================================================================
# travel-algo-lab DSW 一键启动脚本（魔搭 CPU 服务器）
# =============================================================================
# 用法（魔搭 DSW 实例内）：
#   bash run_heavy.sh --stage all --budget 8
#
# 前置条件：
#   1. DSW 实例已创建（CPU 型即可，建议 8 核以上）
#   2. 环境变量 GITEE_TOKEN 已设置（Gitee 私人令牌，权限只需 projects read）
#   3. Python 3.9+ 已安装（DSW 默认带）
#
# 数据流：GitHub master → Actions 镜像 → Gitee → 本脚本从 Gitee API 拉取 → 跑完结果落盘
# =============================================================================
set -euo pipefail

STAGE="${1:-all}"
BUDGET="${2:-8}"
WORKDIR="${WORKDIR:-/mnt/workspace}"
REPO_URL="https://github.com/888newstep/travel-website.git"
EXPERIMENT_DIR="experiments/travel-algo-lab"

echo "============================================"
echo " travel-algo-lab DSW 暴力运算"
echo " stage=${STAGE} budget=${BUDGET}h"
echo " workdir=${WORKDIR}"
echo "============================================"

# 1. 克隆仓库（如已存在则 pull）
if [ ! -d "${WORKDIR}/travel-website" ]; then
    echo "[1/4] 克隆仓库..."
    git clone "${REPO_URL}" "${WORKDIR}/travel-website"
else
    echo "[1/4] 更新仓库..."
    cd "${WORKDIR}/travel-website" && git pull origin master
fi

cd "${WORKDIR}/travel-website/${EXPERIMENT_DIR}"

# 2. 安装依赖（轻量，DSW 已预装 numpy/pandas）
echo "[2/4] 检查依赖..."
python3 -c "import numpy, pandas; print('numpy', numpy.__version__, 'pandas', pandas.__version__)" 2>&1 || {
    echo "安装依赖..."
    pip install numpy pandas --quiet
}

# 3. 确保 Gitee token 可用（notebook 用它拉取真实数据）
echo "[3/4] 校验 GITEE_TOKEN..."
if [ -z "${GITEE_TOKEN:-}" ]; then
    echo "WARNING: GITEE_TOKEN 未设置，notebook 数据加载将失败"
    echo "请在 DSW 控制台 → 环境变量中设置 GITEE_TOKEN"
else
    echo "GITEE_TOKEN 已设置（长度 ${#GITEE_TOKEN}）"
fi

# 4. 启动暴力运算（后台 + 日志）
echo "[4/4] 启动暴力运算..."
nohup python3 travel_algo_heavy.py \
    --stage "${STAGE}" \
    --budget "${BUDGET}" \
    --workers 0 \
    > heavy.log 2>&1 &

PID=$!
echo "PID=${PID}"
echo "日志: tail -f heavy.log"
echo "结果: results.csv (每完成一个 job 立即落盘，断点续跑)"

# 等待完成或预算到期
echo "等待运算完成（预算 ${BUDGET} 小时）..."
wait ${PID} || true

echo "============================================"
echo " 运算完成。结果文件："
echo "   ${WORKDIR}/travel-website/${EXPERIMENT_DIR}/results.csv"
echo "   ${WORKDIR}/travel-website/${EXPERIMENT_DIR}/heavy.log"
echo "============================================"

# 5. （可选）把结果推回仓库
if [ "${PUSH_BACK:-0}" = "1" ]; then
    echo "[5] 推回结果..."
    cd "${WORKDIR}/travel-website"
    git add experiments/travel-algo-lab/results.csv
    git commit -m "algo: brute-force results (DSW, stage=${STAGE})" || true
    git push origin master
    echo "已推回 GitHub（自动镜像 Gitee）"
fi
#!/usr/bin/env bash
# travel 云端部署脚本（服务器侧执行；GitHub Actions 或人工 SSH 均可调用）
#
# 部署形态：裸 jar + systemd（travel-stack.target，5 服务，无 file-service）
# 用法：./deploy.sh <上传了新 jar 的目录> [部署根目录]
#   默认部署根目录 /home/ubuntu/travel
# 流程：备份现有 jar → 覆盖新 jar → 重启 travel-stack.target → 健康检查 → 失败自动回滚
#
# 注意：travel-stack 与 newagent/novel-agent 三方 systemd Conflicts 互斥，
#       重启 travel 会自动停掉正在运行的 newagent —— 手动触发前请确认当前不需要另一个项目。

set -euo pipefail

STAGING_DIR="${1:?用法: deploy.sh <新jar所在目录> [部署根目录]}"
DEPLOY_ROOT="${2:-/home/ubuntu/travel}"
TARGET="travel-stack.target"
GATEWAY_HEALTH="http://127.0.0.1:8090/actuator/health"
HEALTH_TIMEOUT_SEC=180
MODULES=(gateway user-service attraction-service route-service collection-service)

log() { echo "[deploy $(date +%H:%M:%S)] $*"; }

# 1. 校验 staging 目录：5 个模块 jar 必须齐全
log "校验新版本产物: $STAGING_DIR"
for mod in "${MODULES[@]}"; do
    jar_path=$(find "$STAGING_DIR" -maxdepth 1 -name "${mod}-*.jar" ! -name "*.original" | head -1)
    [[ -n "$jar_path" ]] || { log "ERROR: 缺少 $mod 的 jar"; exit 1; }
    eval "JAR_${mod//-/_}=$jar_path"
done

# 2. 备份当前运行版本
BACKUP_DIR="$DEPLOY_ROOT/backup/$(date +%Y%m%d%H%M%S)"
mkdir -p "$BACKUP_DIR"
for mod in "${MODULES[@]}"; do
    src=$(find "$DEPLOY_ROOT" -maxdepth 1 -name "${mod}-*.jar" ! -name "*.original" | head -1)
    if [[ -n "$src" ]]; then
        cp "$src" "$BACKUP_DIR/"
        log "已备份: $(basename "$src")"
    fi
done

# 3. 覆盖新 jar（保持原文件名约定；旧 jar 直接覆盖，备份已在 backup 目录）
log "部署新版本..."
for mod in "${MODULES[@]}"; do
    var="JAR_${mod//-/_}"
    jar_path="${!var}"
    dest="$DEPLOY_ROOT/$(basename "$jar_path")"
    # 同名才覆盖；版本号变化时删除旧版本 jar 避免残留
    for old in $(find "$DEPLOY_ROOT" -maxdepth 1 -name "${mod}-*.jar" ! -name "*.original"); do
        [[ "$old" != "$dest" ]] && rm -f "$old"
    done
    # mv 原子替换：直接 cp 覆盖运行中 jar 会截断文件，懒加载类可能 NoClassDefFoundError
    cp "$jar_path" "$dest.new" && mv -f "$dest.new" "$dest"
    log "已部署: $(basename "$jar_path")"
done

# 4. 重启（互斥机制自动停 newagent）
log "重启 $TARGET（若 newagent/novel-agent 在运行将被 systemd 互斥停止）"
sudo systemctl restart "$TARGET"

# 5. 健康检查（gateway 就绪即代表路由可达；再逐个确认服务单元 active）
check_health() {
    local waited=0
    while (( waited < HEALTH_TIMEOUT_SEC )); do
        if curl -fsS --max-time 3 "$GATEWAY_HEALTH" >/dev/null 2>&1; then
            log "gateway 健康检查通过 (${waited}s)"
            return 0
        fi
        sleep 5; waited=$((waited + 5))
    done
    log "ERROR: gateway ${HEALTH_TIMEOUT_SEC}s 内未就绪"
    return 1
}

rollback() {
    log "回滚到备份版本: $BACKUP_DIR"
    for mod in "${MODULES[@]}"; do
        bak=$(find "$BACKUP_DIR" -maxdepth 1 -name "${mod}-*.jar" ! -name "*.original" | head -1)
        if [[ -n "$bak" ]]; then
            # 先清掉本次部署的坏 jar，再复制回备份
            find "$DEPLOY_ROOT" -maxdepth 1 -name "${mod}-*.jar" ! -name "*.original" -exec rm -f {} \;
            cp "$bak" "$DEPLOY_ROOT/"
        fi
    done
    sudo systemctl restart "$TARGET" || true
    log "已回滚并重启"
}

if ! check_health; then
    # 逐个服务单元确认，便于日志定位
    systemctl --no-pager --failed || true
    journalctl -u "$TARGET" -n 50 --no-pager || true
    rollback
    exit 1
fi

for mod in "${MODULES[@]}"; do
    unit="$(systemctl list-units --type=service --all | grep -oE "[a-z0-9-]*${mod}[a-z0-9-]*\.service" | head -1)"
    if [[ -n "$unit" ]]; then
        state=$(systemctl is-active "$unit" || true)
        log "$unit: $state"
    fi
done

# 6. 清理旧 release（保留最近 3 个备份）
ls -1dt "$DEPLOY_ROOT"/backup/*/ 2>/dev/null | tail -n +4 | xargs -r rm -rf
log "部署完成 ✅"

#!/usr/bin/env bash
# travel 云端部署脚本（服务器侧执行；GitHub Actions 或人工 SSH 均可调用）
#
# 部署形态：裸 jar + systemd（travel-stack.target，5 服务，无 file-service）
# 服务器 jar 命名（实际约定）：gateway.jar / user.jar / attraction.jar / route.jar / collection.jar
# systemd 单元：travel-<短名>.service + travel-stack.target
#
# 用法：./deploy.sh <上传了新 jar 的目录> [部署根目录]   （默认部署根目录 /home/ubuntu/travel）
# 流程：备份当前 jar → mv 原子替换 → 重启 travel-stack.target → 健康检查 → 失败自动回滚
#
# 注意：travel-stack 与 newagent/novel-agent 三方 systemd Conflicts 互斥，
#       重启 travel 会自动停掉正在运行的 newagent —— 触发前请确认当前不需要另一个项目。

set -euo pipefail

STAGING_DIR="${1:?用法: deploy.sh <新jar所在目录> [部署根目录]}"
DEPLOY_ROOT="${2:-/home/ubuntu/travel}"
TARGET="travel-stack.target"
GATEWAY_HEALTH="http://127.0.0.1:8090/actuator/health"
HEALTH_TIMEOUT_SEC=300

# 模块名（Maven artifactId）→ 服务器 jar 短名
declare -A SERVER_JAR=(
    [gateway]=gateway.jar
    [user-service]=user.jar
    [attraction-service]=attraction.jar
    [route-service]=route.jar
    [collection-service]=collection.jar
)
MODULES=(gateway user-service attraction-service route-service collection-service)

log() { echo "[deploy $(date +%H:%M:%S)] $*"; }

# 1. 校验 staging 目录：5 个模块 jar 必须齐全
log "校验新版本产物: $STAGING_DIR"
declare -A STAGED_JAR=()
for mod in "${MODULES[@]}"; do
    jar_path=$(find "$STAGING_DIR" -maxdepth 1 -name "${mod}-*.jar" ! -name "*.original" | head -1)
    [[ -n "$jar_path" ]] || { log "ERROR: 缺少 $mod 的 jar"; exit 1; }
    STAGED_JAR[$mod]="$jar_path"
    log "  ${mod} -> $(basename "$jar_path")"
done

# 2. 备份当前运行版本
BACKUP_DIR="$DEPLOY_ROOT/backup/$(date +%Y%m%d%H%M%S)"
mkdir -p "$BACKUP_DIR"
for mod in "${MODULES[@]}"; do
    src="$DEPLOY_ROOT/${SERVER_JAR[$mod]}"
    if [[ -f "$src" ]]; then
        cp "$src" "$BACKUP_DIR/"
        log "已备份: ${SERVER_JAR[$mod]}"
    else
        log "WARN: 服务器上无 ${SERVER_JAR[$mod]}（首次部署？）"
    fi
done

# 3. mv 原子替换新 jar（直接 cp 覆盖运行中 jar 会截断文件，懒加载类可能 NoClassDefFoundError）
log "部署新版本..."
for mod in "${MODULES[@]}"; do
    dest="$DEPLOY_ROOT/${SERVER_JAR[$mod]}"
    cp "${STAGED_JAR[$mod]}" "$dest.new" && mv -f "$dest.new" "$dest"
    log "已部署: ${SERVER_JAR[$mod]}"
done

# 4. 重启（互斥机制自动停 newagent/novel-agent）
log "重启 $TARGET（若 newagent/novel-agent 在运行将被 systemd 互斥停止）"
sudo systemctl restart "$TARGET"

# 5. 健康检查
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
        bak="$BACKUP_DIR/${SERVER_JAR[$mod]}"
        if [[ -f "$bak" ]]; then
            cp "$bak" "$DEPLOY_ROOT/${SERVER_JAR[$mod]}.new" && \
                mv -f "$DEPLOY_ROOT/${SERVER_JAR[$mod]}.new" "$DEPLOY_ROOT/${SERVER_JAR[$mod]}"
        fi
    done
    sudo systemctl restart "$TARGET" || true
    log "已回滚并重启"
}

if ! check_health; then
    systemctl --no-pager --failed || true
    journalctl -u "$TARGET" -n 50 --no-pager || true
    rollback
    exit 1
fi

for mod in "${MODULES[@]}"; do
    unit="travel-${mod%-service}.service"
    log "$unit: $(systemctl is-active "$unit" || true)"
done

# 6. 清理旧备份（保留最近 3 份）
ls -1dt "$DEPLOY_ROOT"/backup/*/ 2>/dev/null | tail -n +4 | xargs -r rm -rf
log "部署完成"

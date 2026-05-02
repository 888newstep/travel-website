#!/bin/bash

# 健康检查脚本
# 用于检查服务运行状态

APP_NAME="travel"
APP_PORT=8080
HEALTH_URL="http://localhost:${APP_PORT}/api/health"
LOG_FILE="/var/log/travel/health-check.log"

# 创建日志目录
mkdir -p $(dirname $LOG_FILE)

# 日志函数
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a $LOG_FILE
}

# 检查进程
check_process() {
    local pid=$(pgrep -f "${APP_NAME}.*jar")
    if [ -n "$pid" ]; then
        log "INFO: 应用进程正在运行，PID: $pid"
        return 0
    else
        log "ERROR: 应用进程未运行"
        return 1
    fi
}

# 检查端口
check_port() {
    if netstat -tuln | grep -q ":$APP_PORT "; then
        log "INFO: 端口 $APP_PORT 正在监听"
        return 0
    else
        log "ERROR: 端口 $APP_PORT 未监听"
        return 1
    fi
}

# 检查HTTP健康接口
check_http() {
    local response=$(curl -s -o /dev/null -w "%{http_code}" $HEALTH_URL 2>/dev/null)
    if [ "$response" == "200" ]; then
        log "INFO: HTTP健康检查通过 (HTTP $response)"
        return 0
    else
        log "ERROR: HTTP健康检查失败 (HTTP $response)"
        return 1
    fi
}

# 主检查逻辑
main() {
    log "========== 健康检查开始 =========="
    
    local all_ok=true
    
    check_process || all_ok=false
    check_port || all_ok=false
    check_http || all_ok=false
    
    if [ "$all_ok" = true ]; then
        log "========== 健康检查通过 =========="
        exit 0
    else
        log "========== 健康检查失败 =========="
        exit 1
    fi
}

main "$@"

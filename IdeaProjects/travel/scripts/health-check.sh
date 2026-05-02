#!/bin/bash

# 健康检查脚本
# 用于检查服务是否正常运行

APP_NAME="travel"
APP_URL="http://localhost:8080/api/health"
LOG_FILE="/var/log/travel/health-check.log"
MAX_RETRIES=3
RETRY_INTERVAL=5

# 创建日志目录
mkdir -p "$(dirname "$LOG_FILE")"

# 日志函数
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

# 检查HTTP服务
check_http() {
    local url=$1
    local response
    local http_code

    response=$(curl -s -o /dev/null -w "%{http_code}" "$url" 2>/dev/null)
    http_code=$?

    if [ $http_code -eq 0 ] && [ "$response" = "200" ]; then
        return 0
    else
        return 1
    fi
}

# 检查进程
check_process() {
    pgrep -f "$APP_NAME.*jar" > /dev/null 2>&1
    return $?
}

# 主检查逻辑
main() {
    log "开始健康检查..."

    # 检查进程是否存在
    if ! check_process; then
        log "ERROR: 进程不存在"
        exit 1
    fi

    # 检查HTTP接口
    local retries=0
    local success=false

    while [ $retries -lt $MAX_RETRIES ]; do
        if check_http "$APP_URL"; then
            success=true
            break
        fi
        retries=$((retries + 1))
        log "WARN: 健康检查失败，重试 $retries/$MAX_RETRIES"
        sleep $RETRY_INTERVAL
    done

    if [ "$success" = true ]; then
        log "INFO: 健康检查通过"
        exit 0
    else
        log "ERROR: 健康检查失败，服务可能异常"
        exit 1
    fi
}

# 执行主函数
main

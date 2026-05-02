#!/bin/bash

# 健康检查脚本
# 用于检查应用健康状态

APP_URL="${APP_URL:-http://localhost:8080/api/health}"
TIMEOUT="${TIMEOUT:-10}"
LOG_FILE="${LOG_FILE:-/var/log/travel/health-check.log}"

# 日志函数
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

# 检查HTTP服务
check_http() {
    local url=$1
    local timeout=$2
    
    response=$(curl -s -o /dev/null -w "%{http_code}" --max-time "$timeout" "$url" 2>/dev/null)
    
    if [ "$response" = "200" ]; then
        return 0
    else
        return 1
    fi
}

# 检查应用健康状态
check_health() {
    log "正在检查应用健康状态: $APP_URL"
    
    if check_http "$APP_URL" "$TIMEOUT"; then
        log "✓ 应用健康状态正常"
        return 0
    else
        log "✗ 应用健康状态异常"
        return 1
    fi
}

# 检查数据库连接
check_database() {
    log "正在检查数据库连接..."
    
    # 从配置文件中读取数据库信息
    DB_URL=$(grep "spring.datasource.url" /opt/travel/config/application-prod.properties 2>/dev/null | cut -d'=' -f2)
    DB_USER=$(grep "spring.datasource.username" /opt/travel/config/application-prod.properties 2>/dev/null | cut -d'=' -f2)
    DB_PASS=$(grep "spring.datasource.password" /opt/travel/config/application-prod.properties 2>/dev/null | cut -d'=' -f2)
    
    if [ -z "$DB_URL" ]; then
        log "⚠ 无法读取数据库配置，跳过数据库检查"
        return 0
    fi
    
    # 提取主机和端口
    DB_HOST=$(echo "$DB_URL" | sed -n 's/.*@\([^:]*\):.*/\1/p')
    DB_PORT=$(echo "$DB_URL" | sed -n 's/.*:\([0-9]*\)\/.*/\1/p')
    
    if nc -z "$DB_HOST" "$DB_PORT" 2>/dev/null; then
        log "✓ 数据库连接正常"
        return 0
    else
        log "✗ 数据库连接异常"
        return 1
    fi
}

# 检查Redis连接
check_redis() {
    log "正在检查Redis连接..."
    
    REDIS_HOST=$(grep "spring.data.redis.host" /opt/travel/config/application-prod.properties 2>/dev/null | cut -d'=' -f2)
    REDIS_PORT=$(grep "spring.data.redis.port" /opt/travel/config/application-prod.properties 2>/dev/null | cut -d'=' -f2)
    
    if [ -z "$REDIS_HOST" ]; then
        REDIS_HOST="localhost"
    fi
    if [ -z "$REDIS_PORT" ]; then
        REDIS_PORT="6379"
    fi
    
    if nc -z "$REDIS_HOST" "$REDIS_PORT" 2>/dev/null; then
        log "✓ Redis连接正常"
        return 0
    else
        log "✗ Redis连接异常"
        return 1
    fi
}

# 检查磁盘空间
check_disk() {
    log "正在检查磁盘空间..."
    
    # 检查根分区
    usage=$(df -h / | awk 'NR==2 {print $5}' | sed 's/%//')
    
    if [ "$usage" -lt 80 ]; then
        log "✓ 磁盘空间充足 (${usage}%)"
        return 0
    elif [ "$usage" -lt 90 ]; then
        log "⚠ 磁盘空间警告 (${usage}%)"
        return 0
    else
        log "✗ 磁盘空间不足 (${usage}%)"
        return 1
    fi
}

# 检查内存使用
check_memory() {
    log "正在检查内存使用..."
    
    # 获取内存使用率
    mem_usage=$(free | grep Mem | awk '{printf "%.0f", $3/$2 * 100.0}')
    
    if [ "$mem_usage" -lt 80 ]; then
        log "✓ 内存使用正常 (${mem_usage}%)"
        return 0
    elif [ "$mem_usage" -lt 90 ]; then
        log "⚠ 内存使用警告 (${mem_usage}%)"
        return 0
    else
        log "✗ 内存使用过高 (${mem_usage}%)"
        return 1
    fi
}

# 主函数
main() {
    log "========== 开始健康检查 =========="
    
    local exit_code=0
    
    # 执行各项检查
    check_health || exit_code=1
    check_database || exit_code=1
    check_redis || exit_code=1
    check_disk || exit_code=1
    check_memory || exit_code=1
    
    log "========== 健康检查完成 =========="
    
    if [ $exit_code -eq 0 ]; then
        log "✓ 所有检查通过"
    else
        log "✗ 部分检查未通过"
    fi
    
    return $exit_code
}

# 执行主函数
main "$@"

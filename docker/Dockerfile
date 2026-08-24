# 智慧旅游系统 Dockerfile
# 基于OpenJDK 17构建

FROM openjdk:17-jdk-slim

# 设置工作目录
WORKDIR /app

# 安装必要的工具
RUN apt-get update && apt-get install -y \
    curl \
    netcat-traditional \
    && rm -rf /var/lib/apt/lists/*

# 创建日志目录
RUN mkdir -p /app/logs

# 复制JAR文件
COPY target/travel-0.0.1-SNAPSHOT.jar app.jar

# 复制启动脚本
COPY docker/docker-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod +x /app/docker-entrypoint.sh

# 设置时区
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# JVM参数
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/app/logs/heapdump.hprof"

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/api/health || exit 1

# 暴露端口
EXPOSE 8080

# 启动命令
ENTRYPOINT ["/app/docker-entrypoint.sh"]

FROM maven:3.9.9-eclipse-temurin-17 AS builder

ARG MODULE_NAME

WORKDIR /workspace

COPY backend backend

RUN mvn -f backend/pom.xml -pl ${MODULE_NAME} -am -DskipTests package

FROM eclipse-temurin:17-jre-alpine

ARG MODULE_NAME
ARG APP_PORT
ARG HEALTH_PATH

ENV TZ=Asia/Shanghai
ENV APP_PORT=${APP_PORT}
ENV HEALTH_PATH=${HEALTH_PATH}
ENV JAVA_OPTS="-Xms256m -Xmx768m -XX:+UseContainerSupport -XX:+UseG1GC"

WORKDIR /app

RUN apk add --no-cache curl tzdata && \
    ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone && \
    addgroup -S travel && adduser -S travel -G travel && \
    mkdir -p /app/logs && chown -R travel:travel /app

COPY --from=builder /workspace/backend/${MODULE_NAME}/target/*.jar app.jar

USER travel

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=5 \
    CMD curl -fsS "http://localhost:${APP_PORT}${HEALTH_PATH}" || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

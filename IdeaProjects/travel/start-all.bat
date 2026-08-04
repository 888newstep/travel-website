@echo off
chcp 65001 >nul
echo ========================================
echo   智慧旅游系统 - 一键启动脚本
echo ========================================
echo.

REM 加载环境变量
if exist deploy\.env (
    echo [1/6] 加载环境配置...
    for /f "usebackq tokens=1,* delims==" %%a in ("deploy\.env") do (
        if "%%b" neq "" (
            set "%%a=%%b"
        )
    )
) else (
    echo [警告] 未找到 deploy\.env 配置文件
    echo 请复制 deploy\.env.example 为 deploy\.env 并配置
    pause
    exit /b 1
)

echo [2/6] 检查 Nacos 服务...
curl -s http://localhost:8848/nacos/v1/console/health/readiness >nul 2>&1
if errorlevel 1 (
    echo [错误] Nacos 未启动，请先运行 nacos\bin\startup.cmd -m standalone
    pause
    exit /b 1
)
echo ✓ Nacos 运行正常

echo [3/6] 检查 MySQL 服务...
sc query MySQL80 | find "RUNNING" >nul
if errorlevel 1 (
    echo [错误] MySQL 服务未运行
    pause
    exit /b 1
)
echo ✓ MySQL 运行正常

echo [4/6] 检查 Redis 服务...
sc query redis6379 | find "RUNNING" >nul
if errorlevel 1 (
    echo [警告] Redis 服务未运行，部分功能可能受影响
) else (
    echo ✓ Redis 运行正常
)

echo [5/6] 编译后端项目...
cd backend
call mvn clean package -DskipTests
if errorlevel 1 (
    echo [错误] 编译失败
    cd ..
    pause
    exit /b 1
)
cd ..
echo ✓ 编译成功

echo [6/6] 启动微服务...
echo.
echo 正在启动服务，请稍候...
echo.

REM 启动各个微服务（后台运行）
start "User Service" java -jar backend\user-service\target\user-service-1.0-SNAPSHOT.jar
timeout /t 5 /nobreak >nul

start "Attraction Service" java -jar backend\attraction-service\target\attraction-service-1.0-SNAPSHOT.jar
timeout /t 5 /nobreak >nul

start "Route Service" java -jar backend\route-service\target\route-service-1.0-SNAPSHOT.jar
timeout /t 5 /nobreak >nul

start "Collection Service" java -jar backend\collection-service\target\collection-service-1.0-SNAPSHOT.jar
timeout /t 5 /nobreak >nul

start "File Service" java -jar backend\file-service\target\file-service-1.0-SNAPSHOT.jar
timeout /t 5 /nobreak >nul

start "Gateway" java -jar backend\gateway\target\gateway-1.0-SNAPSHOT.jar
timeout /t 10 /nobreak >nul

echo.
echo ========================================
echo   所有服务已启动！
echo ========================================
echo.
echo Nacos 控制台: http://localhost:8848/nacos
echo   默认账号: nacos / nacos
echo.
echo 微服务端口:
echo   - User Service:       8091
echo   - Attraction Service: 8092
echo   - Route Service:      8093
echo   - Collection Service: 8094
echo   - File Service:       8095
echo   - Gateway:            8090
echo.
echo 前端开发服务器:
echo   cd frontend ^&^& npm run dev
echo   访问: http://localhost:3000
echo.
echo 按任意键查看服务状态...
pause >nul

echo.
echo 检查服务状态...
curl -s http://localhost:8090/actuator/health >nul 2>&1
if errorlevel 1 (
    echo [警告] Gateway 可能还在启动中，请稍等片刻
) else (
    echo ✓ Gateway 运行正常
)

echo.
pause
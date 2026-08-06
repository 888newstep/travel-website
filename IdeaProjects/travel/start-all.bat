@echo off
chcp 65001 >nul
set NACOS_HOME=backend\nacos\nacos
set NACOS_STARTUP=%NACOS_HOME%\bin\startup.cmd
echo ========================================
echo   鏅烘収鏃呮父绯荤粺 - 涓€閿惎鍔ㄨ剼鏈?
echo ========================================
echo.

REM 鍔犺浇鐜鍙橀噺
if exist deploy\.env (
    echo [1/6] 鍔犺浇鐜閰嶇疆...
    for /f "usebackq tokens=1,* delims==" %%a in ("deploy\.env") do (
        if "%%b" neq "" (
            set "%%a=%%b"
        )
    )
) else (
    echo [璀﹀憡] 鏈壘鍒?deploy\.env 閰嶇疆鏂囦欢
    echo 璇峰鍒?deploy\.env.example 涓?deploy\.env 骞堕厤缃?
    pause
    exit /b 1
)

echo [2/6] 妫€鏌?Nacos 鏈嶅姟...
curl -s http://localhost:8848/nacos/v1/console/health/readiness >nul 2>&1
if errorlevel 1 (
    if not exist "%NACOS_STARTUP%" (
        echo [ERROR] Missing Nacos startup script: %NACOS_STARTUP%
        pause
        exit /b 1
    )
    echo [INFO] Nacos not ready, starting from %NACOS_STARTUP% ...
    start "Nacos" "%NACOS_STARTUP%" -m standalone
    set /a NACOS_WAIT_COUNT=0
    :wait_nacos
    timeout /t 2 /nobreak >nul
    curl -s http://localhost:8848/nacos/v1/console/health/readiness >nul 2>&1
    if not errorlevel 1 goto nacos_ready
    set /a NACOS_WAIT_COUNT+=1
    if %NACOS_WAIT_COUNT% GEQ 20 (
        echo [ERROR] Nacos startup timed out: %NACOS_STARTUP%
        pause
        exit /b 1
    )
    goto wait_nacos
)
echo 鉁?Nacos 杩愯姝ｅ父

:nacos_ready
echo [3/6] 妫€鏌?MySQL 鏈嶅姟...
sc query MySQL80 | find "RUNNING" >nul
if errorlevel 1 (
    echo [閿欒] MySQL 鏈嶅姟鏈繍琛?
    pause
    exit /b 1
)
echo 鉁?MySQL 杩愯姝ｅ父

echo [4/6] 妫€鏌?Redis 鏈嶅姟...
sc query redis6379 | find "RUNNING" >nul
if errorlevel 1 (
    echo [璀﹀憡] Redis 鏈嶅姟鏈繍琛岋紝閮ㄥ垎鍔熻兘鍙兘鍙楀奖鍝?
) else (
    echo 鉁?Redis 杩愯姝ｅ父
)

echo [5/6] 缂栬瘧鍚庣椤圭洰...
cd backend
call mvn clean package -DskipTests
if errorlevel 1 (
    echo [閿欒] 缂栬瘧澶辫触
    cd ..
    pause
    exit /b 1
)
cd ..
echo 鉁?缂栬瘧鎴愬姛

echo [6/6] 鍚姩寰湇鍔?..
echo.
echo 姝ｅ湪鍚姩鏈嶅姟锛岃绋嶅€?..
echo.

REM 鍚姩鍚勪釜寰湇鍔★紙鍚庡彴杩愯锛?
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
echo   鎵€鏈夋湇鍔″凡鍚姩锛?
echo ========================================
echo.
echo Nacos 鎺у埗鍙? http://localhost:8848/nacos
echo   榛樿璐﹀彿: nacos / nacos
echo.
echo 寰湇鍔＄鍙?
echo   - User Service:       8091
echo   - Attraction Service: 8092
echo   - Route Service:      8093
echo   - Collection Service: 8094
echo   - File Service:       8095
echo   - Gateway:            8090
echo.
echo 鍓嶇寮€鍙戞湇鍔″櫒:
echo   cd frontend ^&^& npm run dev
echo   璁块棶: http://localhost:3000
echo.
echo 鎸変换鎰忛敭鏌ョ湅鏈嶅姟鐘舵€?..
pause >nul

echo.
echo 妫€鏌ユ湇鍔＄姸鎬?..
curl -s http://localhost:8090/actuator/health >nul 2>&1
if errorlevel 1 (
    echo [璀﹀憡] Gateway 鍙兘杩樺湪鍚姩涓紝璇风◢绛夌墖鍒?
) else (
    echo 鉁?Gateway 杩愯姝ｅ父
)

echo.
pause

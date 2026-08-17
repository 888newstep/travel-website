@echo off
chcp 65001 >nul
echo ========================================
echo   停止所有服务
echo ========================================
echo.

echo 正在停止微服务...

REM 查找并停止所有 Java 进程（排除 Nacos）
for /f "tokens=2" %%a in ('tasklist /fi "imagename eq java.exe" /nh ^| findstr /v "nacos"') do (
    echo 停止进程 %%a
    taskkill /pid %%a /f >nul 2>&1
)

echo.
echo [OK] 微服务已停止
echo.
echo 如需停止 Nacos，请运行: backend\nacos\nacos\bin\shutdown.cmd
echo.
pause
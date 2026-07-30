@echo off
chcp 65001 >nul
title SysON 一键启动

echo ========================================
echo       SysON 启动中，请稍候...
echo ========================================
echo.

:: ========== 1. 启动 Docker PostgreSQL ==========
echo [1/3] 启动 PostgreSQL 容器...
docker start syson-postgres 2>nul
if %errorlevel% equ 0 (
    echo   ✓ PostgreSQL 容器已启动
) else (
    echo   × PostgreSQL 容器启动失败，请先执行：
    echo     docker run -d --name syson-postgres ^
    echo       -e POSTGRES_USER=username ^
    echo       -e POSTGRES_PASSWORD=root ^
    echo       -e POSTGRES_DB=postgres ^
    echo       -p 5555:5432 postgres:15
    pause
    exit /b 1
)

:: ========== 2. 启动后端（新窗口）==========
echo [2/3] 启动后端（8080端口，新窗口...
start "SysON-Backend" cmd /c "cd /d F:\WorkSpace\AIWorkSpace\syson-ds && set SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5555/postgres&& set SPRING_DATASOURCE_USERNAME=username&& set SPRING_DATASOURCE_PASSWORD=root&& mvn spring-boot:run -Dcheckstyle.skip=true -Dspring.liquibase.enabled=false -pl backend/application/syson-application"

echo   ✓ 后端窗口已打开，首次启动需约 3 分钟
echo     日志输出在 SysON-Backend 窗口中

:: ========== 3. 启动前端（新窗口）==========
echo [3/3] 启动前端（5173端口，新窗口...
start "SysON-Frontend" cmd /c "cd /d F:\WorkSpace\AIWorkSpace\syson-ds\frontend\syson && npm start"

echo   ✓ 前端窗口已打开

echo ========================================
echo 全部启动命令已执行
echo.
echo   前端: http://localhost:5173
echo   后端: http://localhost:8080
echo.
echo 首次启动请等待后端窗口出现 "Started SysONApplication"
echo ========================================
echo.
pause

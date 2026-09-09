@echo off
chcp 65001 >nul
cd /d "%~dp0"
title AI 模拟面试平台 - 后端服务

echo ============================================
echo   AI 在线模拟面试平台
echo   模式: H2 文件库 + Mock AI (零外部依赖)
echo ============================================
echo.

where java >nul 2>nul
if errorlevel 1 (
    echo [错误] 未检测到 java，请先安装 JDK 17 并配置 PATH
    pause
    exit /b 1
)

if not exist "target\ai-interview-server-1.0.0.jar" (
    echo [错误] 未找到 target\ai-interview-server-1.0.0.jar
    echo 请先执行构建:
    echo   mvn.cmd -B -DskipTests package
    pause
    exit /b 1
)

echo 正在启动服务，请稍候...
echo 启动后访问: http://localhost:8080
echo.
echo 账号: admin / admin123    ^(管理员^)
echo       demo  / demo1234    ^(普通用户^)
echo.
echo --------------------------------------------
echo 提示: 若浏览器开了系统代理(如 Clash)，需在代理
echo       设置中放行 localhost，否则页面打不开。
echo 关闭本窗口即可停止服务。
echo --------------------------------------------
echo.

java -jar target\ai-interview-server-1.0.0.jar --spring.profiles.active=h2 --ai-interview.ai.provider=mock --server.port=8080

pause

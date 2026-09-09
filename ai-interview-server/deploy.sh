#!/usr/bin/env bash
#
# ai-interview 一键部署脚本（零外部依赖形态）
# ------------------------------------------------------------
# 前置条件：服务器已安装 Java 17（openjdk-17-jdk）。
# 本脚本以「H2 文件库 + Mock 评分引擎」启动，完全不依赖 MySQL 与外网 LLM，
# 单机即可提供「前端页面 + 后端 API + 流式评分」的完整闭环。
#
# 用法：  bash deploy.sh
# 说明：  脚本会优先停止 8080 端口上已有的本应用进程，再用 nohup 启动新进程。
#
set -e

APP_NAME="ai-interview"
APP_PORT=8080
DEPLOY_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "==> 部署目录: $DEPLOY_DIR"

# 1) 定位 jar（优先当前目录，其次 target/）
JAR=""
if [ -f "$DEPLOY_DIR/ai-interview-server-1.0.0.jar" ]; then
  JAR="$DEPLOY_DIR/ai-interview-server-1.0.0.jar"
elif [ -f "$DEPLOY_DIR/target/ai-interview-server-1.0.0.jar" ]; then
  JAR="$DEPLOY_DIR/target/ai-interview-server-1.0.0.jar"
fi
if [ -z "$JAR" ]; then
  echo "✗ 未找到 ai-interview-server-1.0.0.jar，请先将该 jar 与 deploy.sh 放在同一目录"
  exit 1
fi
echo "==> 使用 jar: $JAR"

# 2) 检查 Java
if ! command -v java >/dev/null 2>&1; then
  echo "✗ 未检测到 java，请先执行: sudo apt update && sudo apt install -y openjdk-17-jdk"
  exit 1
fi
echo "==> java 版本: $(java -version 2>&1 | head -1)"

# 3) 停止同端口旧进程（若存在）
OLD_PID=$(ss -ltnp 2>/dev/null | grep ":$APP_PORT " | grep -o 'pid=[0-9]*' | head -1 | cut -d= -f2 || true)
if [ -n "$OLD_PID" ]; then
  echo "==> 停止旧进程 pid=$OLD_PID"
  kill "$OLD_PID" 2>/dev/null || true
  sleep 3
fi

# 4) 生成 JWT 密钥（仅首次）
if [ ! -f "$DEPLOY_DIR/.env.local" ]; then
  JWT_SECRET=$(openssl rand -base64 48 2>/dev/null | tr -d '\n' || head -c 64 /dev/urandom | base64)
  echo "AI_INTERVIEW_JWT_SECRET=$JWT_SECRET" > "$DEPLOY_DIR/.env.local"
  echo "==> 已生成 JWT 密钥并写入 .env.local"
fi
JWT_SECRET=$(grep AI_INTERVIEW_JWT_SECRET "$DEPLOY_DIR/.env.local" | cut -d= -f2-)

# 5) 启动（H2 + Mock 评分，零外部依赖）
LOG="$DEPLOY_DIR/app.log"
nohup setsid java -jar "$JAR" \
  --server.port=$APP_PORT \
  --spring.profiles.active=h2 \
  --ai-interview.ai.provider=deepseek \
  --ai-interview.security.jwt-secret="$JWT_SECRET" \
  > "$LOG" 2>&1 &

echo "==> 已启动，日志: $LOG"
echo "==> 等待服务就绪..."

# 6) 健康检查（最多 30 次，每次 2s）
READY=0
for i in $(seq 1 30); do
  if curl -s -m3 "http://localhost:$APP_PORT/api/health" >/dev/null 2>&1; then
    READY=1
    break
  fi
  sleep 2
done

if [ "$READY" -eq 1 ]; then
  echo "✓ 应用已启动"
  echo "    首页:    http://<服务器IP>:$APP_PORT/"
  echo "    接口文档: http://<服务器IP>:$APP_PORT/swagger-ui.html"
  echo "    健康检查: http://<服务器IP>:$APP_PORT/api/health"
  echo "    初始账号: admin / admin123 (管理员)    demo / demo1234 (演示用户)"
else
  echo "✗ 启动超时，请查看日志: $LOG"
  tail -n 30 "$LOG"
  exit 1
fi

# 7) 放通本机防火墙（云厂商安全组需另行在控制台放行 TCP $APP_PORT）
if command -v ufw >/dev/null 2>&1; then
  sudo ufw allow $APP_PORT/tcp >/dev/null 2>&1 || true
  echo "==> 已尝试放通本机 ufw $APP_PORT/tcp（若需 sudo 请手动执行）"
fi
echo "==> 提示: 腾讯云/阿里云等还需在「安全组」控制台放行入站 TCP $APP_PORT"

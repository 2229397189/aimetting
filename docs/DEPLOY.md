# 部署手册（AI 在线模拟面试平台）

> **部署方式 = 一键脚本 `deploy.sh`**，不是裸 `nohup java -jar`。
> 本文口径全部来自仓库既有脚本：`ai-interview-server/deploy.sh`、`smoke_test.sh`、`verify.sh`。
>
> 目标机：腾讯云轻量 `81.70.147.211`（Ubuntu，用户 `ubuntu`）。80 端口被 Nginx 占用 → 本项目跑 **8080**。
> 该机未配置 scp/ssh 密钥，**上传只能走 OrcaTerm WebShell 的「文件上传」**。

---

## 0. 部署形态（零中间件）

- 运行形态：**H2 文件库 + 真实 DeepSeek 评分**，单机无需 MySQL / Redis / MQ。
- 前端已由 pom 的 `copy-frontend-dist` 打进 jar，**不需要单独部署前端**。
- 数据落地在部署目录下的 `./data/aimeeting.mv.db`（H2），重启保留。

---

## 1. 服务器前置

| 项 | 说明 |
| --- | --- |
| JDK 17 | `sudo apt update && sudo apt install -y openjdk-17-jdk`（`deploy.sh` 会自检 `java`） |
| 本机 ufw | `deploy.sh` 会自动尝试 `sudo ufw allow 8080/tcp` |
| **云安全组** | **必须去腾讯云控制台放行入站 TCP 8080**（脚本管不到安全组） |

---

## 2. 本地构建产物

```bash
# 前端（vite 清 dist 会被安全守卫拦，必要时先改名旧 dist）
cd ai-interview-web && npm run build -- --outDir dist-tmp

# 后端（必须 clean：只改 static 时 `mvn package` 不会重打 jar）
cd ../ai-interview-server
"D:\develop\apache-maven-3.9.15\bin\mvn.cmd" -B -Dfile.encoding=UTF-8 clean package
```

产物：`ai-interview-server/target/ai-interview-server-1.0.0.jar`
> 坑：`mvn clean` 若因本地 java 进程占用 `target/*.jar` 失败，先停本地实例再打包。

---

## 3. 上传（OrcaTerm WebShell）

把下面两个文件**放到服务器同一个目录**（例如 `~/ai-interview/`）——OrcaTerm 支持拖拽上传：

1. `ai-interview-server-1.0.0.jar`
2. `deploy.sh`

（可选，用于上线后验证：`smoke_test.sh`）

> `deploy.sh` 先找**当前目录**的 jar，找不到再找 `target/` —— 所以 jar 与脚本**必须同目录**。

---

## 4. 一键部署

```bash
cd ~/ai-interview
bash deploy.sh
```

`deploy.sh` 依次执行：

1. 定位 jar、自检 `java`
2. 停掉 8080 端口上已有的本应用进程（`ss -ltnp` + `kill`）
3. 生成 / 读取 `.env.local`（**不进版本库**）：
   - `AI_INTERVIEW_JWT_SECRET=<openssl 随机 48 字节>`
   - `AI_INTERVIEW_AI_API_KEY=`（**需你手填**）
4. `set -a; . .env.local; set +a` 导出环境变量
   - ⚠️ **空值会被 `unset`**，回落到 jar 内置 key 并打警告；
     若留成空字符串则会被绑定为 `apiKey=""` → `isMockMode()=true` → **整站静默降级为规则评分**（HTTP 仍 200，看不出来）
5. 启动：

   ```bash
   nohup setsid java -jar ai-interview-server-1.0.0.jar \
     --server.port=8080 \
     --spring.profiles.active=h2 \
     --ai-interview.ai.provider=deepseek \
     --ai-interview.jwt.secret="$JWT_SECRET" > app.log 2>&1 &
   ```

6. 轮询 `http://localhost:8080/api/health`（最多 30 次 × 2s）
7. 尝试 `ufw allow 8080/tcp`

---

## 5. 配置项（**不要再用错变量名**）

| 用途 | 环境变量 | 绑定位置（application.yml） |
| --- | --- | --- |
| DeepSeek Key | **`AI_INTERVIEW_AI_API_KEY`** | `ai-interview.ai.api-key` |
| JWT 密钥 | **`AI_INTERVIEW_JWT_SECRET`** | `ai-interview.jwt.secret` |
| LLM base url | `AI_INTERVIEW_AI_BASE_URL`（默认 `https://api.deepseek.com`） | `ai-interview.ai.base-url` |

- ⚠️ **不是** `DEEPSEEK_API_KEY`。
- 填 Key：编辑 `~/ai-interview/.env.local` 的 `AI_INTERVIEW_AI_API_KEY=sk-xxx`，再重跑 `bash deploy.sh`。
- ⚠️ 仓库 `application.yml` 里带内置 DeepSeek key（已进 git、**已泄露**）→ 生产必须用 `.env.local` 覆盖并轮换。
- ⚠️ JWT 必须由 `.env.local` 注入：早期 `deploy.sh` 传错配置键，随机密钥从未生效，线上一直用 yml 默认公开密钥（可伪造任意用户 token）——现已修正，请确认升级到当前脚本。

---

## 6. 访问与初始账号

- 首页：`http://81.70.147.211:8080/`
- 健康：`http://81.70.147.211:8080/api/health`
- 接口文档：`http://81.70.147.211:8080/swagger-ui.html`
- 初始账号：`admin / admin123`（管理员）、`demo / demo1234`（演示用户）

---

## 7. 上线验证（推荐）

```bash
cd ~/ai-interview
bash smoke_test.sh        # 48 项全功能冒烟（真实 DeepSeek 评分），需服务已启动
```

- 只看 HTTP 200 **不够**：用管理员 token 调 `/api/admin/ai/health`，确认 `available:true`；
  若 `mockMode:true` → Key 没生效，评分是规则兜底。
- 前端是否最新：`curl -s localhost:8080/ | grep -oE 'assets/index-[A-Za-z0-9_-]+\.js'`
  与本地 `ai-interview-web/dist/assets/index-*.js` 的 hash 比对。

---

## 8. 更新发布

本地 `clean package` → 重新上传覆盖 jar → `bash deploy.sh`（自动停旧进程、健康检查）。

---

## 9. 排错

| 现象 | 处置 |
| --- | --- |
| 起不来 / 超时 | `tail -n 50 ~/ai-interview/app.log` |
| 评分是假的（HTTP 200 但没真评分） | 查 `/api/admin/ai/health` 的 `mockMode`；日志里 `AiProviderFactory` 输出 |
| 页面还是旧的 | 比对前端 hash；确认打包用了 **clean package** |
| 外网打不开 | 安全组放行 TCP 8080 + `ufw allow 8080/tcp` |

> 备注：该机器免费期至 **2026-10-08**，到期前需迁移或续费。

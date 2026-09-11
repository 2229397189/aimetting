# 部署手册（AI 在线模拟面试平台）

部署目标：腾讯云轻量应用服务器 `81.70.147.211:8080`（OrcaTerm WebShell 管理）。
产物：`ai-interview-server/target/ai-interview-server-1.0.0.jar`（前端 `dist` 已打进 jar 的 `static/`，无需单独部署前端）。

---

## 1. 服务器前置要求

| 项 | 说明 |
| --- | --- |
| JDK | 17（运行 `java -jar` 需要；`java -version` 确认） |
| 防火墙 | 放通 **TCP 8080**（云平台防火墙 + 系统 firewall 都要放行） |
| 依赖中间件 | **无**。默认 H2 文件库，开箱即用；如需 MySQL 见 §5 |
| DeepSeek Key | 见 §4，缺省会进入 **Mock 模式**（返回假评分，仅用于演示） |

---

## 2. 本地构建产物（在你自己机器上）

```bash
# 1) 构建前端产物到 dist（绕过 WorkBuddy 安全删除对 vite 清 dist 的拦截）
cd ai-interview-web
npm install
npm run build -- --outDir dist-tmp
# 把产物拷贝到后端静态资源目录（pom 的 copy-frontend-dist 读 ../ai-interview-web/dist，二选一）
mkdir -p ../ai-interview-server/src/main/resources/static
cp -r dist-tmp/* ../ai-interview-server/src/main/resources/static/

# 2) 打包 jar（含后端 + 前端 static）
cd ../ai-interview-server
"D:\develop\apache-maven-3.9.15\bin\mvn.cmd" -B -Dfile.encoding=UTF-8 clean package
# 产物：target/ai-interview-server-1.0.0.jar
```

> 说明：`mvn package` 中的 `copy-frontend-dist` 阶段会从 `../ai-interview-web/dist` 复制前端；
> 若你用 `dist-tmp` 命名，请手动复制到 `src/main/resources/static/`（如上），否则 jar 内静态页是旧的。

---

## 3. 上传到服务器（OrcaTerm WebShell）

1. 打开 OrcaTerm，进入 `81.70.147.211` 实例的 WebShell。
2. 上传 `ai-interview-server-1.0.0.jar` 到 `/opt/aimeeting/`（`/root/aimeeting/` 亦可，自行统一）。
   - OrcaTerm 支持拖拽上传，或用 WebShell 内 `rz` 命令。
3. 确认文件大小与本地一致：`ls -lh ai-interview-server-1.0.0.jar`。

---

## 4. 配置 DeepSeek Key（关键，否则评分是假的）

应用读取顺序：`环境变量 DEEPSEEK_API_KEY` > `application.yml` 里的 `ai.deepseek.api-key`。
**推荐用环境变量**，不要把 key 写进仓库。

```bash
# 在启动命令前注入（生产务必用真实 key）
export DEEPSEEK_API_KEY="sk-xxxxxxxxxxxxxxxxxxxxxxxx"
```

- 若 key 为空：日志会打印 `使用真实供应商` 的反面（Mock），`AiProperties.isMockMode()==true`，
  评估/报告接口返回**编造的示例分数**，仅供联调，面试数据不可信。
- 重建 key：DeepSeek 开放平台 → API Keys → 删除旧 key、新建，取得新 `sk-...` 后更新上面环境变量。

---

## 5. 启动

### 5.1 零中间件（默认，H2 文件库）
```bash
cd /opt/aimeeting
export DEEPSEEK_API_KEY="sk-xxxxxxxx"
nohup java -jar ai-interview-server-1.0.0.jar > app.log 2>&1 &
```
- 数据落在运行目录的 `./data/aimeeting.mv.db`（H2）。重启保留。
- 端口 8080，上下文路径 `/`。

### 5.2 使用 MySQL（可选）
```bash
export DEEPSEEK_API_KEY="sk-xxxxxxxx"
nohup java -jar ai-interview-server-1.0.0.jar \
  --spring.profiles.active=mysql \
  --spring.datasource.url=jdbc:mysql://127.0.0.1:3306/aimeeting \
  --spring.datasource.username=xxx --spring.datasource.password=xxx > app.log 2>&1 &
```
- 表结构见 `src/main/resources/db/schema-mysql.sql`，首次启动由 JPA/H2 初始化策略或手动执行。

---

## 6. 健康检查与验证

```bash
# 进程
ps aux | grep ai-interview-server | grep -v grep

# 端口
ss -lntp | grep 8080

# 接口冒烟（返回 HTML 即前端已打进 jar）
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/

# 注册冒烟
curl -s -X POST http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"username":"demo","password":"Demo1234","email":"demo@test.com"}'
```

日志排错：`tail -f app.log`。关注 `AiProviderFactory` 行确认是 `deepseek` 还是 `mock`。

---

## 7. 更新发布流程（日常）

1. 本地按 §2 重新 `mvn clean package`。
2. 上传新 jar 覆盖 `/opt/aimeeting/` 旧文件。
3. `kill <旧pid>` 然后重新 `nohup java -jar ... &`。
4. 按 §6 做健康检查。

> 提示：H2 文件库在 jar 运行时被占用，更新前请先停进程再覆盖 jar，避免锁文件损坏。

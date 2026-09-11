# AI 在线模拟面试平台（aimeeting）

> 上传简历 → 选方向/难度 → 逐题作答 → **AI 实时流式评分与追问** → 生成**五维能力报告**。
>
> 一个前后端一体、零中间件依赖（无需 Redis / MQ）的 AI 模拟面试系统，主打「**拷打型**」面试官风格：不放过任何没有证据的表述，并对简历中的项目/实习经历做**真实性判定**。

---

## 目录

- [核心特性](#核心特性)
- [技术栈](#技术栈)
- [快速开始](#快速开始)
- [目录结构](#目录结构)
- [关键设计](#关键设计)
- [配置项](#配置项)
- [接口文档](#接口文档)
- [测试](#测试)
- [部署](#部署)
- [许可证](#许可证)

---

## 核心特性

### 🎙 拷打型 AI 面试官
- 人设对标一线大厂技术面：节奏快、措辞直接、不粉饰。
- **不放过没有证据的表述**——回答停留在「背概念」层面会被持续追问，直到答透才收手。
- **简历真实性拷打**：若候选人提供了简历，优先挑其项目/实习经历中最「亮眼」的 1–2 条深挖，按 STAR/CAR 做深度测试；说不出人名、数据、时间、流程，或被自己写的技术栈问住，则在 `authenticity` 维度大幅扣分并给出「该经历真实性存疑」判定。

### ⚡ 实时流式面试（SSE）
- 基于 SSE 的 7 类事件流式推送：`question` / `score` / `comment` / `follow_up` / `progress` / `done` / `error`。
- 边生成边渲染，答题体验接近真实对话；连接断开时服务端能正确感知并抛错，不静默吞异常。

### 📊 五维能力报告
- 五维雷达图：`PROFESSIONAL`（专业能力）、`EXPRESSION`（表达）、`LOGIC`（逻辑）、`PROJECT_DEPTH`（项目深度）、`POTENTIAL`（潜力）。
- 含总分、逐题点评、亮点（≥2）、改进点（≥3）、可执行下一步建议（≥3）。
- 报告生成**幂等**（重复触发只生成一条）；AI 失败时自动降级为规则生成（`generatedBy=RULE`），仍产出完整报告。
- 支持 Markdown 导出。

### 📄 简历解析与容错
- 支持 **TXT / MD / DOCX / PDF**（PDF 使用 Apache PDFBox）。
- DOCX 走标准库 zip + XML 解析 `word/document.xml`；PDF 优先 PDFBox，空结果回退内容流启发式。
- **扫描件、图片型、加密、损坏**文件会给出清晰可读的错误提示，而不是回传二进制乱码被误判为「不是简历」。

### 📚 内置题库 2000+
- **2064 条**题目，覆盖 **8 个方向** × **3 档难度**：Java 后端 / 前端 / 数据库 / 操作系统 / 计算机网络 / 算法 / 系统设计 / 行为面试。
- 由 `tools/gen_question_seed.py` 生成，启动时由 `QuestionSeeder` **按 title 幂等 upsert**，重复启动不会灌重复数据。

### 🛡 AI 调用防护链（AiGuard）
单次 AI 调用串联六层保护，任何一层失效都不会拖垮整体：

```
single-flight 去重 → 限流 → 熔断 → 舱壁(并发) → 超时 → 重试(指数退避 + Equal Jitter)
```

- **single-flight**：同 key 并发请求只放行一次真实调用，其余共享结果（50 并发实测仅 1 次真实调用）。
- **熔断**：滑动窗口 20、失败率 50% 触发，开启 30s 后进入半开。
- **舱壁**：并发上限 20，超限返回 `AI_BUSY` 而非无限堆积。
- **重试**：瞬时错误指数退避 + Equal Jitter 抖动；**网络超时不重试**（maxRetries=0），避免雪崩。

### 🧱 工程特性
- **零中间件**：不依赖 Redis / MQ，缓存、锁、single-flight、幂等全部基于 Caffeine + JVM 实现。
- **单 jar 前后端一体**：前端构建产物在 `prepare-package` 阶段被打进 jar，一条 `java -jar` 即可提供完整服务。
- **统一返回体 + 全局异常**：错误码语义化（如 `C0504` 网络超时、`AI_BUSY`、`RATE_LIMITED`）。
- **AI 密钥不落代码**：通过环境变量注入，未注入时回落到内置开发 key 并提示轮换。

---

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Java 17、Spring Boot 3.2.5、MyBatis-Plus 3.5.7、Spring Security + JWT |
| 数据库 | MySQL 8（默认）/ H2（零中间件场景） |
| 文档解析 | Apache PDFBox 3.0.2、标准库 Zip/XML（DOCX） |
| 缓存与并发 | Caffeine、JVM 原生并发原语 |
| 接口文档 | springdoc-openapi（Swagger UI） |
| 前端 | Vue 3 + TypeScript、Vite 5、Pinia、Vue Router 4、Element Plus、ECharts 5、Axios、markdown-it |
| AI | DeepSeek（OpenAI 协议兼容，可换 base-url） |

---

## 快速开始

### 环境要求

| 组件 | 版本 |
|---|---|
| JDK | 17+ |
| Maven | 3.9+（**Windows 下请用 `mvn.cmd`**，Git Bash 的 `mvn` 脚本不可用） |
| Node | 18+（推荐 22） |

### 1. 配置 AI 密钥

通过环境变量注入（**不要写进代码**）：

```bash
export AI_INTERVIEW_AI_API_KEY="sk-你的DeepSeekKey"
export AI_INTERVIEW_JWT_SECRET="至少32字节的随机字符串"
export AI_INTERVIEW_AI_BASE_URL="https://api.deepseek.com"   # 可选，默认值即此项
```

> ⚠️ 未注入 `AI_INTERVIEW_AI_API_KEY` 时系统会回落到内置开发 key 并以**规则降级评分**运行（HTTP 仍返回 200，容易误判为「AI 正常」）。请务必注入自己的 key。

### 2. 本地开发（前后端分离）

```bash
# 后端（8080），零中间件可用 h2 profile
cd ai-interview-server
mvn.cmd -Dfile.encoding=UTF-8 spring-boot:run -Dspring-boot.run.profiles=h2

# 前端（5173，/api 已代理到 8080）
cd ai-interview-web
npm install
npm run dev
```

### 3. 打包运行（推荐，前端自动打进 jar）

```bash
cd ai-interview-web && npm run build
cd ../ai-interview-server && mvn.cmd -Dfile.encoding=UTF-8 package
java -jar target/ai-interview-server-1.0.0.jar
```

浏览器打开 <http://localhost:8080> 即可使用（前后端同端口，无需 Nginx）。

### 4. 一键部署（Linux 服务器）

```bash
bash deploy.sh
```

`deploy.sh` 会自动完成：定位 jar → 自检 Java → 停止 8080 旧进程 → 读取/生成 `.env.local` → 以 `h2 + deepseek` 启动 → 健康检查 `/api/health` → 放通防火墙。

**内置演示账号**：`admin / admin123`、`demo / demo1234`。

---

## 目录结构

```
aimeeting/
├── ai-interview-server/        # 后端（Spring Boot）
│   ├── src/main/java/com/aimeeting/interview/
│   │   ├── auth/               # 认证授权、JWT、用户
│   │   ├── question/           # 题库、方向/难度枚举、Seeder
│   │   ├── ai/                 # AI 接入层、AiGuard 防护链、多 Agent 框架
│   │   ├── interview/          # 面试会话核心（SSE、工作流引擎、评分、追问）
│   │   ├── resume/             # 简历上传与文本抽取
│   │   ├── report/             # 五维报告生成与导出
│   │   └── common/             # 统一返回体、异常、错误码、幂等
│   ├── deploy.sh               # 一键部署脚本
│   ├── smoke_test.sh           # 全功能冒烟测试
│   └── verify.sh               # 路由注册/404 掩盖验证
├── ai-interview-web/           # 前端（Vue 3 + TS + Vite）
│   └── src/{views,components,api,stores,styles}
├── docs/                       # PRD、架构设计、部署手册、评审 rubric
└── tools/                      # gen_question_seed.py（题库生成脚本）
```

---

## 关键设计

### AI 防护链为什么这样排

顺序不可调换：**去重 → 限流 → 熔断 → 舱壁 → 超时 → 重试**。
先去重能消掉最大一块重复流量；限流/熔断在系统已不健康时快速失败；舱壁限制并发防止线程被打满；超时兜住单次调用；最后才重试，且只对**瞬时错误**重试，永久错误（如配额）直接失败。

### SSE 事件协议

| 事件 | 含义 |
|---|---|
| `question` | 推送新题目 |
| `score` | 该题评分 |
| `comment` | 该题点评 |
| `follow_up` | 追问（最多 2 轮，可配） |
| `progress` | 进度心跳 |
| `done` | 会话结束 |
| `error` | 错误终止 |

### 题库幂等

题库以 `title` 为幂等键做 upsert，且种子数据按 `part0..part8` 分片——避免单个 Java 方法字节码超过 64KB 上限。

### 简历解析的失败设计

PDF 若 PDFBox 与启发式都抽不到可读文本（扫描件/图片型 PDF 的典型情况），**直接抛出清晰错误**，而不是返回二进制噪声。这避免了上层把噪声当简历文本、再误判成「不是简历」的连锁错误。

---

## 配置项

常用配置集中在 `ai-interview-server/src/main/resources/application.yml`：

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `server.port` | `8080` | 服务端口 |
| `spring.profiles.active` | `mysql` | 切 `h2` 可零中间件运行 |
| `ai-interview.ai.provider` | `deepseek` | AI 供应商 |
| `ai-interview.ai.model` | `deepseek-v4-flash` | 面试对话模型 |
| `ai-interview.ai.report-model` | `deepseek-chat` | 报告生成模型 |
| `ai-interview.ai.max-retries` | `2` | 重试次数（网络超时为 0） |
| `ai-interview.ai.bulkhead-concurrency` | `20` | 舱壁并发上限 |
| `ai-interview.ai.circuit-breaker-failure-rate` | `0.5` | 熔断失败率阈值 |
| `ai-interview.interview.default-question-count` | `8` | 默认题量（3–15） |
| `ai-interview.interview.max-follow-up` | `2` | 单题最大追问轮次 |
| `ai-interview.interview.resume-max-size-mb` | `5` | 简历大小上限 |
| `ai-interview.jwt.access-expire-seconds` | `7200` | Access Token 有效期 |

> 所有敏感项均支持 `${ENV:默认值}` 覆盖，**生产环境请用环境变量注入**。

---

## 接口文档

启动后访问：

- Swagger UI：<http://localhost:8080/swagger-ui.html>
- OpenAPI JSON：<http://localhost:8080/v3/api-docs>
- 健康检查：<http://localhost:8080/api/health>

---

## 测试

```bash
cd ai-interview-server
mvn.cmd -Dfile.encoding=UTF-8 test
```

当前 **65 个测试全部通过**，覆盖：

- **AI 容错（10 例）**：网络超时、50 并发 single-flight 去重、熔断短路、舱壁打满、限流、瞬时/永久错误重试策略、退避抖动、健康快照。
- **SSE 容错（4 例）**：客户端断开后发送抛错、complete/error 幂等。
- **简历解析（7 例）**：TXT/DOCX/PDF 正常解析、非法类型、扫描件空 PDF、损坏 PDF 字节。
- **认证/用户/领域策略/报告**等常规用例。

---

## 部署

详见 [`docs/DEPLOY.md`](docs/DEPLOY.md)。要点：

1. 上传 `ai-interview-server-1.0.0.jar` 与 `deploy.sh` 到**同一目录**（**不要上传 `.env.local`**，以免覆盖服务器已有密钥）。
2. 备份旧包：`cp xxx.jar xxx.jar.bak`。
3. 执行 `bash deploy.sh`。
4. 验证：访问 `/api/health`，确认返回 `"mockMode": false`（说明 AI Key 已正确绑定，非规则降级）。

---

## 许可证

暂未指定开源协议。如需开源使用，请联系仓库作者。

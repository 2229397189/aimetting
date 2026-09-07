# AI 在线模拟面试平台 — 产品需求文档（PRD）

| 项目信息 | 内容 |
| --- | --- |
| 文档语言 | 中文 |
| 项目名称 | `ai_mock_interview_platform`（snake_case） |
| 目标用户 | 陆强：大四在校生 / Java 后端实习生，秋招目标字节跳动 |
| 原始需求 | 基于 **Java + Vue3** 从零构建一个**功能完整、可运行**的 AI 在线模拟面试平台；前后端一键启动；测试需校验返回内容正确性；模块化提交 GitHub；每模块由 PM 做质量评价 |
| 后端技术栈 | Java 17 + Spring Boot 3.2.5 + MyBatis-Plus 3.5.7 + Spring MVC + SseEmitter + OkHttp + JWT(jjwt 0.12.6) + H2(默认)/MySQL(profile) + Caffeine + springdoc-openapi |
| 前端技术栈 | Vue 3.5 + Vite 5 + TypeScript + Pinia + Vue Router 4 + Element Plus + Axios + ECharts + markdown-it |
| 大模型 | DeepSeek（OpenAI 协议兼容，baseUrl `https://api.deepseek.com`，模型 `deepseek-v4-flash` / `deepseek-chat`） |
| 架构约束 | **零中间件依赖即可启动**（默认 H2 嵌入式库；Redis / MongoDB 不可用；MySQL 作为可选 profile） |
| 构建命令 | 后端：`D:\develop\apache-maven-3.9.15\bin\mvn.cmd`（Git Bash 下 `mvn` 不可用） |

---

## 1. 产品目标与用户画像

### 1.1 产品目标（Goals）

| # | 目标 | 一句话描述 | 衡量指标 |
| --- | --- | --- | --- |
| G1 | 真实面试仿真 | 提供"选方向 → 逐题作答 → AI 追问 → 实时评分 → 报告"的完整闭环，逼近真实技术面试体感 | 单会话平均完成 ≥ 6 题；追问触发率 ≥ 40% |
| G2 | 个性化出题 | 结合岗位方向 + 难度 + 简历内容生成针对性题目，而非通用题库随机抽 | 简历模式下题目与简历关键词命中率 ≥ 60% |
| G3 | 可量化提升 | 输出五维雷达图 + 逐题点评 + 改进建议，让用户知道"差在哪、怎么练" | 报告生成成功率 ≥ 99%；报告含 ≥ 5 条可执行建议 |
| G4 | 零门槛可运行 | `mvn.cmd spring-boot:run` + `npm run dev` 两条命令即可跑通全流程，**无 AI Key 时自动降级 mock 仍可完成整场面试** | 冷启动 ≤ 60s；mock 模式端到端用例 100% 通过 |
| G5 | 工程质量示范 | DDD 分包、统一返回体、三级异常、AI 调用防护、Single-flight、答题幂等、状态机——作为简历可讲的高质量项目 | 后端单测覆盖率 ≥ 60%；测试断言校验业务内容而非 HTTP 200 |

### 1.2 用户画像

| 画像 | 特征 | 核心诉求 | 对应功能优先级 |
| --- | --- | --- | --- |
| **A. 在校生**（主目标，陆强本人） | 大三/大四，有课设/实习项目，缺面试经验，预算敏感 | 低成本高频次练手；想知道"我这个水平能过吗" | P0 全量 + 简历解析（P0） |
| **B. 应届求职者** | 秋招/春招冲刺期，目标大厂，时间紧 | 针对性查漏补缺；模拟目标公司风格的追问压力 | AI 追问（P0）+ 报告雷达图（P0）+ 历史回看（P0） |
| **C. 转岗/跳槽者** | 有工作经验，需跨方向（如前端→后端） | 快速摸底新方向考点；用简历反向校验包装是否成立 | 简历解析 + 岗位方向题库（P1） |

---

## 2. 核心用户旅程

### 2.1 主旅程 Mermaid 流程图

```mermaid
flowchart TD
    A([访客]) --> B[注册 / 登录<br/>JWT 签发]
    B --> C{是否上传简历?}
    C -- "是（可选）" --> D[粘贴文本 或 上传 PDF/DOCX/TXT]
    D --> E[AI 解析简历<br/>提取技能/项目/亮点]
    E --> F[简历评分 + 修改建议]
    C -- "否" --> G
    F --> G[选择面试方向<br/>Java后端/前端/数据库/OS/网络/算法/系统设计/行为面试]
    G --> H[配置难度 + 题量<br/>easy|medium|hard, 3~15 题]
    H --> I[创建会话 INIT]
    I --> J[开始面试 → ASKING]
    J --> K[AI 出题 / 题库抽题<br/>SSE 流式渲染]
    K --> L[用户提交文本答案]
    L --> M{答案长度 ≥ 10 字符?}
    M -- 否 --> L
    M -- 是 --> N[EVALUATING<br/>AI 实时评分 + 点评]
    N --> O{需追问 且<br/>追问次数 < 2?}
    O -- "是" --> P[FOLLOW_UP<br/>AI 生成追问]
    P --> L
    O -- "否" --> Q{还有下一题?}
    Q -- "是" --> K
    Q -- "否" --> R[生成面试报告<br/>总分 + 五维雷达图 + 逐题点评]
    R --> S([COMPLETED<br/>查看报告])
    S --> T[历史记录列表]
    T --> U[回看某场：题目/答案/评分/报告]
    U --> V[删除 / 导出 Markdown]

    J -.-> W[暂停 PAUSED]
    W -.-> J
    J -.-> X[主动结束 → 强制出报告]
    X --> R
```

### 2.2 旅程关键节点说明

| 步骤 | 系统动作 | 失败兜底 |
| --- | --- | --- |
| 注册/登录 | BCrypt 校验 → 签发 accessToken(2h) + refreshToken(7d) | 账号禁用(B0103)、密码错误(A0101) |
| 简历解析 | 文本 → AI 结构化提取（JSON）→ 落库 | AI 失败 → 降级为正则/关键词抽取，标记 `parsedBy=RULE` |
| 创建会话 | 生成 `sessionNo`，状态 INIT，预生成题目骨架 | 题库为空 → 用内置 seed 题库（≥200 题） |
| AI 出题 | Single-flight 去重 → 流式返回 | 超时/限流 → 从题库按方向+难度随机抽题，标记 `source=QUESTION_BANK` |
| 提交答案 | 幂等双键校验 → 落库 → 流式评分 | 重传直接回放上次结果；AI 失败 → 规则评分（关键词命中 + 长度分） |
| 生成报告 | 聚合逐题评分 → AI 生成总结 + 五维打分 | AI 失败 → 按逐题均分与维度加权计算，标记 `generatedBy=RULE` |

---

## 3. 功能需求池

> 优先级：**P0 = Must have（一期必须交付）**；**P1 = Should have**；**P2 = Nice to have**

### 3.1 用户与认证模块（M1）

| ID | 功能名 | 优先级 | 描述 | 验收标准 |
| --- | --- | --- | --- | --- |
| U-01 | 用户注册 | P0 | 用户名(4-20位)+密码(8-20位，含字母数字)+邮箱，校验唯一性 | 重复用户名/邮箱返回明确错误码；密码 BCrypt 加盐落库，明文不出现在日志；返回 `userId` |
| U-02 | 用户登录 | P0 | 用户名/邮箱 + 密码登录，返回 accessToken/refreshToken | 返回 token 可被后续接口解析出 userId；错误密码返回 A0101；连续失败 5 次锁定 5 分钟 |
| U-03 | JWT 鉴权 | P0 | 除白名单外所有接口校验 `Authorization: Bearer <token>`；`@CurrentUser` 参数解析器注入登录态 | 无 token → 401 A0201；token 过期 → 401 A0202；**不使用 ThreadLocal** |
| U-04 | Token 刷新 | P1 | refreshToken 换发 accessToken | refreshToken 过期返回 A0203；刷新后旧 accessToken 仍可用至自然过期 |
| U-05 | 退出登录 | P1 | 前端清除 token，后端加入短期黑名单（Caffeine） | 退出后立即用旧 token 访问受保护接口返回 401 |
| U-06 | 查看/修改个人信息 | P0 | 昵称、头像、目标岗位、工作年限、自我介绍 | 修改后 GET 返回新值；字段超长返回参数校验错误 |
| U-07 | 修改密码 | P0 | 校验原密码 → 设置新密码 | 原密码错误拒绝；修改成功后旧 token 失效需重新登录 |
| U-08 | 个人数据概览 | P1 | 累计面试场次、完成场次、平均分、最近 7 日趋势 | 数字与历史记录聚合结果一致 |

### 3.2 面试会话模块（M2）

| ID | 功能名 | 优先级 | 描述 | 验收标准 |
| --- | --- | --- | --- | --- |
| S-01 | 创建会话 | P0 | 选择方向（可多选）、难度（easy/medium/hard）、题量（3-15，默认 8）、可选绑定简历 | 创建成功返回 `sessionId`，状态 `INIT`；题量越界返回参数错误；绑定他人简历返回 403 |
| S-02 | 会话状态机 | P0 | `INIT → ASKING → EVALUATING → FOLLOW_UP → COMPLETED`，含 `PAUSED` / `ABORTED`；EnumMap 合法转移表校验 | 非法跳转（如 INIT → COMPLETED）抛 `ServiceException` B0301；转移表覆盖全部状态对 |
| S-03 | 开始面试 | P0 | INIT → ASKING，触发首题生成 | 返回首题内容与 `questionNo=1`；重复调用返回当前题不重复生成 |
| S-04 | 取下一题 | P0 | 推进 `currentQuestionIndex`，生成/抽取下一题 | 题序连续不重复；超出题量自动进入 COMPLETED 并触发报告 |
| S-05 | 暂停会话 | P1 | 记录 `prevStatus`，状态置 PAUSED | 暂停中提交答案返回 B0302；恢复后回到 `prevStatus` |
| S-06 | 恢复会话 | P1 | PAUSED → `prevStatus` | 恢复后题目与答案完整保留 |
| S-07 | 结束会话 | P0 | 用户主动结束或答完所有题 → COMPLETED，触发报告生成 | 结束后不可再答题；报告在 30s 内可查到 |
| S-08 | 会话列表 | P0 | 分页 + 按状态/方向/时间筛选 | 只返回**当前用户**的会话；分页 total 正确 |
| S-09 | 会话详情 | P0 | 会话配置 + 全部题目/答案/评分/追问链 | 越权访问他人 sessionId 返回 403 B0303 |
| S-10 | 删除会话 | P1 | 逻辑删除，级联隐藏其报告 | 删除后列表不可见；历史记录页同步消失 |
| S-11 | 会话空闲超时 | P2 | 30 分钟无操作自动 PAUSED；PAUSED 超 7 天自动 ABORTED | 定时任务扫描；状态变更落操作日志 |

### 3.3 题库模块（M3）

| ID | 功能名 | 优先级 | 描述 | 验收标准 |
| --- | --- | --- | --- | --- |
| Q-01 | 内置题库 seed | P0 | 8 个方向 × 3 难度，总量 ≥ 200 题（JSON seed，启动自动导入，幂等） | 空库启动后题库 ≥ 200 条；重复启动不产生重复数据 |
| Q-02 | 题目分类 | P0 | 方向：Java后端 / 前端 / 数据库 / 操作系统 / 计算机网络 / 算法 / 系统设计 / 行为面试 | 方向枚举统一维护；下拉选项与后端枚举一致 |
| Q-03 | 题目 CRUD | P0 | 题干、参考答案要点、方向、难度、标签、解析 | 新增后立即可被抽到；删除为逻辑删除不影响历史会话 |
| Q-04 | 随机抽题 | P0 | 按 方向 + 难度 + 排除已出题 随机抽取 | 同一会话内不重复；可选题不足时放宽难度而非报错 |
| Q-05 | 题目筛选查询 | P0 | 分页 + 方向/难度/关键词模糊搜索 | 关键词命中题干或标签；分页参数越界返回空列表而非 500 |
| Q-06 | 批量导入 | P2 | JSON/CSV 批量导入，返回成功/失败明细 | 单行格式错误不影响其余行；返回失败行号与原因 |
| Q-07 | 题目难度自适应 | P2 | 根据前序答题得分动态调整后续难度 | 得分 ≥ 85 提升一级，≤ 50 降低一级，每场最多调整 2 次 |

### 3.4 AI 能力模块（M4）

| ID | 功能名 | 优先级 | 描述 | 验收标准 |
| --- | --- | --- | --- | --- |
| A-01 | 模型工厂 + 通用 Handler | P0 | 一套代码兼容 OpenAI / DeepSeek / Qwen，通过 `ai.provider` 配置切换 | 仅改配置即可切换 provider，无需改业务代码 |
| A-02 | AI 个性化出题 | P0 | 输入：方向 + 难度 + 简历摘要 + 已出题目；输出：1 道题（题干 + 考察点 + 参考答案要点） | 返回合法 JSON；与已出题目重复率 = 0；JSON 解析失败自动重试 1 次后降级 |
| A-03 | 答案实时评分 | P0 | 输入：题目 + 答案 + 参考答案要点；输出：分数(0-100) + 点评 + 亮点 + 不足 | 分数为整数且落在 0-100；点评非空；SSE 流式按 chunk 返回 |
| A-04 | AI 追问（Follow-up） | P0 | 基于答案薄弱点生成 1 条深入追问 | 每题追问次数 ≤ 2；追问与原题相关；追问也参与评分加权 |
| A-05 | 简历解析与评分 | P0 | 提取技能/项目/经历/教育，输出简历分 + ≥3 条修改建议 | 返回结构化 JSON；非简历文本返回明确错误 A0401 |
| A-06 | 面试报告生成 | P0 | 聚合逐题表现，输出总分、五维分、亮点、改进建议、后续行动计划 | 五维分均 0-100；建议 ≥ 5 条；生成耗时 P95 ≤ 30s |
| A-07 | AI 调用防护 | P0 | 超时（出题 60s / 评分 90s / 报告 120s）+ 熔断（失败率 50% 开断 30s）+ 重试（最多 2 次，指数退避）+ 舱壁（并发信号量 20） | 压测触发限流时返回 C0502 且**不拖垮主流程**；异常归类 TIMEOUT / OVERLOADED / UNAVAILABLE |
| A-08 | **分布式 Single-flight** | P0 | 同一 `(stage, requestKey)` 只有一个 owner 真调 LLM，follower 等待复用结果；Caffeine + JVM 锁实现单机版，抽象层可替换为 Redis | 并发 10 个相同请求只产生 1 次真实 LLM 调用（用 mock provider 计数验证）；owner 失败时 follower 全部收到错误而非死等（有超时） |
| A-09 | AI 调用日志 | P1 | 记录 provider / 模型 / 耗时 / token / 成功失败 / 请求响应摘要 | 管理端可查；日志脱敏（不落完整 prompt 中的敏感信息） |
| A-10 | Mock 降级模式 | P0 | `ai.provider=mock` 或无 Key 时，用预置题目 + 规则评分跑通**全流程** | 无 Key 环境下完成整场面试并出报告，测试全绿 |

### 3.5 答题与幂等模块（M5）

| ID | 功能名 | 优先级 | 描述 | 验收标准 |
| --- | --- | --- | --- | --- |
| W-01 | 提交答案 | P0 | 文本答案（10-5000 字符），返回 SSE 流：评分 → 点评 → 追问 | SSE 首个事件 ≤ 3s（P95）；流结束后答案与评分已落库 |
| W-02 | **答题幂等（双键）** | P0 | 处理中键 `idem:proc:{userId}:{sessionId}:{stage}:{clientToken}` + 回放键 `idem:replay:{...}` | 同一 `clientToken` 重复提交：处理中→返回"处理中"，已完成→**直接回放上次评分结果**，LLM 调用次数不增加 |
| W-03 | 答案长度校验 | P0 | 最短 10 字符（中文按字符计），最长 5000 | 过短返回 A0102 且不调用 LLM；超长截断或拒绝 |
| W-04 | 跳过题目 | P1 | 主动跳过，记 0 分并标记 `skipped` | 报告中标明跳过；不影响后续出题 |
| W-05 | 追问链作答 | P0 | 对追问继续作答，评分按权重计入该题总分 | 原题 70% + 追问 30%（有追问时）；无追问则原题 100% |
| W-06 | 答题草稿自动保存 | P2 | 前端 5s 防抖保存草稿到 localStorage | 刷新页面草稿不丢失 |

### 3.6 简历模块（M6）

| ID | 功能名 | 优先级 | 描述 | 验收标准 |
| --- | --- | --- | --- | --- |
| R-01 | 简历文本解析 | P0 | 粘贴简历文本 → AI 结构化提取 | 返回技能列表/项目列表/教育/工作年限；解析方式标记 AI/RULE |
| R-02 | 简历文件上传 | P1 | 支持 PDF / DOCX / TXT，≤ 5MB | 超限或类型不符返回明确错误；解析出文本内容 |
| R-03 | 简历评分与建议 | P0 | 0-100 分 + 优势 + 改进建议（≥3 条） | 分数 0-100；建议非空 |
| R-04 | 简历列表/详情/删除 | P1 | 一个用户多份简历，可设默认 | 删除后引用该简历的会话保留历史快照 |
| R-05 | 简历绑定会话 | P0 | 创建会话时可选绑定，AI 据此出题 | 出题 prompt 中出现简历关键词 |

### 3.7 面试报告模块（M7）

| ID | 功能名 | 优先级 | 描述 | 验收标准 |
| --- | --- | --- | --- | --- |
| P-01 | 报告生成 | P0 | 会话结束触发，聚合逐题评分 + AI 总结 | 总分 = 逐题加权平均（保留 1 位小数）；生成方式标记 AI/RULE |
| P-02 | 五维雷达图 | P0 | 专业技能 / 表达沟通 / 逻辑思维 / 项目深度 / 潜力，各 0-100 | ECharts radar 渲染；五维分与后端返回完全一致 |
| P-03 | 逐题点评 | P0 | 题干 + 用户答案 + 分数 + 点评 + 参考答案要点 | 题目数量与会话配置一致 |
| P-04 | 亮点与改进 | P0 | 亮点 ≥ 2 条、改进建议 ≥ 3 条、后续行动 ≥ 3 条 | 内容非空且非模板占位符 |
| P-05 | 报告列表/详情/删除 | P0 | 与历史记录联动 | 越权不可访问 |
| P-06 | 报告导出 | P2 | 导出 Markdown / PDF | 导出内容含全部点评与雷达图数据 |

### 3.8 管理端（M8，轻量）

| ID | 功能名 | 优先级 | 描述 | 验收标准 |
| --- | --- | --- | --- | --- |
| M-01 | 题目管理 | P1 | 后台 CRUD、批量导入、启用/停用 | 停用题不再被抽到 |
| M-02 | 用户列表 | P1 | 分页查询、启用/禁用 | 禁用用户登录被拒 |
| M-03 | 会话统计 | P1 | 日活场次、完成率、平均分、方向分布 | 数据与真实会话一致 |
| M-04 | AI 调用监控 | P2 | 调用量、成功率、平均耗时、失败原因分布 | 可从日志表聚合 |
| M-05 | 角色区分 | P1 | `ROLE_USER` / `ROLE_ADMIN`，管理接口需 ADMIN | 普通用户访问 `/api/admin/**` 返回 403 |

### 3.9 前端工程化（M9）

| ID | 功能名 | 优先级 | 描述 | 验收标准 |
| --- | --- | --- | --- | --- |
| F-01 | Axios 封装 | P0 | 内置**去重 + 防抖 + 错误归一**，严格对齐 `Result{code,message,data,requestId}` | 相同请求 300ms 内自动去重；非 `"0"` code 统一弹错 |
| F-02 | SSE 客户端 | P0 | 打字机效果渲染，支持中断 | 断流有兜底提示；用户可停止生成 |
| F-03 | 全局状态 | P0 | Pinia 管理 user / session / config | 刷新后 token 与用户信息恢复 |
| F-04 | 响应式与暗色 | P2 | 移动端可用 + 暗色模式 | 375px 宽度下不横向滚动 |
| F-05 | 单元测试 | P1 | Vitest 覆盖 utils / store / API 适配层 | 覆盖率 ≥ 50% |

---

## 4. 关键业务规则

| 编号 | 规则 | 值 / 说明 |
| --- | --- | --- |
| BR-01 | 每题最多追问次数 | **默认 2 次**（`interview.max-follow-up=2`），达到上限不再追问，直接进入下一题 |
| BR-02 | 分数权重 | 有追问：原题 70% + 追问均分 30%；无追问：原题 100%。总分 = 各题加权平均 |
| BR-03 | 答案长度 | 最短 **10** 字符，最长 **5000** 字符（中文 1 字 = 1 字符，去首尾空白后计算） |
| BR-04 | 题量范围 | 3 ~ 15 题，默认 **8** 题 |
| BR-05 | 会话空闲超时 | 30 分钟无操作 → 自动 `PAUSED`；`PAUSED` 超 7 天 → 自动 `ABORTED` |
| BR-06 | 状态机非法跳转 | 抛 `ServiceException(B0301)`，返回 HTTP 409 |
| BR-07 | 幂等键 TTL | 处理中键 60s；回放键 **24h**（Caffeine，单机版） |
| BR-08 | AI 超时 | 出题 60s / 评分 90s / 报告 120s / 简历解析 90s |
| BR-09 | AI 重试 | 最多 2 次，指数退避 500ms → 1500ms；**仅对 TIMEOUT / UNAVAILABLE 重试，参数错误不重试** |
| BR-10 | 熔断 | 滑动窗口 20 次调用，失败率 ≥ 50% 开断，30s 后半开 |
| BR-11 | 舱壁并发 | 全局 AI 并发信号量 20，超出返回 `C0502 AI 服务繁忙，请稍后再试` |
| BR-12 | Single-flight 跟随超时 | follower 最长等待 120s，超时返回 `C0503` 而非无限阻塞 |
| BR-13 | AI 失败兜底文案 | 出题失败：`"AI 出题服务繁忙，已为你切换到精选题库题目"`；评分失败：`"AI 评分暂不可用，已按参考答案要点给出初步评分"`；报告失败：`"AI 总结生成失败，已基于你的答题数据生成基础报告"`；**均需带 `degraded=true` 标识并在前端轻提示** |
| BR-14 | Mock 模式触发 | `ai.provider=mock` 或 `api-key` 为空 → 全程规则引擎（预置题目 + 关键词命中评分），**不抛异常、不阻断流程** |
| BR-15 | 越权校验 | 所有 session / report / resume / answer 查询**强制拼接 `user_id = @CurrentUser`**（ADMIN 除外）；不匹配返回 403（不返回 404，避免资源枚举） |
| BR-16 | 评分合法性 | AI 返回分数必须 0-100 整数，越界或非数字 → 视为解析失败，触发一次重试后降级为规则评分 |
| BR-17 | 题目去重 | 同一会话内题干 MD5 去重；AI 出题 prompt 注入已出题干列表 |
| BR-18 | 密码策略 | 8-20 位，须同时含字母与数字；BCrypt strength=10 |
| BR-19 | Token 有效期 | accessToken 2h，refreshToken 7d |
| BR-20 | 登录失败锁定 | 同一账号连续失败 5 次锁定 5 分钟（Caffeine 计数） |
| BR-21 | 会话编号 | `sessionNo` = `IM` + yyyyMMdd + 8 位随机，全局唯一 |
| BR-22 | XSS 防护 | 用户输入答案 / 简历原文在前端以纯文本渲染；AI 返回 Markdown 经 markdown-it + 白名单消毒后再 `v-html`；后端存储原文不转义 |

---

## 5. 数据实体概览

| 实体 | 表名 | 关键字段 | 说明 |
| --- | --- | --- | --- |
| 用户 | `t_user` | id, username, password_hash, email, nickname, avatar, role, status, last_login_at, created_at, deleted | 唯一索引 username / email |
| 用户资料 | `t_user_profile` | id, user_id, target_position, work_years, intro, phone | 与 user 一对一 |
| 简历 | `t_resume` | id, user_id, title, raw_text, file_url, parsed_json, score, advantage, suggestions, parsed_by(AI/RULE), is_default | `parsed_json` 存技能/项目/教育 |
| 题目 | `t_question` | id, direction, difficulty, title, reference_points, tags, analysis, source(SEED/AI/ADMIN), status, created_by | 索引 (direction, difficulty, status) |
| 面试会话 | `t_interview_session` | id, session_no, user_id, resume_id, directions, difficulty, total_question, current_index, status, prev_status, score, started_at, finished_at, deleted | 主状态持久化在此 |
| 会话题目 | `t_session_question` | id, session_id, question_no, question_id, title, reference_points, source(AI/BANK), difficulty | 冗余存题干，题库删改不影响历史 |
| 答题记录 | `t_session_answer` | id, session_id, session_question_id, user_id, content, is_follow_up, parent_answer_id, score, comment, highlights, gaps, evaluated_by(AI/RULE), follow_up_count, skipped, client_token, created_at | 追问以 `parent_answer_id` 串成链 |
| 面试报告 | `t_interview_report` | id, session_id, user_id, total_score, dimension_json, highlights, improvements, actions, overall_comment, generated_by(AI/RULE), created_at | `dimension_json` 存五维分 |
| 幂等记录 | `t_idempotent_record` | id, biz_key, user_id, stage, status(PROCESSING/SUCCESS/FAILED), result_json, expire_at | 双键模式持久化层（Caffeine 加速） |
| AI 调用日志 | `t_ai_call_log` | id, user_id, biz_type, provider, model, request_digest, response_digest, prompt_tokens, completion_tokens, cost_ms, success, error_type, error_msg, request_id | 用于管理与排查 |
| 操作/状态流水 | `t_session_event` | id, session_id, from_status, to_status, event, operator, created_at | 状态机审计 |
| 字典/枚举配置 | `t_dict` | id, type, code, label, sort | 方向、难度等下拉（也可前端枚举，二选一） |

**关键索引**：`t_interview_session(user_id, status, created_at)`、`t_session_answer(session_id, session_question_id)`、`t_question(direction, difficulty, status)`、`t_idempotent_record(biz_key)` 唯一。

---

## 6. API 需求清单

> 统一前缀 `/api`；统一返回体 `Result{code:"0", message, data, requestId}`；分页统一 `PageInfo{list, total, pageNum, pageSize}`；鉴权除标注 `公开` 外均需 JWT。
> 错误码分层：**A** 客户端错误 / **B** 系统错误 / **C** 远程（AI）错误。

### 6.1 认证与用户（`/api/auth`、`/api/user`）

| # | 方法 | 路径 | 说明 | 鉴权 |
| --- | --- | --- | --- | --- |
| 1 | POST | `/api/auth/register` | 用户注册，返回 userId | 公开 |
| 2 | POST | `/api/auth/login` | 登录，返回 accessToken/refreshToken/userInfo | 公开 |
| 3 | POST | `/api/auth/logout` | 退出登录，token 进短期黑名单 | 是 |
| 4 | POST | `/api/auth/token/refresh` | 用 refreshToken 换发 accessToken | 公开 |
| 5 | GET | `/api/user/profile` | 获取当前登录用户详细信息 | 是 |
| 6 | PUT | `/api/user/profile` | 修改昵称/头像/目标岗位/工作年限/简介 | 是 |
| 7 | POST | `/api/user/password` | 修改密码（校验原密码） | 是 |
| 8 | GET | `/api/user/stats` | 个人数据概览（场次/平均分/趋势） | 是 |

### 6.2 简历（`/api/resume`）

| # | 方法 | 路径 | 说明 | 鉴权 |
| --- | --- | --- | --- | --- |
| 9 | POST | `/api/resume/parse` | 提交简历文本 → AI 解析 + 评分 + 建议 | 是 |
| 10 | POST | `/api/resume/upload` | 上传 PDF/DOCX/TXT 文件并解析 | 是 |
| 11 | GET | `/api/resume` | 当前用户简历列表 | 是 |
| 12 | GET | `/api/resume/{id}` | 简历详情（含解析结果与评分） | 是 |
| 13 | DELETE | `/api/resume/{id}` | 删除简历 | 是 |
| 14 | PUT | `/api/resume/{id}/default` | 设为默认简历 | 是 |

### 6.3 面试会话（`/api/interview/sessions`）

| # | 方法 | 路径 | 说明 | 鉴权 |
| --- | --- | --- | --- | --- |
| 15 | POST | `/api/interview/sessions` | 创建会话（方向/难度/题量/简历） | 是 |
| 16 | GET | `/api/interview/sessions` | 会话分页列表（按状态/方向/时间筛选） | 是 |
| 17 | GET | `/api/interview/sessions/{id}` | 会话详情（配置 + 全部题目/答案/评分） | 是 |
| 18 | POST | `/api/interview/sessions/{id}/start` | 开始面试 INIT→ASKING，产出首题 | 是 |
| 19 | GET | `/api/interview/sessions/{id}/next-question` | 取下一题（SSE 流式，AI 出题或题库抽题） | 是 |
| 20 | POST | `/api/interview/sessions/{id}/pause` | 暂停会话 | 是 |
| 21 | POST | `/api/interview/sessions/{id}/resume` | 恢复会话 | 是 |
| 22 | POST | `/api/interview/sessions/{id}/finish` | 结束会话并触发报告生成 | 是 |
| 23 | DELETE | `/api/interview/sessions/{id}` | 删除会话（逻辑删除） | 是 |
| 24 | GET | `/api/interview/sessions/{id}/messages` | 会话对话流水（题/答/追问/评分） | 是 |
| 25 | GET | `/api/interview/sessions/{id}/status` | 轻量轮询会话状态与进度（SSE 断线补偿） | 是 |

### 6.4 答题与 AI 评分（`/api/interview`）

| # | 方法 | 路径 | 说明 | 鉴权 |
| --- | --- | --- | --- | --- |
| 26 | POST | `/api/interview/sessions/{id}/answers` | **提交答案**，SSE 流式返回评分+点评+追问（幂等，带 `X-Client-Token`） | 是 |
| 27 | GET | `/api/interview/sessions/{id}/answers/stream` | SSE 统一流端点（评分/追问共用，EventSource 兼容 GET） | 是 |
| 28 | POST | `/api/interview/answers/{answerId}/skip` | 跳过多选题辅助端点（保留扩展） | 是 |
| 29 | POST | `/api/interview/sessions/{id}/questions/{qid}/skip` | 跳过当前题，记 0 分 | 是 |
| 30 | POST | `/api/interview/sessions/{id}/follow-up/answer` | 提交追问答案（复用 #26 逻辑，带 parentAnswerId） | 是 |
| 31 | GET | `/api/interview/answers/{answerId}` | 查询单条答题与评分详情（幂等回放校验用） | 是 |

### 6.5 报告（`/api/reports`）

| # | 方法 | 路径 | 说明 | 鉴权 |
| --- | --- | --- | --- | --- |
| 32 | POST | `/api/reports/{sessionId}/generate` | 触发生成面试报告（幂等） | 是 |
| 33 | GET | `/api/reports/{sessionId}` | 按会话查询报告详情 | 是 |
| 34 | GET | `/api/reports` | 报告分页列表（历史记录） | 是 |
| 35 | GET | `/api/reports/{id}` | 按报告 ID 查询详情 | 是 |
| 36 | DELETE | `/api/reports/{id}` | 删除报告 | 是 |
| 37 | GET | `/api/reports/{id}/export` | 导出 Markdown | 是 |

### 6.6 题库（`/api/questions`）

| # | 方法 | 路径 | 说明 | 鉴权 |
| --- | --- | --- | --- | --- |
| 38 | GET | `/api/questions` | 题目分页查询（方向/难度/关键词筛选） | 是 |
| 39 | GET | `/api/questions/{id}` | 题目详情 | 是 |
| 40 | POST | `/api/questions` | 新增题目 | ADMIN |
| 41 | PUT | `/api/questions/{id}` | 修改题目 | ADMIN |
| 42 | DELETE | `/api/questions/{id}` | 删除题目（逻辑删除） | ADMIN |
| 43 | POST | `/api/questions/random` | 随机抽题（方向+难度+排除列表） | 是 |
| 44 | GET | `/api/questions/directions` | 获取方向/难度枚举列表 | 公开 |
| 45 | POST | `/api/questions/import` | 批量导入题目 | ADMIN |

### 6.7 管理端（`/api/admin`）

| # | 方法 | 路径 | 说明 | 鉴权 |
| --- | --- | --- | --- | --- |
| 46 | GET | `/api/admin/users` | 用户分页列表 | ADMIN |
| 47 | PUT | `/api/admin/users/{id}/status` | 启用/禁用用户 | ADMIN |
| 48 | GET | `/api/admin/statistics/overview` | 总览：用户数/会话数/完成率/平均分 | ADMIN |
| 49 | GET | `/api/admin/statistics/sessions` | 会话趋势与方向分布 | ADMIN |
| 50 | GET | `/api/admin/ai-calls` | AI 调用日志分页查询 | ADMIN |
| 51 | GET | `/api/admin/ai/health` | AI 服务健康与当前 provider/模型 | ADMIN |

### 6.8 运维与公共

| # | 方法 | 路径 | 说明 | 鉴权 |
| --- | --- | --- | --- | --- |
| 52 | GET | `/api/health` | 健康检查（DB/AI provider/mock 标识） | 公开 |
| 53 | GET | `/api/config/client` | 前端所需配置（是否 mock、题量范围、追问上限） | 公开 |
| 54 | GET | `/v3/api-docs` + `/swagger-ui.html` | springdoc-openapi 接口文档 | 公开 |

> 合计 **54 个接口**，覆盖全部 P0/P1 功能。

---

## 7. UI 页面清单

| 路由 | 页面 | 职责 | 关键组件 |
| --- | --- | --- | --- |
| `/login` | 登录/注册 | 表单校验、JWT 存储、记住我 | `AuthForm`、`ElForm` 校验规则 |
| `/` | 首页 Dashboard | 入口导航、最近面试、数据概览卡片、快速开一场 | `StatCard`、`QuickStart`、`RecentList` |
| `/resume` | 简历管理 | 粘贴/上传、解析结果展示、评分与建议、设为默认 | `ResumeUpload`、`ResumeScore`、`SkillTags` |
| `/interview/setup` | 面试配置 | 选方向（多选）、难度、题量、绑定简历 | `DirectionSelector`、`DifficultyRadio`、`Slider` |
| `/interview/:sessionId` | **面试进行中（核心页）** | 题干展示、答案输入、SSE 打字机评分、追问链、进度条、暂停/结束 | `QuestionPanel`、`AnswerEditor`、`StreamingScore`、`FollowUpChain`、`ProgressStep`、`StatusBadge` |
| `/interview` | 面试记录列表 | 分页、状态筛选、进入/删除 | `SessionTable`、`StatusFilter` |
| `/report/:sessionId` | **面试报告** | 总分、五维雷达图、逐题点评、亮点/改进/行动 | `RadarChart(ECharts)`、`ScoreRing`、`QuestionReview`、`SuggestionList` |
| `/report` | 报告列表 | 历史报告分页、查看/删除/导出 | `ReportTable` |
| `/questions` | 题库浏览 | 方向/难度筛选、题目详情、参考答案折叠 | `QuestionFilter`、`QuestionCard` |
| `/profile` | 个人中心 | 资料编辑、改密码、我的数据 | `ProfileForm`、`PasswordForm` |
| `/admin/questions` | 后台-题目管理 | 表格 CRUD、批量导入 | `QuestionAdminTable`、`ImportDialog` |
| `/admin/users` | 后台-用户管理 | 用户列表、启停 | `UserAdminTable` |
| `/admin/dashboard` | 后台-数据看板 | 场次趋势、方向分布、AI 调用监控 | `LineChart`、`PieChart`、`AiStatTable` |
| `*` | 404 / 403 | 错误兜底 | `ErrorPage` |

**全局组件**：`AppHeader`（含用户菜单/暗色开关）、`AppSider`、`MarkdownRender`（消毒后渲染）、`StreamText`（打字机）、`ErrorBoundary`、`LoadingSkeleton`。

---

## 8. 非功能需求

### 8.1 性能

| 项 | 指标 |
| --- | --- |
| 应用冷启动 | ≤ 60s（H2 模式，含建表 + seed 导入） |
| 页面首屏 | 本地 LCP ≤ 2s |
| 非 AI 接口 P95 | ≤ 300ms |
| SSE 首个 token | ≤ 3s（P95） |
| 单题评分完成 | ≤ 15s（P95） |
| 报告生成 | ≤ 30s（P95） |
| 并发 | 单实例支撑 50 并发会话（AI 舱壁 20） |

### 8.2 安全

| 项 | 要求 |
| --- | --- |
| 密码存储 | BCrypt(strength=10)，禁止明文/可逆加密，日志脱敏 |
| 传输 | 建议 HTTPS（本地 HTTP 可接受）；token 不落 URL |
| 鉴权 | JWT 签名校验 + 过期校验；`@CurrentUser` 注入，禁 ThreadLocal |
| 越权 | 所有资源查询强制带 `user_id`，横向越权返回 403（BR-15） |
| XSS | AI 返回 Markdown 走 markdown-it + 白名单消毒；用户输入纯文本渲染（BR-22） |
| SQL 注入 | MyBatis-Plus 参数绑定，禁止 `${}` 拼接 |
|  uploads | 文件类型白名单 + 大小 ≤ 5MB + 重命名存储 |
| 限流 | 登录/注册/AI 提交按 IP+用户 限流（Caffeine 计数器） |

### 8.3 可观测性

| 项 | 要求 |
| --- | --- |
| requestId | 全链路透传（Filter 生成 → MDC → 返回体 `requestId` → 前端错误提示展示） |
| 日志 | Logback：console + `logs/app.log` 按天滚动；AI 调用单独 `logs/ai.log` |
| AI 监控 | 调用量、成功率、P95 耗时、token 消耗、失败原因分布（落 `t_ai_call_log`） |
| 健康检查 | Spring Boot Actuator `health`、`info`；`/api/health` 暴露 DB 与 AI provider 状态 |
| 状态审计 | 会话状态变更全量落 `t_session_event` |

### 8.4 降级与容错

| 场景 | 行为 |
| --- | --- |
| **无 AI Key / `ai.provider=mock`** | 全流程走规则引擎：预置题库出题 + 关键词命中评分 + 模板化报告，**必须能完整跑通一场面试** |
| AI 超时 | 重试 2 次（指数退避）→ 降级（BR-08 / BR-09） |
| AI 熔断打开 | 直接降级，不等待超时 |
| AI 舱壁打满 | 返回 `C0502` + 友好文案，前端可重试 |
| AI 返回非法 JSON | 重试 1 次 → 仍失败则降级规则评分，`evaluatedBy=RULE` |
| DB 不可用 | 返回 `B0101`，前端全局错误提示 |
| SSE 断流 | 前端提示"连接中断，正在重试"，可用 #25 轮询接口补偿状态 |
| 前端 JS 错误 | `ErrorBoundary` 捕获，展示 requestId 便于排查 |

### 8.5 工程化与测试

| 项 | 要求 |
| --- | --- |
| 测试断言 | **必须校验返回内容**：注册后查库校验密码非明文、登录后解析 token 校验 userId、评分结果校验分数区间与点评非空、幂等重放校验 LLM 调用次数不增 —— 禁止只断言 HTTP 200 |
| 后端测试 | JUnit5 + Mockito + H2；覆盖率 ≥ 60%；MockMvc 覆盖核心 20 个接口 |
| 前端测试 | Vitest 覆盖 utils / store / API 适配层 |
| 配置管理 | `.env` / `application-*.yml` 分离；Key 不入库不进 git（`.env.example` 提供模板） |
| 文档 | README（一键启动说明，含 `mvn.cmd` 注意点）、CHANGELOG、Swagger |
| 提交规范 | Conventional Commits：`feat(module): xxx`；每完成一个模块一次提交 |
| 部署 | `docker-compose.yml`（可选 profile 启 MySQL） |

---

## 9. 待确认问题与已做假设

### 9.1 已做假设（如与预期不符请指出）

| # | 假设 | 影响面 |
| --- | --- | --- |
| AS-01 | **默认使用 H2 嵌入式文件库**（`./data/aimeeting.mv.db`），MySQL 仅在 `spring.profiles.active=mysql` 时启用 | 零中间件可启动 |
| AS-02 | **Redis 不可用** → 缓存、幂等键、Single-flight、登录失败计数全部用 **Caffeine + JVM 锁**实现，并抽象出 `CacheService` / `LockService` / `SingleFlight` 接口，后续可替换为 Redis 实现 | 架构可插拔 |
| AS-03 | **AI 无 Key / 超时 / 熔断时降级为规则题库出题 + 关键词评分**，仍可跑通全流程 | 演示与测试不依赖外部服务 |
| AS-04 | 简历文件上传支持 **PDF/DOCX/TXT**：TXT 直接读，PDF 用 Apache PDFBox，DOCX 用 Apache POI；若解析依赖过重，一期可只做 **TXT + 粘贴文本**（P1 降级） | 依赖体积 |
| AS-05 | 面试方向**一期固定 8 类**枚举（Java后端/前端/数据库/操作系统/计算机网络/算法/系统设计/行为面试），不做自定义方向 | 简化题库与 prompt |
| AS-06 | 管理端为**轻量后台**，复用同一前端工程 `/admin/**` 路由，不单独建 admin 应用 | 交付成本 |
| AS-07 | 默认 LLM 用 `deepseek-v4-flash`（成本低、速度快），报告生成可用 `deepseek-chat`（质量高），通过配置切换 | 成本与质量平衡 |
| AS-08 | 一次会话**单方向为主**，允许配置多选但出题按权重混合 | 简化状态机 |
| AS-09 | 前端渲染 AI 内容用 markdown-it + 白名单（不引入 DOMPurify 额外依赖，若时间允许再加） | 依赖体积 |
| AS-10 | 报告导出一期做 **Markdown**，PDF 导出列为 P2 | 优先级 |

### 9.2 需确认问题

| # | 问题 | PM 建议 |
| --- | --- | --- |
| Q1 | MySQL 密码是否可获取？若可用，是否需要一期就做 MySQL profile 的联调验证？ | 建议：一期以 H2 为准，MySQL profile 写好配置但不强制联调，README 说明切换方式 |
| Q2 | 简历解析是否必须支持 PDF/DOCX 上传，还是一期只做文本粘贴？ | 建议：P0 文本粘贴，P1 文件上传（PDFBox/POI 会增加约 20MB 依赖） |
| Q3 | 面试报告是否需要支持"对比历史多次面试的进步曲线"？ | 建议：列 P2，一期只做单次报告 + 列表 |
| Q4 | 是否需要语音输入/语音转文字作答？ | 建议：**不做**（浏览器 STT 兼容性差、成本高），纯文本作答 |
| Q5 | 题库 seed 题目由谁产出？是否需要 AI 批量生成后人工抽检？ | 建议：先用 AI 生成 200+ 题做 seed JSON，人工抽检 10% 后入库 |
| Q6 | 是否需要真实的"多用户并发 + 分布式"验证（如启两个实例）？ | 建议：单机单实例验证 Single-flight（用并发 mock 计数证明只调 1 次），不强制多实例 |
| Q7 | 前端是否需要移动端适配与暗色模式？ | 建议：响应式做基础适配（P1），暗色模式 P2 |
| Q8 | 单测覆盖率的硬性门槛定多少？ | 建议：后端行覆盖 ≥ 60%，核心域（interview/ai）≥ 70% |

---

## 10. 里程碑建议（供架构师排期参考）

| 阶段 | 内容 | 产出 |
| --- | --- | --- |
| M1 骨架 | 工程初始化、统一返回体、异常体系、JWT、用户模块、H2 建表 | 可注册登录 |
| M2 题库 | 题目实体、seed 导入、CRUD、随机抽题 | 题库可用 |
| M3 AI 基建 | 模型工厂、Handler、防护（超时/熔断/重试/舱壁）、Single-flight、幂等、Mock provider | AI 可调用且可降级 |
| M4 会话核心 | 会话状态机、SSE、出题、答题、评分、追问 | 能完整跑一场面试 |
| M5 简历 | 解析、评分、建议、绑定会话 | 个性化出题 |
| M6 报告 | 报告生成、五维雷达图、逐题点评、历史列表 | 闭环完成 |
| M7 前端 | Vue3 全量页面 + axios 封装 + SSE 打字机 + ECharts | 端到端可演示 |
| M8 管理端 + 工程化 | 后台页面、docker-compose、README、测试补齐 | 可交付 |

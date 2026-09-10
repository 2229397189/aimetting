# AI 模拟面试平台竞品研究 & 改进清单

> 研究性质：纯产品调研，不修改任何 Java/TS 源码。
> 研究方法：WebSearch 检索 6+ 主流平台（2025-2026 现状）。
> 目标读者：本项目（Spring Boot + Vue AI 模拟面试平台）的产品/技术负责人。

---

## 1. Platform Snapshots（平台速览）

| 平台 | 独特点（UX/产品） | 题库/生成 | 实时反馈 | 追问 | 评分/报告 | 简历解析 | 成长追踪 | 备注 |
|------|------|------|------|------|------|------|------|------|
| **interviewing.io** | 匿名真人模拟 + 免费 AI Interviewer；可回放整场面试 | 真人题 + 200+ 题集 | 结束后详细可操作反馈 | 真人主导 | 1-5 星多维（沟通/解题/编码/整体）+ 录用建议 | 弱 | 有（对比同级别） | 系统设计与编码白板（CoderPad 风格） |
| **Final Round AI** | 简历+目标岗位个性化；实时 CoPilot（隐身模式）；STAR 即时提醒 | 基于简历/JD/行业动态生成 | 实时 + 情绪/语速分析 | AI 自适应 | 表现分 + 语音清晰度 + 参与度分析 | **强**（核心入口） | **有**（成长曲线） | 语音/视频/文本多模式 |
| **PrepWise (开源)** | 语音 AI 面试官（VAPI）；按角色/技术栈生成 | 80+ 技术动态生成 | 语音对话中实时 | 维持节奏的追问 | 5 维：沟通/技术/解题/文化契合/自信 | 中（角色/技术栈） | 有（仪表盘） | 含 Monaco 代码编辑器 |
| **Huru** | 2 万+ 题，多语言，手机优先；招聘站 Chrome 插件拉 JD | 从 LinkedIn/Indeed JD 生成 | 实时语音/表达提示 | 有 | 内容准确性+语音/语法+自信度多维 | 中（JD 驱动） | 有（录像回看） | 填充词/语调/能量分析极强 |
| **Mockmate** | 真实公司题库（Google/Amazon 等）；组织心理学家背书算法 | 800+ 真实公司题 / JD 匹配 | 提交即即时客观分析 | 有 | 4 指标：Accuracy/Depth/Confidence/Relevance | 中（JD 粘贴） | 弱 | 打/说/录视频多模态 |
| **开源 AI Interviewer**（neerazz / Interviewer-AI-Agent / HopeLoom / prabhuanantht） | 系统设白板、Agentic 多阶段、多 LLM、自适应人设 | 简历感知生成 | 流式 + 自适应 | **Agentic 智能追问**（Confused/Efficient/Chatty 人设） | JSON 鲁棒解析的结构化评分+改进计划 | **强**（简历感知） | 有（会话历史） | 白板画图、进度追踪、WebSocket |

**关键趋势（跨平台共识）**：
1. 简历/JD 驱动个性化已成标配（Final Round AI、Huru、PrepWise、开源项目）。
2. 多维度结构化评分 > 单一总分（PrepWise 5 维、Mockmate 4 指标、MockTrail 12 参数）。
3. 报告必须"可操作"：给标杆答案 + 重述答案 + 下一步练习（Huru、Final Round AI、interviewing.io）。
4. 行为/STAR 框架化反馈（MockTrail、OfferGoose、Final Round AI 的 STAR 提醒）。
5. 系统设白板 + 架构回顾是差异化亮点（neerazz 开源、MockTrail）。
6. 成长曲线/进度追踪提升留存（Final Round AI、PrepWise、Huru 录像回看）。

---

## 2. 优先级改进清单

### P0 — Must-have（直接影响真实感与留存，建议优先）

| 功能 | 一句话描述 | 为什么重要（引用平台） | 工作量 |
|------|------|------|------|
| **评分锚定 Rubric（锚点样例）** | 给 0-100 各分段配"标杆答案样例 + 扣分锚点"，让同题多次评分一致 | 我们当前 0-100 缺锚点易漂移；Mockmate 用 Accuracy/Depth/Confidence/Relevance 四维明确各自含义，PrepWise 5 维可对照 | M |
| **简历/JD 驱动个性化出题** | 上传简历+JD，AI 据此生成与目标岗位强相关的题与追问 | Final Round AI 以简历为核心入口、Huru 从 JD 拉题、开源项目简历感知生成——个性化是行业标配 | M |
| **结构化 5 维报告 + 标杆答案** | 报告除总分外给各维分、标杆答案、重述答案、下一步练习清单 | Huru/ Final Round AI / interviewing.io 的"可操作报告"是留存关键，单纯分数无指导意义 | M |
| **追问主题广度控制** | 追问时强制覆盖"深挖技术 / 横向对比 / 边界-case / 权衡"等多类，避免重复同点 | 开源 Interviewer-AI-Agent 用 Agentic 多阶段+人设避免追问单一；我们当前追问可能偏窄 | S |
| **STAR 行为题结构化反馈** | 行为题按 STAR（情境/任务/行动/结果）逐段点评，缺哪段标哪段 | MockTrail 4 段 STAR 百分比分析、Final Round AI STAR 即时提醒、OfferGoose STAR  adherence——行为题是高频弱项 | M |

### P1 — Should-have（显著提升体验与差异化）

| 功能 | 一句话描述 | 为什么重要（引用平台） | 工作量 |
|------|------|------|------|
| **跨场成长曲线 / 进度追踪** | 跨多次面试聚合各维分数趋势、薄弱点演进、推荐练习 | Final Round AI 成长曲线、PrepWise 仪表盘、Huru 录像回看——进度可视化显著提升留存 | L |
| **系统设白板 + 架构回顾** | 系统设题支持画架构图，AI 对图与讲解做回顾评分 | neerazz 开源项目白板+架构回顾、MockTrail 架构回顾——技术岗强差异化 | L |
| **表达/语音分析（填充词、语速、能量）** | 语音答题时分析填充词、停顿、语调能量并给逐句纠正 | Huru 填充词/语调/能量分析极强、Yoodli 填充词检测——表达是技术岗普遍短板 | L |
| **公司/岗位专属题包** | 预置 Amazon LP、Google GCA、Meta CORE 等框架题 | MockTrail 500+ 公司专属、Mockmate 真实公司题库——目标导向练习更聚焦 | M |
| **追问质量护栏（避免重复/无关）** | 多轮中记录已问点，追问引擎去重并校验相关性 | 开源 Interviewer-AI-Agent 用 question hash 去重 + move-on 逻辑；我们多轮易重复 | S |
| **报告可分享/对比（与同方向考生分位）** | 匿名分位对比，给"你在同方向处于前 X%" | interviewing.io 同级别对比、Mockmate 客观排名——社会比较驱动复练 | M |

### P2 — Nice-to-have（锦上添花）

| 功能 | 一句话描述 | 为什么重要（引用平台） | 工作量 |
|------|------|------|------|
| **面试官人设模拟（压力型/友好型/深挖型）** | 切换 interviewer 风格，练不同真实场景 | prabhuanantht 自适应人设（Confused/Efficient/Chatty）、OfferGoose 人设——提升真实感 | M |
| **实时语音/视频答题模式** | 支持说/录视频而不仅打字，逼近真实面试 | Huru 视频、PrepWise 语音、Final Round AI 多模式——文本 ≠ 真实面试 | L |
| **多语言练习** | 中英等多语言出题与反馈 | Huru 5 语言、Final Round AI 非母语者场景——国际化扩展 | M |
| **AI 生成练习路线图** | 基于弱项自动排下周练习计划（如"本周专练澄清问题"） | Final Round AI 路线图、interviewing.io "每周focus一点"最佳实践——降低弃用 | S |
| **会话回放 + 高亮** | 回放整场，AI 标记亮点/卡壳时刻 | interviewing.io 回放、Huru 录像——自我觉察提升 | M |
| **题源社区/真实经历题** | 用户贡献真实面经题，丰富题库 | Final Round AI 用真实面试经历更新题库——UGC 增强时效 | M |

---

## 3. 六个关注角度的对应结论

- **(i) 评分一致性 / Rubric 锚点**：当前 0-100 缺锚点。做法——为每个维度定义"锚点答案 + 扣分规则"（参考 Mockmate 四维定义、PrepWise 5 维）。这是 P0，投入小回报大。
- **(ii) 追问质量 & 主题广度**：当前多轮可能偏窄。做法——Agentic 式多阶段计划 + 追问类型轮换（深挖/对比/边界/权衡）+ 已问点去重（参考开源 Interviewer-AI-Agent）。
- **(iii) 报告可操作性**：当前有 highlights/gaps/improved answer，已不错。补强：标杆答案对照 + 各维分 + 下一步练习清单 + 跨场对比（参考 Huru/Final Round AI）。
- **(iv) 简历驱动个性化**：当前未做。P0 级——上传简历+JD 即生成个性化题与追问（行业标配，见 Final Round AI/Huru/开源）。
- **(v) 进度追踪**：当前缺。P1——跨场成长曲线 + 薄弱点演进 + 练习推荐（见 Final Round AI/PrepWise）。
- **(vi) 行为 STAR & 系统设白板**：STAR 反馈 P0（框架化逐段点评）；系统设白板 P1（画图+架构回顾，见 neerazz 开源/MockTrail）。

---

## 4. Recommended next 3 features for our project（建议优先做的 3 个）

1. **评分 Rubric 锚点化 + 5 维结构化报告**（P0，M）：给现有 0-100 评分加锚点样例与扣分规则，并把报告从"分数+评论"升级为"各维分 + 标杆答案 + 重述答案 + 下一步练习"。直接解决评分漂移与报告不可操作两大痛点，工作量中等、风险低（仅 AI 输出结构 + 前端展示）。
2. **简历/JD 驱动个性化出题与追问**（P0，M）：新增简历/JD 上传，作为出题与多轮追问的上下文输入。这是与头部平台拉齐的最低门槛功能，且天然契合我们已有的"方向/难度"题库体系。
3. **STAR 行为题结构化反馈 + 追问主题广度控制**（P0/P1，S-M）：行为题按 STAR 四段点评缺失段落；技术题追问强制覆盖多类型并去重。以较小改动显著提升真实感与反馈质量，且复用现有 SSE/JSON 模式。

---

## 参考来源（按平台）
- interviewing.io — 官网与第三方评测（匿名真人+AI Interviewer、回放、多维评分）
- Final Round AI — 官网/评测（简历个性化、实时 CoPilot、STAR 提醒、成长曲线、情绪分析）
- PrepWise (开源 kenton/Eahtasham/ShariniN/PradheebanAnandhan) — GitHub（5 维评分、语音、代码编辑器、进度仪表盘）
- Huru — 官网与 Final Round AI 对比文（2 万题、语音/填充词/能量分析、JD Chrome 插件、多语言）
- Mockmate — 官网与 EazyLearnings（真实公司题库、4 指标 Accuracy/Depth/Confidence/Relevance、JD 匹配）
- 开源 AI Interviewer — neerazz(系统设白板/简历感知)、Sanidhya3008( Agentic 多阶段/去重)、gstearmit(语音流式)、HopeLoom(多 LLM/WebSocket)、prabhuanantht(自适应人设) — GitHub
- MockTrail / OfferGoose / Prepzi.ai — 横向评测（12 参数、STAR 反馈、公司专属、系统设架构回顾）

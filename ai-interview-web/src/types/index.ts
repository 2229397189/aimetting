/**
 * 全局类型定义
 * 与后端 Result / PageInfo / 枚举 / SSE 信封一一对应（见 ARCHITECTURE.md 第 3、4.4、6 章）
 */

/* ------------------------------------------------------------------ */
/* 统一返回体                                                          */
/* ------------------------------------------------------------------ */

/** 后端统一返回体；code === '0' 表示成功 */
export interface Result<T = unknown> {
  code: string
  message: string
  data: T
  requestId?: string
  success?: boolean
}

/** 统一分页结构 */
export interface PageInfo<T = unknown> {
  list: T[]
  total: number
  pageNum: number
  pageSize: number
  pages?: number
}

/** 分页查询基类参数 */
export interface PageQuery {
  pageNum?: number
  pageSize?: number
}

/* ------------------------------------------------------------------ */
/* 枚举                                                                */
/* ------------------------------------------------------------------ */

/** 面试方向（与后端 Direction 枚举一致，8 类） */
export type Direction =
  | 'JAVA_BACKEND'
  | 'FRONTEND'
  | 'DATABASE'
  | 'OS'
  | 'NETWORK'
  | 'ALGORITHM'
  | 'SYSTEM_DESIGN'
  | 'BEHAVIORAL'

/** 难度（与后端 Difficulty 枚举一致） */
export type Difficulty = 'EASY' | 'MEDIUM' | 'HARD'

/** 会话状态（与后端 SessionStatus 一致） */
export type SessionStatus =
  | 'INIT'
  | 'ASKING'
  | 'EVALUATING'
  | 'FOLLOW_UP'
  | 'PAUSED'
  | 'COMPLETED'
  | 'ABORTED'

/** Δ1 增量：三阶段面试流程 */
export type InterviewPhase = 'TECHNICAL' | 'PROJECT' | 'BEHAVIORAL'

/** 用户角色 */
export type UserRole = 'USER' | 'ADMIN'

/** 题目来源 */
export type QuestionSource = 'AI' | 'BANK' | 'SEED' | 'ADMIN'

/** 评分来源 */
export type EvaluatedBy = 'AI' | 'RULE'

/** 报告生成方式 */
export type GeneratedBy = 'AI' | 'RULE'

/** 简历解析方式 */
export type ParsedBy = 'AI' | 'RULE'

/** SSE done 事件给出的下一步动作 */
export type NextAction = 'NEXT_QUESTION' | 'FOLLOW_UP' | 'COMPLETED' | 'WAIT'

/* ------------------------------------------------------------------ */
/* 认证与用户                                                          */
/* ------------------------------------------------------------------ */

export interface LoginReq {
  username: string
  password: string
  remember?: boolean
}

export interface RegisterReq {
  username: string
  password: string
  confirmPassword?: string
  email: string
  nickname?: string
}

export interface TokenResp {
  accessToken: string
  refreshToken: string
  tokenType?: string
  expiresIn?: number
}

export interface LoginResp extends TokenResp {
  userId: number
  username: string
  nickname?: string
  role: UserRole
}

export interface UserProfile {
  id: number
  username: string
  nickname: string
  email: string
  avatar: string
  role: UserRole
  status: number
  targetPosition?: string
  workYears?: number
  intro?: string
  phone?: string
  createdAt?: string
  lastLoginAt?: string
}

export interface UpdateProfileReq {
  nickname?: string
  avatar?: string
  targetPosition?: string
  workYears?: number
  intro?: string
  phone?: string
}

export interface ChangePasswordReq {
  oldPassword: string
  newPassword: string
}

export interface UserStats {
  totalSessions: number
  completedSessions: number
  averageScore: number
  totalQuestions: number
  resumeCount: number
  trend: TrendPoint[]
}

export interface TrendPoint {
  date: string
  count: number
  score?: number
}

/* ------------------------------------------------------------------ */
/* 简历                                                                */
/* ------------------------------------------------------------------ */

export interface ResumeResp {
  id: number
  userId: number
  title: string
  rawText?: string
  fileUrl?: string
  score?: number
  advantage?: string
  suggestions?: string[] | string
  parsedBy?: ParsedBy
  isDefault?: boolean
  createdAt?: string
}

export interface ResumeDetailResp extends ResumeResp {
  parsedJson?: ResumeParsed | null
}

export interface ResumeParsed {
  skills?: string[]
  projects?: ResumeProject[]
  educations?: ResumeEducation[]
  workYears?: number
  summary?: string
}

export interface ResumeProject {
  name?: string
  role?: string
  description?: string
  techStack?: string[]
}

export interface ResumeEducation {
  school?: string
  major?: string
  degree?: string
  period?: string
}

export interface ResumeParseReq {
  title?: string
  content: string
  clientToken?: string
}

/* ------------------------------------------------------------------ */
/* 题库                                                                */
/* ------------------------------------------------------------------ */

export interface QuestionResp {
  id: number
  direction: Direction
  difficulty: Difficulty
  title: string
  referencePoints?: string[] | string
  tags?: string[] | string
  analysis?: string
  source?: QuestionSource
  status?: number
  createdAt?: string
}

export interface QuestionDetailResp extends QuestionResp {
  createdBy?: number
  updatedAt?: string
}

export interface QuestionSaveReq {
  id?: number
  direction: Direction
  difficulty: Difficulty
  title: string
  referencePoints?: string[]
  tags?: string[]
  analysis?: string
  status?: number
}

export interface QuestionQuery extends PageQuery {
  direction?: Direction | ''
  difficulty?: Difficulty | ''
  keyword?: string
}

export interface DirectionOption {
  code: Direction
  label: string
  count?: number
}

export interface DifficultyOption {
  code: Difficulty
  label: string
  count?: number
}

export interface DirectionsResp {
  directions: DirectionOption[]
  difficulties: DifficultyOption[]
}

/* ------------------------------------------------------------------ */
/* 面试会话                                                            */
/* ------------------------------------------------------------------ */

export interface CreateSessionReq {
  directions: Direction[]
  difficulty: Difficulty
  totalQuestion: number
  resumeId?: number | null
  /** Δ1 增量：目标岗位 JD 文本（选填，0~3000 字） */
  jdText?: string
  phasePlan?: Record<InterviewPhase, number>
}

export interface SessionBrief {
  id: number
  sessionNo: string
  userId: number
  directions: Direction[]
  difficulty: Difficulty
  totalQuestion: number
  currentIndex: number
  status: SessionStatus
  score?: number
  resumeId?: number
  createdAt?: string
  finishedAt?: string
}

export interface SessionQuestionItem {
  sessionQuestionId: number
  questionNo: number
  questionId?: number
  title: string
  referencePoints?: string[]
  difficulty?: Difficulty
  source?: QuestionSource
  /** Δ1 增量：所属阶段 */
  phase?: InterviewPhase
  answer?: SessionAnswerItem
  followUps?: SessionAnswerItem[]
}

export interface SessionAnswerItem {
  answerId: number
  sessionQuestionId?: number
  questionNo?: number
  content: string
  score?: number
  comment?: string
  highlights?: string[]
  gaps?: string[]
  /** Δ1 增量：得分 < 80 时给出的改进后参考答案 */
  improvedAnswer?: string
  isFollowUp?: boolean
  parentAnswerId?: number
  followUpCount?: number
  skipped?: boolean
  evaluatedBy?: EvaluatedBy
  degraded?: boolean
  createdAt?: string
}

export interface SessionDetail {
  id: number
  sessionNo: string
  userId: number
  resumeId?: number
  directions: Direction[]
  difficulty: Difficulty
  totalQuestion: number
  currentIndex: number
  status: SessionStatus
  prevStatus?: SessionStatus
  score?: number
  jdText?: string
  questions: SessionQuestionItem[]
  createdAt?: string
  startedAt?: string
  finishedAt?: string
}

export interface SessionStatusResp {
  id: number
  status: SessionStatus
  currentIndex: number
  totalQuestion: number
  score?: number
  reportId?: number
}

export interface SessionPageQuery extends PageQuery {
  status?: SessionStatus | ''
  direction?: Direction | ''
}

export interface SubmitAnswerReq {
  sessionQuestionId: number
  content: string
  clientToken?: string
  parentAnswerId?: number | null
  isFollowUp?: boolean
}

export interface QuestionRespStream {
  questionNo: number
  sessionQuestionId: number
  title: string
  referencePoints?: string[]
  difficulty?: Difficulty
  source?: QuestionSource
  totalQuestion?: number
  phase?: InterviewPhase
}

/* ------------------------------------------------------------------ */
/* 报告                                                                */
/* ------------------------------------------------------------------ */

/** 五维雷达图维度 key（与后端 ReportDimension 对应） */
export type DimensionKey =
  | 'PROFESSIONAL'
  | 'EXPRESSION'
  | 'LOGIC'
  | 'PROJECT_DEPTH'
  | 'POTENTIAL'

export interface ReportItem {
  questionNo: number
  title: string
  answer?: string
  score?: number
  comment?: string
  highlights?: string[]
  gaps?: string[]
  /** Δ1 增量：改进后的参考答案（得分 < 80 的题目展示） */
  improvedAnswer?: string
  referencePoints?: string[]
  difficulty?: Difficulty
  skipped?: boolean
  phase?: InterviewPhase
}

export interface ReportDetail {
  id: number
  sessionId: number
  userId: number
  sessionNo?: string
  totalScore: number
  dimensionJson?: Record<string, number> | null
  dimensions?: Record<string, number> | null
  highlights?: string[]
  improvements?: string[]
  actions?: string[]
  overallComment?: string
  generatedBy?: GeneratedBy
  items?: ReportItem[]
  directions?: Direction[]
  difficulty?: Difficulty
  createdAt?: string
}

export interface ReportBrief {
  id: number
  sessionId: number
  sessionNo?: string
  totalScore: number
  generatedBy?: GeneratedBy
  status?: SessionStatus
  directions?: Direction[]
  difficulty?: Difficulty
  createdAt?: string
}

/* ------------------------------------------------------------------ */
/* 管理端                                                              */
/* ------------------------------------------------------------------ */

export interface AdminUserResp {
  id: number
  username: string
  nickname?: string
  email?: string
  role: UserRole
  status: number
  createdAt?: string
  lastLoginAt?: string
}

export interface OverviewStats {
  userCount: number
  sessionCount: number
  completedCount: number
  completionRate: number
  averageScore: number
  questionCount: number
}

export interface SessionTrend {
  days: number
  dates: string[]
  counts: number[]
  directionDistribution?: Array<{ direction: Direction; label?: string; count: number }>
}

export interface AiCallLogResp {
  id: number
  userId?: number
  bizType?: string
  provider?: string
  model?: string
  requestDigest?: string
  responseDigest?: string
  promptTokens?: number
  completionTokens?: number
  costMs?: number
  success?: boolean
  errorType?: string
  errorMsg?: string
  createdAt?: string
}

export interface AiHealthResp {
  provider: string
  model: string
  mock: boolean
  available?: boolean
  circuitBreakerOpen?: boolean
  bulkheadInUse?: number
  bulkheadTotal?: number
  message?: string
}

export interface ClientConfig {
  mockMode?: boolean
  minQuestion?: number
  maxQuestion?: number
  defaultQuestion?: number
  maxFollowUp?: number
  answerMinLength?: number
  answerMaxLength?: number
  appName?: string
}

/* ------------------------------------------------------------------ */
/* SSE 协议（ARCHITECTURE.md 第 6 章）                                  */
/* ------------------------------------------------------------------ */

export type SseEventType =
  | 'question'
  | 'score'
  | 'comment'
  | 'follow_up'
  | 'progress'
  | 'done'
  | 'error'

/** 统一事件信封 */
export interface SseEnvelope<T = unknown> {
  type: SseEventType
  seq: number
  requestId?: string
  sessionId?: number
  questionNo?: number
  degraded?: boolean
  timestamp?: number
  payload: T
}

/** progress：长等待提示 */
export interface ProgressPayload {
  text: string
}

/** question：出题完成 */
export interface QuestionPayload {
  questionNo: number
  sessionQuestionId: number
  title: string
  referencePoints?: string[]
  difficulty?: Difficulty
  source?: QuestionSource
  totalQuestion?: number
  phase?: InterviewPhase
}

/** comment：评分正文增量 */
export interface CommentPayload {
  answerId: number
  delta: string
  finish?: boolean
  /** Δ1 增量：改进后的参考答案增量 */
  improvedAnswerDelta?: string
}

/** score：评分解析完成 */
export interface ScorePayload {
  answerId: number
  score: number
  evaluatedBy?: EvaluatedBy
  highlights?: string[]
  gaps?: string[]
  /** Δ1 增量：改进后的参考答案 */
  improvedAnswer?: string
  degraded?: boolean
}

/** follow_up：判定需要追问 */
export interface FollowUpPayload {
  answerId: number
  parentAnswerId?: number
  sessionQuestionId: number
  title: string
  followUpCount: number
  maxFollowUp: number
}

/** done：流正常结束 */
export interface DonePayload {
  nextAction: NextAction
  status: SessionStatus
  currentIndex: number
  totalQuestion: number
  answerId?: number
  score?: number
  reportId?: number | null
  replayed?: boolean
}

/** error：业务/AI 异常 */
export interface ErrorPayload {
  code: string
  message: string
  retryable?: boolean
}

/* ------------------------------------------------------------------ */
/* 枚举字典（前端展示用）                                              */
/* ------------------------------------------------------------------ */

export interface EnumOption<T = string> {
  value: T
  label: string
  type?: '' | 'success' | 'warning' | 'danger' | 'info'
}

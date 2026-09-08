import type {
  CreateSessionReq,
  PageInfo,
  QuestionRespStream,
  SessionDetail,
  SessionPageQuery,
  SessionStatusResp,
  SubmitAnswerReq,
} from '@/types'
import { http } from './request'
import { apiUrl } from './sse'
import { clientToken } from '@/utils/crypto'

/**
 * 面试会话相关接口（/api/interview/sessions）
 *
 * 注意：提交答案（#26）与取下一题（#19）是 SSE 端点，不能用 axios，
 * 这里只提供「URL + 请求体」构造器，由页面配合 `useSse` 消费。
 */
export const interviewApi = {
  /** 创建会话，返回 sessionId */
  create(data: CreateSessionReq): Promise<number> {
    return http.post<number>('/interview/sessions', data)
  },

  /** 会话分页列表（按状态 / 方向筛选） */
  page(params: SessionPageQuery): Promise<PageInfo<SessionDetail>> {
    return http.get<PageInfo<SessionDetail>>('/interview/sessions', { params })
  },

  /** 会话详情（配置 + 全部题目 / 答案 / 评分） */
  detail(id: number | string): Promise<SessionDetail> {
    return http.get<SessionDetail>(`/interview/sessions/${id}`)
  },

  /** 开始面试：INIT → ASKING，产出首题（阻塞式） */
  start(id: number | string): Promise<QuestionRespStream> {
    return http.post<QuestionRespStream>(`/interview/sessions/${id}/start`)
  },

  /** 暂停会话 */
  pause(id: number | string): Promise<boolean> {
    return http.post<boolean>(`/interview/sessions/${id}/pause`)
  },

  /** 恢复会话 */
  resume(id: number | string): Promise<boolean> {
    return http.post<boolean>(`/interview/sessions/${id}/resume`)
  },

  /** 结束会话并触发报告生成，返回 reportId */
  finish(id: number | string): Promise<number> {
    return http.post<number>(`/interview/sessions/${id}/finish`)
  },

  /** 删除会话（逻辑删除） */
  remove(id: number | string): Promise<boolean> {
    return http.delete<boolean>(`/interview/sessions/${id}`)
  },

  /** 轻量轮询会话状态与进度（SSE 断线补偿） */
  status(id: number | string): Promise<SessionStatusResp> {
    return http.get<SessionStatusResp>(`/interview/sessions/${id}/status`, { dedup: false } as any)
  },

  /** 会话对话流水（题 / 答 / 追问 / 评分） */
  messages(id: number | string): Promise<
    Array<{
      id: number
      role: 'INTERVIEWER' | 'CANDIDATE'
      content: string
      questionNo?: number
      score?: number
      createdAt?: string
    }>
  > {
    return http.get(`/interview/sessions/${id}/messages`)
  },

  /** 跳过当前题（记 0 分） */
  skip(id: number | string, sessionQuestionId: number | string): Promise<boolean> {
    return http.post<boolean>(`/interview/sessions/${id}/questions/${sessionQuestionId}/skip`)
  },

  /** 查询单条答题与评分详情（幂等回放校验用） */
  answerDetail(answerId: number | string): Promise<{
    answerId: number
    content: string
    score?: number
    comment?: string
    highlights?: string[]
    gaps?: string[]
    improvedAnswer?: string
    evaluatedBy?: string
  }> {
    return http.get(`/interview/answers/${answerId}`)
  },
}

/**
 * 构造「提交答案」SSE 请求（POST /api/interview/sessions/{id}/answers）
 * - 带 X-Client-Token 头，配合后端双键幂等（W-02）
 * - body 内也带 clientToken，便于后端无头场景兜底
 */
export function buildSubmitAnswerSse(
  sessionId: number | string,
  req: SubmitAnswerReq,
  token: string,
): { url: string; headers: Record<string, string>; body: Record<string, unknown> } {
  return {
    url: apiUrl(`/interview/sessions/${sessionId}/answers`),
    headers: {
      'X-Client-Token': token,
    },
    body: {
      ...req,
      clientToken: token,
    },
  }
}

/**
 * 构造「取下一题」SSE 请求（GET /api/interview/sessions/{id}/next-question）
 * @param lastSeq 断线重连补偿用，从 0 开始
 */
export function buildNextQuestionSse(
  sessionId: number | string,
  lastSeq = 0,
): { url: string; headers: Record<string, string> } {
  return {
    url: apiUrl(`/interview/sessions/${sessionId}/next-question?lastSeq=${lastSeq}`),
    headers: {},
  }
}

/** 生成一次答题的幂等 token */
export function newClientToken(): string {
  return clientToken('ans')
}

export default interviewApi

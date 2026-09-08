import type {
  AiCallLogResp,
  AiHealthResp,
  OverviewStats,
  PageInfo,
  PageQuery,
  AdminUserResp,
  SessionTrend,
} from '@/types'
import { http } from './request'

/** 管理端接口（/api/admin，均需 ADMIN 角色） */
export const adminApi = {
  /** 用户分页列表 */
  users(params: PageQuery & { keyword?: string }): Promise<PageInfo<AdminUserResp>> {
    return http.get<PageInfo<AdminUserResp>>('/admin/users', { params })
  },

  /** 启用 / 禁用用户（status: 1 启用，0 禁用） */
  updateUserStatus(id: number | string, status: number): Promise<boolean> {
    return http.put<boolean>(`/admin/users/${id}/status`, { status })
  },

  /** 总览统计：用户数 / 会话数 / 完成率 / 平均分 */
  overview(): Promise<OverviewStats> {
    return http.get<OverviewStats>('/admin/statistics/overview')
  },

  /** 会话趋势与方向分布 */
  sessionTrend(days = 7): Promise<SessionTrend> {
    return http.get<SessionTrend>('/admin/statistics/sessions', { params: { days } })
  },

  /** AI 调用日志分页查询 */
  aiCalls(
    params: PageQuery & { bizType?: string; success?: boolean } = { pageNum: 1, pageSize: 10 },
  ): Promise<PageInfo<AiCallLogResp>> {
    return http.get<PageInfo<AiCallLogResp>>('/admin/ai-calls', { params })
  },

  /** AI 服务健康状态（provider / model / mock / 熔断 / 舱壁） */
  aiHealth(): Promise<AiHealthResp> {
    return http.get<AiHealthResp>('/admin/ai/health')
  },
}

/** 健康检查（公开接口） */
export const healthApi = {
  check(): Promise<{ status: string; db: string; aiProvider: string; mock: boolean }> {
    return http.get('/health')
  },
}

/** 前端所需公共配置（公开接口） */
export const configApi = {
  client(): Promise<{
    mockMode?: boolean
    minQuestion?: number
    maxQuestion?: number
    defaultQuestion?: number
    maxFollowUp?: number
    answerMinLength?: number
    answerMaxLength?: number
    appName?: string
  }> {
    return http.get('/config/client')
  },
}

export default adminApi

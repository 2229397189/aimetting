import type { PageInfo, PageQuery, ReportBrief, ReportDetail } from '@/types'
import { http } from './request'

/** 面试报告相关接口（/api/reports） */
export const reportApi = {
  /** 触发生成报告（幂等），返回 reportId */
  generate(sessionId: number | string): Promise<number> {
    return http.post<number>(`/reports/${sessionId}/generate`)
  },

  /** 按会话查询报告详情 */
  getBySession(sessionId: number | string): Promise<ReportDetail> {
    return http.get<ReportDetail>(`/reports/${sessionId}`)
  },

  /** 按报告 ID 查询详情 */
  getById(id: number | string): Promise<ReportDetail> {
    return http.get<ReportDetail>(`/reports/id/${id}`)
  },

  /** 报告分页列表（历史记录） */
  page(params: PageQuery = { pageNum: 1, pageSize: 10 }): Promise<PageInfo<ReportBrief>> {
    return http.get<PageInfo<ReportBrief>>('/reports', { params })
  },

  /** 删除报告 */
  remove(id: number | string): Promise<boolean> {
    return http.delete<boolean>(`/reports/${id}`)
  },

  /** 导出 Markdown 文本 */
  exportMarkdown(id: number | string): Promise<string> {
    return http.get<string>(`/reports/${id}/export`)
  },
}

export default reportApi

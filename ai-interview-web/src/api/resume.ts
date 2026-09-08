import type { PageInfo, PageQuery, ResumeDetailResp, ResumeParseReq, ResumeResp } from '@/types'
import { http } from './request'

/** 简历相关接口（/api/resume） */
export const resumeApi = {
  /** 提交简历文本 → AI 解析 + 评分 + 建议，返回 resumeId */
  parse(data: ResumeParseReq): Promise<number> {
    return http.post<number>('/resume/parse', data)
  },

  /** 上传简历文件（TXT / MD / PDF，≤ 5MB），返回 resumeId */
  upload(file: File, title?: string): Promise<number> {
    const formData = new FormData()
    formData.append('file', file)
    if (title) formData.append('title', title)
    return http.upload<number>('/resume/upload', formData)
  },

  /** 当前用户简历列表（分页） */
  page(params: PageQuery = { pageNum: 1, pageSize: 10 }): Promise<PageInfo<ResumeResp>> {
    return http.get<PageInfo<ResumeResp>>('/resume', { params })
  },

  /** 简历详情（含解析结果与评分） */
  detail(id: number | string): Promise<ResumeDetailResp> {
    return http.get<ResumeDetailResp>(`/resume/${id}`)
  },

  /** 删除简历 */
  remove(id: number | string): Promise<boolean> {
    return http.delete<boolean>(`/resume/${id}`)
  },

  /** 设为默认简历 */
  markDefault(id: number | string): Promise<boolean> {
    return http.put<boolean>(`/resume/${id}/default`)
  },
}

export default resumeApi

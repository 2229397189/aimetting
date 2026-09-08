import type {
  DirectionsResp,
  PageInfo,
  QuestionDetailResp,
  QuestionQuery,
  QuestionResp,
  QuestionSaveReq,
} from '@/types'
import { http } from './request'

/** 题库相关接口（/api/questions） */
export const questionApi = {
  /** 题目分页查询（方向 / 难度 / 关键词） */
  page(params: QuestionQuery): Promise<PageInfo<QuestionResp>> {
    return http.get<PageInfo<QuestionResp>>('/questions', { params })
  },

  /** 题目详情 */
  detail(id: number | string): Promise<QuestionDetailResp> {
    return http.get<QuestionDetailResp>(`/questions/${id}`)
  },

  /** 新增题目（ADMIN） */
  save(data: QuestionSaveReq): Promise<number> {
    return http.post<number>('/questions', data)
  },

  /** 修改题目（ADMIN） */
  update(id: number | string, data: QuestionSaveReq): Promise<boolean> {
    return http.put<boolean>(`/questions/${id}`, data)
  },

  /** 删除题目（ADMIN，逻辑删除） */
  remove(id: number | string): Promise<boolean> {
    return http.delete<boolean>(`/questions/${id}`)
  },

  /** 随机抽题（方向 + 难度 + 排除列表） */
  random(data: {
    direction?: string
    difficulty?: string
    count?: number
    excludeIds?: number[]
  }): Promise<QuestionResp[]> {
    return http.post<QuestionResp[]>('/questions/random', data)
  },

  /** 方向 / 难度枚举列表（公开，不需要登录） */
  directions(): Promise<DirectionsResp> {
    return http.get<DirectionsResp>('/questions/directions')
  },

  /** 批量导入题目（ADMIN） */
  importQuestions(list: QuestionSaveReq[]): Promise<{ successCount: number; failCount: number }> {
    return http.post<{ successCount: number; failCount: number }>('/questions/import', list)
  },
}

export default questionApi

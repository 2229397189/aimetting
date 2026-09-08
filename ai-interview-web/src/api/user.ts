import type { ChangePasswordReq, UpdateProfileReq, UserProfile, UserStats } from '@/types'
import { http } from './request'

/** 用户相关接口（/api/user） */
export const userApi = {
  /** 获取当前登录用户详细信息 */
  profile(): Promise<UserProfile> {
    return http.get<UserProfile>('/user/profile')
  },

  /** 修改个人资料 */
  updateProfile(data: UpdateProfileReq): Promise<boolean> {
    return http.put<boolean>('/user/profile', data)
  },

  /** 修改密码（成功后需重新登录） */
  changePassword(data: ChangePasswordReq): Promise<boolean> {
    return http.post<boolean>('/user/password', data)
  },

  /** 个人数据概览：场次 / 平均分 / 7 日趋势 */
  stats(): Promise<UserStats> {
    return http.get<UserStats>('/user/stats')
  },
}

export default userApi

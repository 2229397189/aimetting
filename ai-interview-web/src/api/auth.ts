import type { LoginReq, LoginResp, RegisterReq } from '@/types'
import { http, setAuthToken } from './request'

/**
 * 认证相关接口（/api/auth）
 * 说明：login / register / refresh 为公开路径，request 层不会注入 Token。
 */
export const authApi = {
  /** 用户注册，返回 userId */
  register(data: RegisterReq): Promise<number> {
    return http.post<number>('/auth/register', data)
  },

  /** 登录，返回 token 与用户信息 */
  login(data: LoginReq): Promise<LoginResp> {
    return http.post<LoginResp>('/auth/login', data)
  },

  /** 退出登录：后端将 token 加入短期黑名单 */
  logout(): Promise<void> {
    return http.post<void>('/auth/logout')
  },

  /** 用 refreshToken 换发 accessToken（后端返回 {token:{accessToken, refreshToken}}） */
  refresh(refreshToken: string): Promise<{ token: { accessToken: string; refreshToken: string } }> {
    return http.post<{ token: { accessToken: string; refreshToken: string } }>('/auth/token/refresh', {
      refreshToken,
    })
  },
}

/**
 * 登录成功后的收尾：写入 token 到本地存储。
 * 抽成方法便于 store 与页面复用，避免遗漏持久化。
 */
export function persistLogin(resp: LoginResp): void {
  if (!resp) return
  setAuthToken(resp.accessToken, resp.refreshToken)
}

export default authApi

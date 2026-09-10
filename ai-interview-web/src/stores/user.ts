import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import type { LoginReq, RegisterReq, UserProfile, UserRole } from '@/types'
import { authApi } from '@/api/auth'
import { userApi } from '@/api/user'
import { getJson, getRaw, remove, setJson, setRaw, StorageKey } from '@/utils/storage'

/**
 * 用户全局状态：token / 用户信息 / 登录登出 / 本地持久化
 * 刷新后从 localStorage 恢复，避免每次进入都重新登录（F-03）
 */
export const useUserStore = defineStore('user', () => {
  /** accessToken */
  const token = ref<string>(getRaw(StorageKey.Token) || '')
  /** refreshToken */
  const refreshToken = ref<string>(getRaw(StorageKey.RefreshToken) || '')
  /** 用户信息 */
  const userInfo = ref<UserProfile | null>(getJson<UserProfile>(StorageKey.UserInfo, null))

  const isLogin = computed<boolean>(() => !!token.value)
  const userId = computed<number>(() => userInfo.value?.id ?? 0)
  const role = computed<UserRole>(() => userInfo.value?.role ?? 'USER')
  const isAdmin = computed<boolean>(() => role.value === 'ADMIN')
  const displayName = computed<string>(
    () => userInfo.value?.nickname || userInfo.value?.username || '未登录',
  )
  const avatar = computed<string>(() => userInfo.value?.avatar || '')

  /** 写入 token 并持久化 */
  function setToken(access: string, refresh?: string): void {
    token.value = access
    setRaw(StorageKey.Token, access)
    if (refresh) {
      refreshToken.value = refresh
      setRaw(StorageKey.RefreshToken, refresh)
    }
  }

  /** 写入用户信息并持久化 */
  function setUserInfo(info: UserProfile | null): void {
    userInfo.value = info
    if (info) setJson(StorageKey.UserInfo, info)
    else remove(StorageKey.UserInfo)
  }

  /** 登录：成功后拉取完整资料。
   *  注意：后端 AuthController 返回的是 {code, data:{token:{accessToken,...}, user:{...}}}，
   *  拦截器已拆掉外层 code/data，所以这里读到的就是 data 本身——即 {token, user} 嵌套结构。
   */
  async function login(payload: LoginReq): Promise<UserProfile> {
    const resp = await authApi.login(payload)
    const tk = resp?.token
    const u = resp?.user
    if (tk?.accessToken) setToken(tk.accessToken, tk.refreshToken)
    if (u?.userId) {
      setUserInfo({
        id: u.userId,
        username: u.username,
        nickname: u.nickname || u.username,
        email: '',
        avatar: '',
        role: u.role,
        status: 1,
      })
    }
    try {
      const profile = await userApi.profile()
      setUserInfo(profile)
      return profile
    } catch (e) {
      // 资料接口失败不影响进入系统，使用登录返回的基础信息
      return (userInfo.value as UserProfile) || ({} as UserProfile)
    }
  }

  /** 注册：成功后返回 userId，不自动登录 */
  async function register(payload: RegisterReq): Promise<number> {
    return await authApi.register(payload)
  }

  /** 拉取最新用户资料 */
  async function fetchProfile(): Promise<UserProfile | null> {
    try {
      const profile = await userApi.profile()
      setUserInfo(profile)
      return profile
    } catch (e) {
      return null
    }
  }

  /** 刷新 accessToken。后端同样返回嵌套 {token:{accessToken, refreshToken}} */
  async function refresh(): Promise<boolean> {
    if (!refreshToken.value) return false
    try {
      const resp = await authApi.refresh(refreshToken.value)
      const tk = resp?.token
      if (tk?.accessToken) setToken(tk.accessToken, tk.refreshToken)
      return true
    } catch (e) {
      logout(false)
      return false
    }
  }

  /** 退出登录：通知后端拉黑 token，并清理本地状态 */
  async function logout(withRequest = true): Promise<void> {
    if (withRequest && token.value) {
      try {
        await authApi.logout()
      } catch (e) {
        // 后端失败也要保证前端退出
      }
    }
    token.value = ''
    refreshToken.value = ''
    userInfo.value = null
    remove(StorageKey.Token)
    remove(StorageKey.RefreshToken)
    remove(StorageKey.UserInfo)
  }

  return {
    token,
    refreshToken,
    userInfo,
    isLogin,
    userId,
    role,
    isAdmin,
    displayName,
    avatar,
    setToken,
    setUserInfo,
    login,
    register,
    fetchProfile,
    refresh,
    logout,
  }
})

export default useUserStore

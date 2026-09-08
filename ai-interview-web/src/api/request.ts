import axios, { AxiosError, AxiosRequestConfig, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import type { Result } from '@/types'
import { getRaw, remove, setRaw, StorageKey } from '@/utils/storage'

/**
 * Axios 封装（对齐 PRD F-01 与 ARCHITECTURE.md 11.3）
 * - baseURL: /api（dev 下由 vite proxy 转发到 http://localhost:8080）
 * - 请求拦截：公开路径外自动注入 `Authorization: Bearer <token>`，并透传 X-Request-Id
 * - 响应拦截：拆包 Result<T>，`code === '0'` 直接返回 data；否则归一弹错并 reject
 * - 401：清理本地 token 并跳转登录页
 * - 300ms 内相同 GET 请求自动去重（在 http.get 包装层实现，避免拦截器返回未 resolve 的 Promise）
 */

/** 公开路径：不需要携带 Token */
const PUBLIC_PATHS = [
  '/auth/login',
  '/auth/register',
  '/auth/token/refresh',
  '/health',
  '/config/client',
  '/questions/directions',
]

/** 判断是否为公开路径 */
export function isPublicPath(url = ''): boolean {
  return PUBLIC_PATHS.some((p) => url.includes(p))
}

/**
 * 错误码 → 用户可读文案
 * A0* 客户端错误 / B0* 系统错误 / C0* 远程（AI）错误
 */
export function mapErrorMessage(code: string, message: string): string {
  if (!code || code === '0') return message || '请求成功'
  const prefix = code.charAt(0).toUpperCase()
  if (prefix === 'A') return message || '请求参数有误，请检查后重试'
  if (prefix === 'B') return message || '系统繁忙，请稍后再试'
  if (prefix === 'C') return message || 'AI 服务暂时不可用，已为你降级处理'
  switch (code) {
    case '400':
      return message || '请求参数错误'
    case '401':
      return message || '登录已过期，请重新登录'
    case '403':
      return message || '没有权限执行该操作'
    case '404':
      return message || '请求的资源不存在'
    case '409':
      return message || '当前状态不允许该操作'
    case '429':
      return message || '操作过于频繁，请稍后再试'
    case '500':
      return message || '服务器开小差了，请稍后再试'
    default:
      return message || '请求失败，请稍后再试'
  }
}

/** 生成简易 requestId，便于与后端 MDC 日志对齐 */
function genRequestId(): string {
  const c = globalThis.crypto as Crypto | undefined
  if (c && typeof c.randomUUID === 'function') return c.randomUUID().replace(/-/g, '').slice(0, 16)
  return `${Date.now().toString(16)}${Math.random().toString(16).slice(2, 10)}`
}

const request = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: { 'Content-Type': 'application/json;charset=UTF-8' },
})

/** 统一的 401 处理：清 token 跳登录，避免多处重复写 */
function handleUnauthorized(): void {
  remove(StorageKey.Token)
  remove(StorageKey.UserInfo)
  const current = window.location.hash || window.location.pathname
  if (!current.includes('/login')) {
    window.location.href = `/login?redirect=${encodeURIComponent(current)}`
  }
}

/** 从任意响应/错误中提取业务码与文案 */
function pickCodeAndMessage(status: number, data?: Result<unknown> | null): { code: string; msg: string } {
  const code = data?.code ? String(data.code) : String(status)
  return { code, msg: mapErrorMessage(code, data?.message || '') }
}

/* --------------------------- 请求拦截器 --------------------------- */

request.interceptors.request.use(
  (config) => {
    const url = config.url || ''
    config.headers = config.headers || {}
    if (!isPublicPath(url)) {
      const token = getRaw(StorageKey.Token)
      if (token) config.headers.Authorization = `Bearer ${token}`
    }
    config.headers['X-Request-Id'] = genRequestId()
    return config
  },
  (error) => Promise.reject(error),
)

/* --------------------------- 响应拦截器 --------------------------- */

request.interceptors.response.use(
  (response: AxiosResponse<Result<unknown>>) => {
    const res = response.data
    // 非标准返回体（文件流、纯文本等）原样透传
    if (!res || typeof res !== 'object' || (res as Result<unknown>).code === undefined) {
      return res as any
    }
    if (String(res.code) === '0') return res.data as any
    const { code, msg } = pickCodeAndMessage(response.status, res)
    ElMessage.error(msg)
    const err = new Error(msg) as Error & { code?: string; requestId?: string }
    err.code = code
    err.requestId = res.requestId
    return Promise.reject(err)
  },
  (error: AxiosError<Result<unknown>>) => {
    if (!error.response) {
      // 网络错误 / 超时
      const isTimeout = error.code === 'ECONNABORTED' || String(error.message || '').includes('timeout')
      ElMessage.error(isTimeout ? '请求超时，请稍后重试' : '网络连接失败，请检查网络后重试')
      return Promise.reject(error)
    }
    const status = error.response.status
    const data = error.response.data
    if (status === 401) {
      ElMessage.error('登录状态已失效，请重新登录')
      handleUnauthorized()
      return Promise.reject(error)
    }
    const { code, msg } = pickCodeAndMessage(status, data)
    ElMessage.error(msg)
    const err = new Error(msg) as Error & { code?: string; status?: number; requestId?: string }
    err.code = code
    err.status = status
    err.requestId = data?.requestId
    return Promise.reject(err)
  },
)

/* ------------------------------ 去重层 ------------------------------ */

interface DedupItem {
  promise: Promise<unknown>
  expire: number
}
const dedupCache = new Map<string, DedupItem>()
/** 去重窗口（ms），对齐 F-01 的 300ms */
const DEDUP_WINDOW = 300

function buildKey(method: string, url: string, params?: unknown, data?: unknown): string {
  let extra = ''
  if (params !== undefined) extra += `P${JSON.stringify(params)}`
  if (data !== undefined) extra += `D${typeof data === 'string' ? data : JSON.stringify(data)}`
  return `${method.toUpperCase()}@${url}@${extra}`
}

function sweepDedup(): void {
  const now = Date.now()
  dedupCache.forEach((item, key) => {
    if (item.expire <= now) dedupCache.delete(key)
  })
}

/**
 * 带 300ms 去重的 GET：窗口内相同请求复用同一个 Promise。
 * 需要跳过去重时传 `dedup: false`。
 */
function dedupGet<T>(url: string, config: AxiosRequestConfig = {}): Promise<T> {
  if ((config as any).dedup === false) {
    return request.get<any, T>(url, config)
  }
  const key = buildKey('get', url, config.params, undefined)
  const now = Date.now()
  const hit = dedupCache.get(key)
  if (hit && hit.expire > now) return hit.promise as Promise<T>
  const promise = request
    .get<any, T>(url, config)
    .catch((e) => {
      // 失败立即失效，不缓存错误结果
      dedupCache.delete(key)
      return Promise.reject(e)
    })
  dedupCache.set(key, { promise, expire: now + DEDUP_WINDOW })
  sweepDedup()
  return promise
}

/** 对外统一 http 客户端（api 层统一使用 http，而非裸 axios 实例） */
export const http = {
  get: dedupGet,
  post: <T>(url: string, data?: unknown, config: AxiosRequestConfig = {}): Promise<T> =>
    request.post<any, T>(url, data, config),
  put: <T>(url: string, data?: unknown, config: AxiosRequestConfig = {}): Promise<T> =>
    request.put<any, T>(url, data, config),
  delete: <T>(url: string, config: AxiosRequestConfig = {}): Promise<T> =>
    request.delete<any, T>(url, config),
  /** 文件上传（multipart/form-data） */
  upload: <T>(url: string, formData: FormData, config: AxiosRequestConfig = {}): Promise<T> =>
    request.post<any, T>(url, formData, {
      ...config,
      headers: { 'Content-Type': 'multipart/form-data', ...(config.headers || {}) },
    }),
}

/** 写入 token（登录后 / 刷新后调用） */
export function setAuthToken(token: string, refreshToken?: string): void {
  setRaw(StorageKey.Token, token)
  if (refreshToken) setRaw(StorageKey.RefreshToken, refreshToken)
}

export default request

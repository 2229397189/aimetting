/**
 * localStorage 封装：统一 key 前缀，带 JSON 安全序列化与容错。
 */
const KEY_PREFIX = 'ai_interview_'

export const StorageKey = {
  Token: 'token',
  RefreshToken: 'refresh_token',
  UserInfo: 'user_info',
  Config: 'client_config',
  DraftPrefix: 'draft_',
} as const

/** 读取原始字符串 */
export function getRaw(key: string): string {
  try {
    return window.localStorage.getItem(KEY_PREFIX + key) || ''
  } catch (e) {
    return ''
  }
}

/** 写入原始字符串 */
export function setRaw(key: string, value: string): void {
  try {
    window.localStorage.setItem(KEY_PREFIX + key, value)
  } catch (e) {
    /* 忽略：隐私模式/超限时静默失败 */
  }
}

/** 读取 JSON */
export function getJson<T = unknown>(key: string, fallback: T | null = null): T | null {
  const raw = getRaw(key)
  if (!raw) return fallback
  try {
    return JSON.parse(raw) as T
  } catch (e) {
    return fallback
  }
}

/** 写入 JSON */
export function setJson(key: string, value: unknown): void {
  try {
    setRaw(key, JSON.stringify(value))
  } catch (e) {
    /* 忽略循环引用等异常 */
  }
}

/** 删除 */
export function remove(key: string): void {
  try {
    window.localStorage.removeItem(KEY_PREFIX + key)
  } catch (e) {
    /* noop */
  }
}

/** 清空本应用所有 key */
export function clearAll(): void {
  try {
    const keys: string[] = []
    for (let i = 0; i < window.localStorage.length; i++) {
      const k = window.localStorage.key(i)
      if (k && k.startsWith(KEY_PREFIX)) keys.push(k)
    }
    keys.forEach((k) => window.localStorage.removeItem(k))
  } catch (e) {
    /* noop */
  }
}

export default {
  getRaw,
  setRaw,
  getJson,
  setJson,
  remove,
  clearAll,
  StorageKey,
}

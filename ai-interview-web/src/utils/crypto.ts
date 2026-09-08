/**
 * 客户端幂等 token 生成（对应后端 X-Client-Token 双键幂等：BR-07 / W-02）
 */

/** 生成 clientToken：优先 crypto.randomUUID，降级为时间戳+随机数 */
export function clientToken(prefix = 'ct'): string {
  const c = globalThis.crypto as Crypto | undefined
  let core = ''
  if (c && typeof c.randomUUID === 'function') {
    core = c.randomUUID().replace(/-/g, '')
  } else if (c && typeof c.getRandomValues === 'function') {
    const arr = new Uint8Array(16)
    c.getRandomValues(arr)
    core = Array.from(arr)
      .map((n) => n.toString(16).padStart(2, '0'))
      .join('')
  } else {
    core = `${Date.now().toString(16)}${Math.random().toString(16).slice(2)}`
  }
  return `${prefix}-${core}`
}

/**
 * 简易非加密 hash（FNV-1a 32 位），用于题目去重 / 草稿 key，
 * 不用于安全场景（密码一律由后端 BCrypt 处理）。
 */
export function fnv1a32(text: string): string {
  let hash = 0x811c9dc5
  for (let i = 0; i < text.length; i++) {
    hash ^= text.charCodeAt(i)
    hash = Math.imul(hash, 0x01000193) >>> 0
  }
  return hash.toString(16).padStart(8, '0')
}

export default { clientToken, fnv1a32 }

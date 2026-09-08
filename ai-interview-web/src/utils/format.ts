import dayjs from 'dayjs'

/**
 * 时间 / 分数 / 文本格式化工具
 */

/** 默认时间格式 */
export const DATE_TIME_FORMAT = 'YYYY-MM-DD HH:mm:ss'
export const DATE_FORMAT = 'YYYY-MM-DD'

/** 格式化日期时间；空值返回占位符 */
export function formatTime(value?: string | number | Date | null, placeholder = '-'): string {
  if (value === null || value === undefined || value === '') return placeholder
  const d = dayjs(value as any)
  return d.isValid() ? d.format(DATE_TIME_FORMAT) : placeholder
}

/** 格式化日期 */
export function formatDate(value?: string | number | Date | null, placeholder = '-'): string {
  if (value === null || value === undefined || value === '') return placeholder
  const d = dayjs(value as any)
  return d.isValid() ? d.format(DATE_FORMAT) : placeholder
}

/** 相对时间（刚刚 / 3 分钟前 / 2 小时前 / 3 天前） */
export function fromNow(value?: string | number | Date | null, placeholder = '-'): string {
  if (value === null || value === undefined || value === '') return placeholder
  const d = dayjs(value as any)
  if (!d.isValid()) return placeholder
  const diffSec = dayjs().diff(d, 'second')
  if (diffSec < 60) return '刚刚'
  if (diffSec < 3600) return `${Math.floor(diffSec / 60)} 分钟前`
  if (diffSec < 86400) return `${Math.floor(diffSec / 3600)} 小时前`
  if (diffSec < 86400 * 30) return `${Math.floor(diffSec / 86400)} 天前`
  return d.format(DATE_FORMAT)
}

/** 时长（毫秒 → 秒，保留 1 位） */
export function formatCost(ms?: number | null): string {
  if (ms === null || ms === undefined || Number.isNaN(ms)) return '-'
  if (ms < 1000) return `${Math.round(ms)} ms`
  return `${(ms / 1000).toFixed(1)} s`
}

/** 分数格式化：保留 1 位小数并去掉多余的 .0 */
export function formatScore(score?: number | null, placeholder = '-'): string {
  if (score === null || score === undefined || Number.isNaN(Number(score))) return placeholder
  const n = Number(score)
  return Number.isInteger(n) ? String(n) : n.toFixed(1)
}

/** 分数 → 等级文案 */
export function scoreLevel(score?: number | null): string {
  const n = Number(score || 0)
  if (n >= 90) return '优秀'
  if (n >= 80) return '良好'
  if (n >= 70) return '中等'
  if (n >= 60) return '及格'
  return '待提升'
}

/** 分数 → Element Plus Tag 类型 */
export function scoreTagType(score?: number | null): 'success' | 'warning' | 'danger' | 'info' {
  const n = Number(score || 0)
  if (n >= 80) return 'success'
  if (n >= 60) return 'warning'
  return 'danger'
}

/** 文本截断（超出加省略号） */
export function truncate(text?: string | null, max = 80, suffix = '…'): string {
  if (!text) return ''
  const arr = Array.from(String(text))
  if (arr.length <= max) return String(text)
  return arr.slice(0, max).join('') + suffix
}

/** 计算字符数（中文按 1 字符计，与后端 BR-03 一致） */
export function charCount(text?: string | null): number {
  if (!text) return 0
  return Array.from(String(text).trim()).length
}

/** 将可能是字符串的多值字段（后端可能返回逗号串或 JSON 串）规范为数组 */
export function toArray(value?: string[] | string | null): string[] {
  if (!value) return []
  if (Array.isArray(value)) return value.filter((i) => i !== null && i !== undefined && i !== '')
  const raw = String(value).trim()
  if (!raw) return []
  if (raw.startsWith('[')) {
    try {
      const parsed = JSON.parse(raw)
      if (Array.isArray(parsed)) return parsed.map((i) => String(i))
    } catch (e) {
      /* 退回按分隔符切 */
    }
  }
  return raw
    .split(/[,，;；\n]/)
    .map((s) => s.trim())
    .filter(Boolean)
}

/** 字节友好的文件大小展示 */
export function formatFileSize(bytes?: number | null): string {
  if (!bytes && bytes !== 0) return '-'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(2)} MB`
}

export default {
  formatTime,
  formatDate,
  fromNow,
  formatCost,
  formatScore,
  scoreLevel,
  scoreTagType,
  truncate,
  charCount,
  toArray,
  formatFileSize,
}

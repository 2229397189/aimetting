import MarkdownIt from 'markdown-it'

/**
 * markdown-it 实例：用于渲染 AI 返回的 Markdown 点评 / 报告内容。
 * 安全策略（BR-22）：
 *  1. html: false —— 禁止原始 HTML 直接透传；
 *  2. linkify 关闭 + 自定义 link 校验，仅允许 http/https；
 *  3. 图片统一降级为纯文本，避免外链追踪与 XSS 面。
 */

const ALLOWED_LINK_PROTOCOLS = ['http:', 'https:']

/** 校验链接协议，非法协议返回 '#' */
function sanitizeHref(href: string): string {
  const raw = String(href || '').trim()
  if (!raw) return '#'
  // 允许相对锚点
  if (raw.startsWith('#') || raw.startsWith('/')) return raw
  try {
    const url = new URL(raw, window.location.origin)
    if (ALLOWED_LINK_PROTOCOLS.includes(url.protocol)) return url.href
  } catch (e) {
    return '#'
  }
  return '#'
}

const md: MarkdownIt = new MarkdownIt({
  html: false, // 关键：禁止原始 HTML
  linkify: false, // 不自动识别链接，减少攻击面
  breaks: true, // 单换行转 <br>，符合中文排版
  typographer: false,
  xhtmlOut: false,
})

// 链接 protocol 白名单
const defaultLinkOpen =
  md.renderer.rules.link_open ||
  ((tokens, idx, options, _env, self) => self.renderToken(tokens, idx, options))
md.renderer.rules.link_open = (tokens, idx, options, env, self) => {
  const token = tokens[idx]
  const hrefIndex = token.attrIndex('href')
  if (hrefIndex >= 0 && token.attrs) {
    token.attrs[hrefIndex][1] = sanitizeHref(token.attrs[hrefIndex][1])
  }
  // 外链一律新窗口打开且不允许 tabnabbing
  token.attrSet('target', '_blank')
  token.attrSet('rel', 'noopener noreferrer nofollow')
  return defaultLinkOpen(tokens, idx, options, env, self)
}

// 图片降级为纯文本链接，避免远程资源注入
md.renderer.rules.image = (tokens, idx) => {
  const token = tokens[idx]
  const srcIndex = token.attrIndex('src')
  const src = srcIndex >= 0 && token.attrs ? token.attrs[srcIndex][1] : ''
  const alt = token.content || ''
  const safeSrc = sanitizeHref(src)
  return `[图片${alt ? '：' + alt : ''}](${safeSrc === '#' ? '已拦截' : safeSrc})`
}

/** 渲染 Markdown 为已消毒的 HTML 字符串 */
export function renderMarkdown(source?: string | null): string {
  if (!source) return ''
  try {
    return md.render(String(source))
  } catch (e) {
    // 渲染异常时退回纯文本转义，避免整页崩溃
    return escapeHtml(String(source))
  }
}

/** 简单 HTML 转义（用于纯文本兜底展示） */
export function escapeHtml(text: string): string {
  return String(text)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

export default { renderMarkdown, escapeHtml, md }

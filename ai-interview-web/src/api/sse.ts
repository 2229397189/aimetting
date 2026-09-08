import type { SseEnvelope, SseEventType } from '@/types'
import { getRaw, StorageKey } from '@/utils/storage'

/**
 * SSE 客户端（ARCHITECTURE.md 6.4）
 *
 * 为什么不用 EventSource：
 *  - EventSource 只支持 GET，无法提交答案（POST /answers）与携带自定义 Header；
 *  - 因此统一使用 `fetch + response.body.getReader() + TextDecoder` 手工解析。
 *
 * 能力：
 *  - 按 `\n\n` 切帧，解析 `event:` / `data:` 前缀，`data` 为单行 JSON；
 *  - 维护 lastSeq，丢弃 `seq <= lastSeq` 的乱序 / 重复事件；
 *  - AbortController 支持 `stop()` 中断（用户点"停止生成"）；
 *  - 收到 `degraded: true` 的信封时通过 `onDegraded` 回调通知 UI；
 *  - 心跳 `: ping` 注释行直接忽略；`:` 开头不解析。
 */

export interface UseSseOptions {
  /** 请求方法，默认 POST（提交答案为 POST；取下一题为 GET） */
  method?: 'GET' | 'POST'
  /** 自定义请求头（会自动补充 Authorization / Accept） */
  headers?: Record<string, string>
  /** 请求体（POST 时 JSON 序列化） */
  body?: unknown
  /** 每收到一个合法事件触发一次 */
  onEvent?: (envelope: SseEnvelope) => void
  /** 流异常（网络中断 / 解析失败 / 业务 error 事件） */
  onError?: (error: Error & { code?: string }) => void
  /** 流结束（正常结束 / 被中断 / 出错后结束） */
  onDone?: (reason: SseDoneReason) => void
  /** 降级提示（envelope.degraded === true） */
  onDegraded?: (envelope: SseEnvelope) => void
  /** 是否立即开始，默认 true */
  autoStart?: boolean
}

export type SseDoneReason = 'complete' | 'aborted' | 'error' | 'server-done'

export interface SseHandle {
  /** 开始（或重新开始）连接 */
  start: () => Promise<void>
  /** 中断连接（保留已收到内容） */
  stop: () => void
  /** 当前已处理的最大 seq */
  lastSeq: () => number
  /** 是否仍在运行 */
  isActive: () => boolean
}

/** 解析单个 SSE 帧（形如 `event: comment\ndata: {...}\n\n`） */
export function parseSseFrame(frame: string): { event?: string; data?: string } | null {
  if (!frame) return null
  let event: string | undefined
  const dataLines: string[] = []
  frame.split(/\r?\n/).forEach((line) => {
    if (!line) return
    if (line.startsWith(':')) return // 心跳注释
    if (line.startsWith('event:')) {
      event = line.slice(6).trim()
    } else if (line.startsWith('data:')) {
      dataLines.push(line.slice(5).trim())
    } else if (line.startsWith('id:')) {
      // 标准 SSE id 字段，暂不参与 seq 判定（后端用 payload.seq）
      return
    } else if (line.startsWith('retry:')) {
      return
    }
  })
  if (dataLines.length === 0) return null
  return { event, data: dataLines.join('\n') }
}

/**
 * 建立 SSE 连接。
 * @param url 相对地址，如 `/interview/sessions/1/answers`
 * @param options 配置项
 */
export function useSse(url: string, options: UseSseOptions = {}): SseHandle {
  const {
    method = 'POST',
    headers = {},
    body,
    onEvent,
    onError,
    onDone,
    onDegraded,
    autoStart = true,
  } = options

  const controller: AbortController = new AbortController()
  let seq = 0
  let active = false
  let stoppedByUser = false

  /** 统一结束处理，保证 onDone 只触发一次 */
  let finished = false
  const finish = (reason: SseDoneReason, error?: Error & { code?: string }) => {
    if (finished) return
    finished = true
    active = false
    if (error && onError) onError(error)
    if (onDone) onDone(reason)
  }

  /** 处理一帧数据 */
  const handleFrame = (frame: string): void => {
    const parsed = parseSseFrame(frame)
    if (!parsed || !parsed.data) return
    let envelope: SseEnvelope
    try {
      envelope = JSON.parse(parsed.data) as SseEnvelope
    } catch (e) {
      // 单行 JSON 解析失败：忽略该帧，避免整条流中断
      return
    }
    if (!envelope || !envelope.type) return

    // seq 乱序 / 重复保护（后端保证同连接内单调递增）
    const currentSeq = typeof envelope.seq === 'number' ? envelope.seq : -1
    if (currentSeq > 0 && currentSeq <= seq) return
    if (currentSeq > 0) seq = currentSeq

    // 降级提示
    if (envelope.degraded === true && onDegraded) onDegraded(envelope)

    // 业务 error 事件
    if (envelope.type === ('error' as SseEventType)) {
      const payload = (envelope.payload || {}) as { code?: string; message?: string; retryable?: boolean }
      const err = new Error(payload.message || 'AI 服务异常') as Error & { code?: string }
      err.code = payload.code || 'C0000'
      finish('error', err)
      return
    }

    if (onEvent) onEvent(envelope)

    // done 事件：流正常结束
    if (envelope.type === ('done' as SseEventType)) {
      finish('server-done')
    }
  }

  const start = async (): Promise<void> => {
    if (active) return
    active = true
    finished = false
    stoppedByUser = false

    const token = getRaw(StorageKey.Token)
    const requestHeaders: Record<string, string> = {
      Accept: 'text/event-stream',
      'Cache-Control': 'no-cache',
      ...headers,
    }
    if (token) requestHeaders.Authorization = `Bearer ${token}`
    if (method === 'POST') requestHeaders['Content-Type'] = 'application/json;charset=UTF-8'

    try {
      const response = await fetch(url, {
        method,
        headers: requestHeaders,
        body: method === 'POST' && body !== undefined ? JSON.stringify(body) : undefined,
        signal: controller.signal,
      })

      if (!response.ok) {
        let message = `请求失败（HTTP ${response.status}）`
        try {
          const text = await response.text()
          if (text) {
            const maybe = JSON.parse(text)
            message = maybe?.message || message
          }
        } catch (e) {
          /* 响应体非 JSON，使用默认文案 */
        }
        const err = new Error(message) as Error & { code?: string }
        err.code = String(response.status)
        finish('error', err)
        return
      }
      if (!response.body) {
        const err = new Error('当前浏览器不支持流式响应') as Error & { code?: string }
        err.code = 'B0000'
        finish('error', err)
        return
      }

      const reader = response.body.getReader()
      const decoder = new TextDecoder('utf-8')
      let buffer = ''

      while (true) {
        const { done, value } = await reader.read()
        if (done) break
        buffer += decoder.decode(value, { stream: true })
        // 按空行切帧，保留残余半帧到下一轮
        let sepIndex = buffer.indexOf('\n\n')
        while (sepIndex !== -1) {
          const frame = buffer.slice(0, sepIndex)
          buffer = buffer.slice(sepIndex + 2)
          handleFrame(frame)
          sepIndex = buffer.indexOf('\n\n')
        }
        // 兼容 \r\n\r\n
        let crlfIndex = buffer.indexOf('\r\n\r\n')
        while (crlfIndex !== -1) {
          const frame = buffer.slice(0, crlfIndex)
          buffer = buffer.slice(crlfIndex + 4)
          handleFrame(frame)
          crlfIndex = buffer.indexOf('\r\n\r\n')
        }
      }
      // 收尾：处理最后一帧（后端可能不以 \n\n 结尾）
      if (buffer.trim()) handleFrame(buffer)
      buffer = ''
      finish(stoppedByUser ? 'aborted' : 'complete')
    } catch (e: unknown) {
      if (stoppedByUser || (e as Error)?.name === 'AbortError') {
        finish('aborted')
        return
      }
      const err = (e instanceof Error ? e : new Error(String(e))) as Error & { code?: string }
      if (!err.code) err.code = 'NETWORK'
      finish('error', err)
    }
  }

  const stop = (): void => {
    stoppedByUser = true
    active = false
    try {
      controller.abort()
    } catch (e) {
      /* 已中断 */
    }
  }

  if (autoStart) {
    void start()
  }

  return {
    start,
    stop,
    lastSeq: () => seq,
    isActive: () => active,
  }
}

/** 统一拼接后端接口地址（带 /api 前缀） */
export function apiUrl(path: string): string {
  const base = '/api'
  if (path.startsWith('http')) return path
  return `${base}${path.startsWith('/') ? path : '/' + path}`
}

export default { useSse, parseSseFrame, apiUrl }

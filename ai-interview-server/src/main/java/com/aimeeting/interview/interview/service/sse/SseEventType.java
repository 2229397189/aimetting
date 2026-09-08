package com.aimeeting.interview.interview.service.sse;

/**
 * SSE 事件类型（与前端 SseEventType 一致，共 7 种）。
 */
public enum SseEventType {
    question,
    score,
    comment,
    follow_up,
    progress,
    done,
    error
}

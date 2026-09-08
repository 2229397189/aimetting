package com.aimeeting.interview.interview.service;

/**
 * 评分正文增量出口：由 SSE 侧实现，把 AI 的正文增量实时推给前端。
 */
public interface AiStreamSink {

    /**
     * 收到一段增量正文。
     *
     * @param delta 增量文本
     */
    void acceptDelta(String delta);

    /** 正文结束（补发 finish 帧）。 */
    void acceptFinish();
}

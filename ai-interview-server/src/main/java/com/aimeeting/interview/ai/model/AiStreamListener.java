package com.aimeeting.interview.ai.model;

/**
 * AI 流式回调：增量文本 / 完成 / 错误。
 */
public interface AiStreamListener {

    /** 收到一段增量文本。 */
    void onDelta(String delta);

    /** 流正常结束，附带完整结果（含 token 统计）。 */
    void onComplete(AiTextResult result);

    /** 流异常结束。 */
    void onError(Throwable t);
}

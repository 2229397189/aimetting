package com.aimeeting.interview.ai.model;

/**
 * AI 业务类型，对应 t_ai_call_log.biz_type 与不同 prompt / 模型参数。
 */
public enum AiBizType {
    QUESTION,
    EVALUATE,
    FOLLOW_UP,
    RESUME,
    REPORT
}

package com.aimeeting.interview.ai.model;

/**
 * AI 调用失败原因归类。
 *
 * <p>定义于 AI 层，但被 {@code common} 的 {@code RemoteException} 引用（用于重试/降级决策），
 * 故在 M1 阶段即落盘，避免后续模块反向修改通用层。
 */
public enum AiErrorType {

    /** 超时。 */
    TIMEOUT,

    /** 服务不可用（连接失败 / 5xx）。 */
    UNAVAILABLE,

    /** 账户额度耗尽 / 计费异常（402）。永久性错误，重试无意义且会浪费调用。 */
    QUOTA,

    /** 被限流（429）。 */
    RATE_LIMIT,

    /** 返回内容无法解析（非 JSON / 字段缺失 / 分数越界）。 */
    INVALID_RESPONSE,

    /** 参数错误（不可重试）。 */
    PARAMS
}

package com.aimeeting.interview.ai.model;

import java.time.Duration;

/**
 * AI 调用阶段：超时 / 单飞 / 熔断分组的维度（BR-08）。
 *
 * <p>每个阶段绑定 {@link AiBizType} 与默认超时，供 {@code AiGuardService} 统一管控。
 */
public enum AiStage {

    QUESTION_GEN(Duration.ofSeconds(60),  AiBizType.QUESTION),
    EVALUATION  (Duration.ofSeconds(90),  AiBizType.EVALUATE),
    FOLLOW_UP_GEN(Duration.ofSeconds(90), AiBizType.FOLLOW_UP),
    RESUME_PARSE(Duration.ofSeconds(90),  AiBizType.RESUME),
    REPORT_GEN  (Duration.ofSeconds(120), AiBizType.REPORT);

    public final Duration timeout;
    public final AiBizType bizType;

    AiStage(Duration timeout, AiBizType bizType) {
        this.timeout = timeout;
        this.bizType = bizType;
    }
}

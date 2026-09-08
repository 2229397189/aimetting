package com.aimeeting.interview.interview.service.sse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * error 事件 payload（架构文档 §6.3）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorPayload {

    /** 业务错误码，如 C0502。 */
    private String code;

    /** 错误文案。 */
    private String message;

    /** 是否可重试。 */
    private boolean retryable;
}

package com.aimeeting.interview.common.convention.exception;

import com.aimeeting.interview.ai.model.AiErrorType;
import com.aimeeting.interview.common.convention.errorcode.IErrorCode;
import lombok.Getter;

/**
 * 远程调用异常（C 类错误码），主要用于 AI 服务调用失败。
 *
 * <p>额外携带 {@link AiErrorType}，供上层判定是否可重试 / 是否需要降级
 * （仅 TIMEOUT 与 UNAVAILABLE 可重试，见 BR-09）。
 */
@Getter
public class RemoteException extends AbstractException {

    private static final long serialVersionUID = 1L;

    /** AI 调用失败归类，用于重试与降级决策。 */
    private final AiErrorType errorType;

    /**
     * @param message   自定义文案
     * @param throwable 原始异常
     * @param errorCode 错误码
     * @param errorType AI 错误归类
     */
    public RemoteException(String message, Throwable throwable, IErrorCode errorCode, AiErrorType errorType) {
        super(message, throwable, errorCode);
        this.errorType = errorType == null ? AiErrorType.UNAVAILABLE : errorType;
    }

    /**
     * @param errorCode 错误码
     * @param errorType AI 错误归类
     */
    public RemoteException(IErrorCode errorCode, AiErrorType errorType) {
        this(null, null, errorCode, errorType);
    }

    /**
     * @param message   自定义文案
     * @param errorCode 错误码
     * @param errorType AI 错误归类
     */
    public RemoteException(String message, IErrorCode errorCode, AiErrorType errorType) {
        this(message, null, errorCode, errorType);
    }
}

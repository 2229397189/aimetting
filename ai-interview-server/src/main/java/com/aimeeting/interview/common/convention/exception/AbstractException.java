package com.aimeeting.interview.common.convention.exception;

import com.aimeeting.interview.common.convention.errorcode.IErrorCode;
import lombok.Getter;

/**
 * 业务异常基类。
 *
 * <p>三级异常体系：
 * <ul>
 *   <li>{@link ClientException} —— 客户端错误（A 类），HTTP 4xx</li>
 *   <li>{@link ServiceException} —— 系统错误（B 类），HTTP 5xx / 409</li>
 *   <li>{@link RemoteException} —— 远程 AI 错误（C 类），HTTP 502 / 429 / 504</li>
 * </ul>
 *
 * <p>所有子类都必须携带 {@link IErrorCode}，由
 * {@code GlobalExceptionHandler} 统一转成 {@code Result}。
 */
@Getter
public abstract class AbstractException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** 业务错误码（A/B/C 分级）。 */
    public final String errorCode;

    /** 面向用户的错误文案（未显式指定时取错误码默认文案）。 */
    public final String errorMessage;

    /**
     * @param message    自定义错误文案，为空时回落到错误码默认文案
     * @param throwable  原始异常
     * @param errorCode  错误码枚举
     */
    public AbstractException(String message, Throwable throwable, IErrorCode errorCode) {
        super(message, throwable);
        this.errorCode = errorCode.code();
        this.errorMessage = (message == null || message.isBlank()) ? errorCode.message() : message;
    }
}

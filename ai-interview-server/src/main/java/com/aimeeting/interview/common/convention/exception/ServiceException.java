package com.aimeeting.interview.common.convention.exception;

import com.aimeeting.interview.common.convention.errorcode.IErrorCode;

/**
 * 系统异常（B 类错误码），对应 HTTP 5xx 或 409（状态机冲突）。
 *
 * <p>典型场景：DB 不可用、账号被禁用、会话状态非法流转、资源归属校验失败。
 */
public class ServiceException extends AbstractException {

    private static final long serialVersionUID = 1L;

    /**
     * @param errorCode 错误码
     */
    public ServiceException(IErrorCode errorCode) {
        this(null, null, errorCode);
    }

    /**
     * @param message   自定义文案
     * @param errorCode 错误码
     */
    public ServiceException(String message, IErrorCode errorCode) {
        this(message, null, errorCode);
    }

    /**
     * @param message    自定义文案
     * @param throwable  原始异常
     * @param errorCode  错误码
     */
    public ServiceException(String message, Throwable throwable, IErrorCode errorCode) {
        super(message, throwable, errorCode);
    }
}

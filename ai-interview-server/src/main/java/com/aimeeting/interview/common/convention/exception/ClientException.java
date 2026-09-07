package com.aimeeting.interview.common.convention.exception;

import com.aimeeting.interview.common.convention.errorcode.IErrorCode;

/**
 * 客户端异常（A 类错误码），对应 HTTP 4xx。
 *
 * <p>典型场景：参数校验失败、用户名已存在、Token 缺失/过期、无权限、限流。
 */
public class ClientException extends AbstractException {

    private static final long serialVersionUID = 1L;

    /**
     * @param errorCode 错误码
     */
    public ClientException(IErrorCode errorCode) {
        this(null, null, errorCode);
    }

    /**
     * @param message   自定义文案
     * @param errorCode 错误码
     */
    public ClientException(String message, IErrorCode errorCode) {
        this(message, null, errorCode);
    }

    /**
     * @param message    自定义文案
     * @param throwable  原始异常
     * @param errorCode  错误码
     */
    public ClientException(String message, Throwable throwable, IErrorCode errorCode) {
        super(message, throwable, errorCode);
    }
}

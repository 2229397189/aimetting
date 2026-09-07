package com.aimeeting.interview.common.convention.result;

import com.aimeeting.interview.common.convention.errorcode.BaseErrorCode;
import com.aimeeting.interview.common.convention.errorcode.IErrorCode;
import com.aimeeting.interview.common.convention.exception.AbstractException;
import com.aimeeting.interview.common.util.MdcUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * {@link Result} 工厂类，统一收口成功 / 失败返回体的构造。
 *
 * <p>所有 Controller 只允许通过本类包装返回值，保证 code / message / requestId 三要素不缺失。
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Results {

    private static final String SUCCESS_MESSAGE = "OK";

    /**
     * 构造无数据的成功返回体。
     *
     * @return {@code Result<Void>}
     */
    public static Result<Void> success() {
        return buildSuccess(null);
    }

    /**
     * 构造带数据的成功返回体。
     *
     * @param data 业务数据
     * @param <T>  数据类型
     * @return {@code Result<T>}
     */
    public static <T> Result<T> success(T data) {
        return buildSuccess(data);
    }

    /**
     * 构造默认系统错误返回体（B0001）。
     *
     * @return {@code Result<Void>}
     */
    public static Result<Void> failure() {
        return failure(BaseErrorCode.SERVICE_ERROR);
    }

    /**
     * 基于业务异常构造失败返回体。
     *
     * @param exception 业务异常（含错误码）
     * @return {@code Result<Void>}
     */
    public static Result<Void> failure(AbstractException exception) {
        Result<Void> result = new Result<>();
        result.setCode(exception.getErrorCode());
        result.setMessage(exception.getErrorMessage());
        result.setRequestId(MdcUtil.getRequestId());
        return result;
    }

    /**
     * 基于错误码枚举构造失败返回体。
     *
     * @param errorCode 错误码枚举
     * @return {@code Result<Void>}
     */
    public static Result<Void> failure(IErrorCode errorCode) {
        return failure(errorCode.code(), errorCode.message());
    }

    /**
     * 基于错误码与文案构造失败返回体。
     *
     * @param errorCode    错误码
     * @param errorMessage 错误文案
     * @return {@code Result<Void>}
     */
    public static Result<Void> failure(String errorCode, String errorMessage) {
        Result<Void> result = new Result<>();
        result.setCode(errorCode);
        result.setMessage(errorMessage);
        result.setRequestId(MdcUtil.getRequestId());
        return result;
    }

    /**
     * 泛型版失败返回体，便于在 {@code return Results.failure(code, msg, XxxResp.class)}
     * 场景下由编译器推导 {@code Result<XxxResp>}，避免调用方强转。
     *
     * @param errorCode    错误码
     * @param errorMessage 错误文案
     * @param unused       仅用于泛型推导的目标类型，实现体不使用
     * @param <T>          目标数据类型
     * @return {@code Result<T>}
     */
    public static <T> Result<T> failure(String errorCode, String errorMessage, Class<T> unused) {
        Result<T> result = new Result<>();
        result.setCode(errorCode);
        result.setMessage(errorMessage);
        result.setRequestId(MdcUtil.getRequestId());
        return result;
    }

    @SuppressWarnings("unchecked")
    private static <T> Result<T> buildSuccess(T data) {
        Result<T> result = new Result<>();
        result.setCode(Result.SUCCESS_CODE);
        result.setMessage(SUCCESS_MESSAGE);
        result.setData(data);
        result.setRequestId(MdcUtil.getRequestId());
        return result;
    }
}

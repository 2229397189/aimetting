package com.aimeeting.interview.common.web;

import com.aimeeting.interview.common.convention.errorcode.BaseErrorCode;
import com.aimeeting.interview.common.convention.errorcode.IErrorCode;
import org.springframework.http.HttpStatus;

/**
 * 业务错误码 -&gt; HTTP 状态码映射。
 *
 * <p>映射规则（与架构文档 3.1 一致）：
 * <ul>
 *   <li>{@code A02*}（Token 相关）-&gt; 401</li>
 *   <li>{@code A03*} / {@code B0303}（越权）-&gt; 403</li>
 *   <li>{@code B0301} / {@code B0302}（状态机冲突）-&gt; 409</li>
 *   <li>{@code C0502}（舱壁打满）-&gt; 429</li>
 *   <li>{@code C0503} / {@code C0504}（等待 / 调用超时）-&gt; 504</li>
 *   <li>其余 {@code A*} -&gt; 400；其余 {@code B*} -&gt; 500；其余 {@code C*} -&gt; 502</li>
 * </ul>
 */
public final class HttpStatusResolver {

    private HttpStatusResolver() {
    }

    /**
     * 根据业务错误码解析 HTTP 状态码。
     *
     * @param errorCode 业务错误码，如 {@code A0201}
     * @return HTTP 状态码，无法识别时返回 500
     */
    public static HttpStatus resolve(String errorCode) {
        if (errorCode == null || errorCode.isBlank()) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        String code = errorCode.trim().toUpperCase();
        if ("0".equals(code)) {
            return HttpStatus.OK;
        }
        // 精确匹配优先（状态机 / 越权 / 舱壁 / 超时）
        switch (code) {
            case "B0301":
            case "B0302":
                return HttpStatus.CONFLICT;
            case "B0303":
            case "A0301":
                return HttpStatus.FORBIDDEN;
            case "C0502":
                return HttpStatus.TOO_MANY_REQUESTS;
            case "C0503":
            case "C0504":
                return HttpStatus.GATEWAY_TIMEOUT;
            case "A0501":
                return HttpStatus.TOO_MANY_REQUESTS;
            default:
                break;
        }
        char prefix = code.charAt(0);
        if (prefix == 'A') {
            return code.startsWith("A02") ? HttpStatus.UNAUTHORIZED : HttpStatus.BAD_REQUEST;
        }
        if (prefix == 'B') {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        if (prefix == 'C') {
            return HttpStatus.BAD_GATEWAY;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    /**
     * 根据错误码枚举解析 HTTP 状态码。
     *
     * @param errorCode 错误码枚举
     * @return HTTP 状态码
     */
    public static HttpStatus resolve(IErrorCode errorCode) {
        return resolve(errorCode == null ? null : errorCode.code());
    }

    /**
     * 默认系统错误的 HTTP 状态码（B0001）。
     *
     * @return 500
     */
    public static HttpStatus defaultStatus() {
        return resolve(BaseErrorCode.SERVICE_ERROR.code());
    }
}

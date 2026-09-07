package com.aimeeting.interview.common.convention.result;

import java.io.Serializable;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 全局统一返回体 {@code Result<T>}。
 *
 * <p>约定：
 * <ul>
 *   <li>{@code code}：业务错误码，成功固定为 {@link #SUCCESS_CODE}（"0"）；
 *       失败为 A/B/C 三级错误码（A 客户端 / B 系统 / C 远程 AI）。</li>
 *   <li>{@code requestId}：全链路追踪 ID，由 {@code RequestIdFilter} 写入 MDC 后回填，
 *       便于前端报错时定位日志。</li>
 *   <li>{@code data}：业务数据，失败时通常为 null。</li>
 * </ul>
 *
 * @param <T> 业务数据类型
 */
@Data
@Accessors(chain = true)
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 成功错误码。 */
    public static final String SUCCESS_CODE = "0";

    /** 业务错误码，"0" 表示成功。 */
    private String code;

    /** 提示信息，成功时为 "OK"。 */
    private String message;

    /** 业务数据。 */
    private T data;

    /** 全链路请求 ID。 */
    private String requestId;

    /**
     * 判断本次调用是否成功。
     *
     * @return code 为 "0" 时返回 true
     */
    public boolean isSuccess() {
        return SUCCESS_CODE.equals(code);
    }
}

package com.aimeeting.interview.common.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.slf4j.MDC;

/**
 * MDC 工具，用于全链路 requestId 透传。
 *
 * <p>链路：{@code RequestIdFilter} 生成 requestId -&gt; 写入 MDC -&gt;
 * 日志 pattern 输出 {@code %X{requestId}} -&gt; {@code Result.requestId} 与响应头
 * {@code X-Request-Id} 回传前端。
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MdcUtil {

    /** MDC 中 requestId 的键名，与 logback pattern 中的 {@code %X{requestId}} 对应。 */
    public static final String REQUEST_ID = "requestId";

    /**
     * 写入 requestId（值为 null 时忽略）。
     *
     * @param requestId 请求 ID
     */
    public static void putRequestId(String requestId) {
        if (requestId != null) {
            MDC.put(REQUEST_ID, requestId);
        }
    }

    /**
     * 读取当前 requestId。
     *
     * @return requestId，不存在时返回空串（保证返回体字段不为 null）
     */
    public static String getRequestId() {
        String value = MDC.get(REQUEST_ID);
        return value == null ? "" : value;
    }

    /**
     * 写入自定义 MDC 键值。
     *
     * @param key   键
     * @param value 值
     */
    public static void put(String key, String value) {
        if (key != null && value != null) {
            MDC.put(key, value);
        }
    }

    /**
     * 移除指定键。
     *
     * @param key 键
     */
    public static void remove(String key) {
        if (key != null) {
            MDC.remove(key);
        }
    }

    /** 清空当前线程的 MDC，防止线程复用导致串号。 */
    public static void clear() {
        MDC.clear();
    }
}

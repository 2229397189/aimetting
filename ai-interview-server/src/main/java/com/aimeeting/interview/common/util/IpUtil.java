package com.aimeeting.interview.common.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 客户端真实 IP 解析工具，用于限流与审计。
 *
 * <p>按常见反向代理头优先级依次尝试：
 * {@code X-Forwarded-For} -&gt; {@code X-Real-IP} -&gt; {@code Proxy-Client-IP} -&gt;
 * {@code WL-Proxy-Client-IP} -&gt; {@code HTTP_CLIENT_IP} -&gt; {@code HTTP_X_FORWARDED_FOR} -&gt;
 * {@code request.getRemoteAddr()}。
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class IpUtil {

    /** 未知 IP 的兜底值。 */
    public static final String UNKNOWN = "unknown";

    private static final String[] IP_HEADERS = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR"
    };

    /**
     * 解析客户端 IP。
     *
     * @param request HTTP 请求，允许为 null
     * @return IP 字符串，无法解析时返回 {@code "0.0.0.0"}
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "0.0.0.0";
        }
        for (String header : IP_HEADERS) {
            String value = request.getHeader(header);
            String ip = firstIp(value);
            if (ip != null) {
                return ip;
            }
        }
        String remoteAddr = request.getRemoteAddr();
        return remoteAddr == null ? "0.0.0.0" : remoteAddr;
    }

    /**
     * 从多段 IP 串（逗号分隔）中取第一段有效 IP。
     *
     * @param value 原始头值
     * @return 有效 IP，无有效值返回 null
     */
    public static String firstIp(String value) {
        if (value == null || value.isEmpty() || UNKNOWN.equalsIgnoreCase(value)) {
            return null;
        }
        int commaIndex = value.indexOf(',');
        String candidate = commaIndex > 0 ? value.substring(0, commaIndex) : value;
        return candidate.trim();
    }
}

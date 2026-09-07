package com.aimeeting.interview.common.web;

import com.aimeeting.interview.common.util.MdcUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * requestId 过滤器：全链路追踪的第一环。
 *
 * <p>职责：
 * <ol>
 *   <li>为每次请求生成 32 位（去掉横杠的 UUID）requestId；
 *       若上游已传 {@code X-Request-Id} 则复用，保证跨服务链路连续。</li>
 *   <li>写入 MDC，供 logback pattern 的 {@code %X{requestId}} 输出。</li>
 *   <li>写入响应头 {@code X-Request-Id}，同时写入 request attribute，
 *       便于 {@code GlobalExceptionHandler} 与 Controller 回填到 {@code Result.requestId}。</li>
 *   <li>请求结束后清理 MDC，避免线程复用串号。</li>
 * </ol>
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    /** 响应头名称。 */
    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    /** request attribute 名称（供异常处理器使用）。 */
    public static final String REQUEST_ID_ATTRIBUTE = "AI_INTERVIEW_REQUEST_ID";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestId = resolveRequestId(request);
        MdcUtil.putRequestId(requestId);
        request.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MdcUtil.clear();
        }
    }

    /**
     * 优先复用上游传递的 requestId，否则生成新的 32 位 UUID。
     *
     * @param request 当前请求
     * @return requestId
     */
    private static String resolveRequestId(HttpServletRequest request) {
        String incoming = request.getHeader(REQUEST_ID_HEADER);
        if (incoming != null && !incoming.isBlank() && incoming.length() <= 64) {
            return incoming;
        }
        return UUID.randomUUID().toString().replace("-", "");
    }
}

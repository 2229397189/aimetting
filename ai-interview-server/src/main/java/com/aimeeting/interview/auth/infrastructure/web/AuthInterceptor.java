package com.aimeeting.interview.auth.infrastructure.web;

import com.aimeeting.interview.auth.dao.entity.UserDO;
import com.aimeeting.interview.auth.dao.repository.UserRepository;
import com.aimeeting.interview.auth.service.JwtClaims;
import com.aimeeting.interview.auth.service.JwtTokenProvider;
import com.aimeeting.interview.auth.service.TokenBlacklistService;
import com.aimeeting.interview.common.convention.context.UserContext;
import com.aimeeting.interview.common.convention.errorcode.BaseErrorCode;
import com.aimeeting.interview.common.convention.exception.ClientException;
import com.aimeeting.interview.common.convention.exception.ServiceException;
import com.aimeeting.interview.config.SecurityProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 鉴权拦截器（U-03）。
 *
 * <p>处理顺序：
 * <ol>
 *   <li>{@code OPTIONS} 预检请求直接放行（CORS 由 {@code CorsFilter} 前置处理）。</li>
 *   <li>命中白名单（{@code ai-interview.security.permit-paths}，Ant 匹配）直接放行。</li>
 *   <li>缺失 {@code Authorization: Bearer <token>} -&gt; A0201（HTTP 401）。</li>
 *   <li>JWT 解析失败：过期 -&gt; A0202；其它 -&gt; A0201。</li>
 *   <li>jti 命中黑名单 -&gt; A0201（退出登录后旧令牌立即失效）。</li>
 *   <li>账号被禁用 -&gt; B0103。</li>
 *   <li>登录态写入 <b>request attribute</b>（键 {@link UserContext#REQUEST_KEY}）
 *       —— <b>关键点：不使用 ThreadLocal</b>，避免线程池复用导致登录态串号。</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    private final TokenBlacklistService tokenBlacklistService;

    private final UserRepository userRepository;

    private final SecurityProperties securityProperties;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        String uri = request.getRequestURI();
        if (isPermitPath(uri)) {
            return true;
        }
        String token = extractToken(request.getHeader(securityProperties.getTokenHeader()));
        if (token == null || token.isBlank()) {
            throw new ClientException(BaseErrorCode.TOKEN_MISSING);
        }
        // 过期抛 A0202，其它非法令牌抛 A0201
        JwtClaims claims = jwtTokenProvider.parse(token);
        if (claims.userId() == null) {
            throw new ClientException(BaseErrorCode.TOKEN_MISSING);
        }
        String jti = jwtTokenProvider.jtiOf(token);
        if (jti != null && tokenBlacklistService.isBlacklisted(jti)) {
            throw new ClientException("令牌已失效，请重新登录", BaseErrorCode.TOKEN_MISSING);
        }
        UserDO user = userRepository.findById(claims.userId());
        if (user == null) {
            throw new ClientException(BaseErrorCode.TOKEN_MISSING);
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new ServiceException(BaseErrorCode.ACCOUNT_DISABLED);
        }
        UserContext userContext = new UserContext(user.getId(), user.getUsername(), user.getRole());
        request.setAttribute(UserContext.REQUEST_KEY, userContext);
        return true;
    }

    /**
     * 判断路径是否命中白名单。
     *
     * @param uri 请求路径
     * @return 命中返回 true
     */
    private boolean isPermitPath(String uri) {
        List<String> permitPaths = securityProperties.getPermitPaths();
        if (permitPaths == null || permitPaths.isEmpty() || uri == null) {
            return false;
        }
        for (String pattern : permitPaths) {
            if (pattern == null || pattern.isBlank()) {
                continue;
            }
            if (pathMatcher.match(pattern, uri)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 从 Authorization 头提取裸 token。
     *
     * @param header Authorization 头值
     * @return 裸 token，非法格式返回 null
     */
    private String extractToken(String header) {
        if (header == null || header.isBlank()) {
            return null;
        }
        String prefix = securityProperties.getTokenPrefix() == null
                ? "Bearer " : securityProperties.getTokenPrefix();
        String value = header.trim();
        if (value.regionMatches(true, 0, prefix, 0, prefix.length())) {
            return value.substring(prefix.length()).trim();
        }
        return null;
    }
}

package com.aimeeting.interview.common.web;

import com.aimeeting.interview.auth.infrastructure.web.AuthInterceptor;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 配置：注册鉴权拦截器与 {@code @CurrentUser} 参数解析器。
 *
 * <p>拦截器作用于 {@code /**}，是否放行由 {@link AuthInterceptor} 依据
 * {@code ai-interview.security.permit-paths} 白名单判定（含 Swagger、健康检查、登录注册等）。
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    private final CurrentUserMethodArgumentResolver currentUserMethodArgumentResolver;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 仅拦截 API 路径，页面（SPA）与静态资源（/assets/**）不进鉴权拦截器，
        // 由前端路由守卫控制访问；后端 API 仍按 permit-paths 白名单放行。
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**");
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserMethodArgumentResolver);
    }
}

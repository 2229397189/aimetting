package com.aimeeting.interview.common.web;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * 跨域配置：允许本地前端（Vite dev server）直连后端调试。
 *
 * <p>允许的来源：{@code http://localhost:5173} / {@code http://127.0.0.1:5173} /
 * {@code http://localhost:8080} 等本地地址；允许携带凭证（cookie / Authorization）。
 */
@Configuration
public class CorsConfig {

    /** 允许的前端来源。 */
    private static final List<String> ALLOWED_ORIGINS = List.of(
            "http://localhost:5173",
            "http://127.0.0.1:5173",
            "http://localhost:5174",
            "http://127.0.0.1:5174",
            "http://localhost:3000",
            "http://localhost:8080");

    /**
     * 注册 CORS 过滤器，保证跨域预检（OPTIONS）在鉴权拦截器之前生效。
     *
     * @return CORS 过滤器
     */
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(ALLOWED_ORIGINS);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Client-Token",
                "X-Request-Id", "Accept", "Origin", "X-Requested-With"));
        config.setExposedHeaders(List.of("X-Request-Id", "Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}

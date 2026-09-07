package com.aimeeting.interview.config;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 安全相关配置（{@code ai-interview.security}）。
 *
 * <p>{@code permit-paths} 为无需鉴权即可访问的路径，支持 Ant 风格通配（{@code /v3/**}）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai-interview.security")
public class SecurityProperties {

    /** 白名单路径（Ant 匹配）。 */
    private List<String> permitPaths = new ArrayList<>();

    /** Bearer 前缀。 */
    private String tokenPrefix = "Bearer ";

    /** Authorization 请求头名。 */
    private String tokenHeader = "Authorization";
}

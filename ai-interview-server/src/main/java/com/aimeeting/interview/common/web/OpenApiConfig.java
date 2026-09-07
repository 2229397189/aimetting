package com.aimeeting.interview.common.web;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * springdoc-openapi 配置：生成 {@code /v3/api-docs} 与 {@code /swagger-ui.html}。
 *
 * <p>统一在文档层面声明 Bearer 鉴权入口，便于在 Swagger UI 中直接调试受保护接口。
 */
@Configuration
public class OpenApiConfig {

    /**
     * 构建 OpenAPI 元数据。
     *
     * @return OpenAPI 描述对象
     */
    @Bean
    public OpenAPI aiInterviewOpenApi() {
        Server localServer = new Server();
        localServer.setUrl("http://localhost:8080");
        localServer.setDescription("本地开发环境");

        return new OpenAPI()
                .info(new Info()
                        .title("AI 在线模拟面试平台 API")
                        .description("基于 Java 17 + Spring Boot 3.2.5 的 AI 模拟面试平台后端接口文档")
                        .version("v1.0.0")
                        .contact(new Contact().name("aimeeting").email("admin@aimeeting.local"))
                        .license(new License().name("MIT")))
                .servers(List.of(localServer));
    }
}

package com.aimeeting.interview.ops.api;

import com.aimeeting.interview.common.convention.result.Result;
import com.aimeeting.interview.common.convention.result.Results;
import com.aimeeting.interview.config.AiProperties;
import com.aimeeting.interview.ops.api.io.resp.HealthResp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康检查接口（公开）：{@code GET /api/health}。
 *
 * <p>通过执行 {@code SELECT 1} 判断数据库连通性；AI 相关字段在 M1 返回真实配置值
 * （provider / 是否 mock），M3 接入后可补充连通性探测。
 */
@Slf4j
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
@Tag(name = "运维", description = "健康检查")
public class HealthController {

    /** 应用版本（取自 pom，缺失时给出默认）。 */
    @Value("${ai-interview.version:1.0.0}")
    private String version;

    private final JdbcTemplate jdbcTemplate;

    private final AiProperties aiProperties;

    /**
     * 健康检查。
     *
     * @return 健康状态
     */
    @GetMapping
    @Operation(summary = "健康检查", description = "返回应用与数据库状态、AI provider 与 mock 标识")
    public Result<HealthResp> health() {
        String dbStatus = checkDatabase();
        HealthResp resp = HealthResp.builder()
                .status("UP")
                .db(dbStatus)
                .aiProvider(aiProperties == null ? "unknown" : aiProperties.getProvider())
                .mockMode(aiProperties != null && aiProperties.isMockMode())
                .version(version)
                .timestamp(LocalDateTime.now())
                .build();
        return Results.success(resp);
    }

    /**
     * 探测数据库连通性，异常时返回 DOWN（健康检查接口本身不抛业务异常）。
     *
     * @return UP / DOWN
     */
    private String checkDatabase() {
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return (result != null && result == 1) ? "UP" : "DOWN";
        } catch (Exception e) {
            log.error("[Health] 数据库连通性检查失败: {}", e.getMessage());
            return "DOWN";
        }
    }
}

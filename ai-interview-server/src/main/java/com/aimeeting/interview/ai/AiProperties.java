package com.aimeeting.interview.ai;

import com.aimeeting.interview.ai.model.AiBizType;
import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * AI 相关配置绑定（application.yml 的 {@code ai-interview.ai} 段）。
 *
 * <p>注意：M1 在 {@code com.aimeeting.interview.config} 下已有一个同名配置类
 * （被 {@code HealthController} 使用）。为避免两个 {@code AiProperties} 的
 * 默认 Bean 名 {@code aiProperties} 冲突，此处显式指定 Bean 名为
 * {@code aiCoreProperties}；按类型注入不受影响。
 */
@Data
@Configuration("aiCoreProperties")
@ConfigurationProperties(prefix = "ai-interview.ai")
public class AiProperties {

    private String provider = "deepseek";
    private String baseUrl = "https://api.deepseek.com";
    private String apiKey = "";
    private String model = "deepseek-chat";
    private String reportModel = "deepseek-chat";

    private int connectTimeoutSeconds = 10;
    private int writeTimeoutSeconds = 30;
    private int questionTimeoutSeconds = 60;
    private int evaluateTimeoutSeconds = 90;
    private int followUpTimeoutSeconds = 90;
    private int resumeTimeoutSeconds = 90;
    private int reportTimeoutSeconds = 120;

    private int maxRetries = 2;
    private int retryBaseDelayMillis = 500;

    private int bulkheadConcurrency = 20;
    private int singleflightWaitTimeoutSeconds = 120;
    private int circuitBreakerWindowSize = 20;
    private double circuitBreakerFailureRate = 0.5;
    private int circuitBreakerOpenSeconds = 30;

    private double temperature = 0.7;
    private int maxTokens = 2048;

    /** 取某业务阶段的超时（毫秒）。 */
    public long stageTimeoutMillis(AiBizType bizType) {
        return switch (bizType) {
            case QUESTION -> Duration.ofSeconds(questionTimeoutSeconds).toMillis();
            case EVALUATE -> Duration.ofSeconds(evaluateTimeoutSeconds).toMillis();
            case FOLLOW_UP -> Duration.ofSeconds(followUpTimeoutSeconds).toMillis();
            case RESUME -> Duration.ofSeconds(resumeTimeoutSeconds).toMillis();
            case REPORT -> Duration.ofSeconds(reportTimeoutSeconds).toMillis();
        };
    }

    /** 取业务使用的模型（报告用 reportModel）。 */
    public String modelFor(AiBizType bizType) {
        return bizType == AiBizType.REPORT ? reportModel : model;
    }

    /** 是否 mock 模式（离线演示，不调用真实大模型）。 */
    public boolean isMock() {
        return "mock".equalsIgnoreCase(provider);
    }
}

package com.aimeeting.interview.config;

import java.time.Duration;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 能力相关配置（{@code ai-interview.ai}）。
 *
 * <p>M1 阶段仅落配置类（含默认值），真实调用由 M3 的
 * {@code AiProviderFactory} / {@code AiGuardService} 消费。
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai-interview.ai")
public class AiProperties {

    /** Provider 名称：deepseek / openai / qwen / mock。 */
    private String provider = "deepseek";

    /** OpenAI 协议兼容的 base url。 */
    private String baseUrl = "https://api.deepseek.com";

    /** API Key，为空时自动降级为 mock provider。 */
    private String apiKey = "";

    /** 通用对话模型。 */
    private String model = "deepseek-v4-flash";

    /** 报告生成专用模型（质量优先）。 */
    private String reportModel = "deepseek-chat";

    /** 连接超时（秒）。 */
    private int connectTimeoutSeconds = 10;

    /** 写超时（秒）。 */
    private int writeTimeoutSeconds = 30;

    /** 出题超时（秒）。 */
    private int questionTimeoutSeconds = 60;

    /** 评分超时（秒）。 */
    private int evaluateTimeoutSeconds = 90;

    /** 追问生成超时（秒）。 */
    private int followUpTimeoutSeconds = 90;

    /** 简历解析超时（秒）。 */
    private int resumeTimeoutSeconds = 90;

    /** 报告生成超时（秒）。 */
    private int reportTimeoutSeconds = 120;

    /** 最大重试次数（BR-09）。 */
    private int maxRetries = 2;

    /** 重试基础退避时间（毫秒），指数退避 500 -&gt; 1500。 */
    private long retryBaseDelayMillis = 500L;

    /** 舱壁并发上限（BR-11）。 */
    private int bulkheadConcurrency = 20;

    /** Single-flight follower 最长等待时间（秒，BR-12）。 */
    private int singleflightWaitTimeoutSeconds = 120;

    /** 熔断滑动窗口大小（BR-10）。 */
    private int circuitBreakerWindowSize = 20;

    /** 熔断失败率阈值。 */
    private double circuitBreakerFailureRate = 0.5;

    /** 熔断打开持续时间（秒）。 */
    private long circuitBreakerOpenSeconds = 30L;

    /** 采样温度。 */
    private double temperature = 0.7;

    /** 单次最大输出 token。 */
    private int maxTokens = 2048;

    /**
     * 是否 mock 模式：provider 显式为 mock 或 apiKey 为空（BR-14）。
     *
     * @return mock 模式返回 true
     */
    public boolean isMockMode() {
        return "mock".equalsIgnoreCase(provider) || apiKey == null || apiKey.isBlank();
    }

    /**
     * 熔断打开时长。
     *
     * @return Duration
     */
    public Duration circuitBreakerOpenDuration() {
        return Duration.ofSeconds(circuitBreakerOpenSeconds);
    }

    /**
     * Single-flight 等待超时。
     *
     * @return Duration
     */
    public Duration singleflightWaitTimeout() {
        return Duration.ofSeconds(singleflightWaitTimeoutSeconds);
    }
}

package com.aimeeting.interview.config;

import com.aimeeting.interview.ai.model.AiBizType;
import java.time.Duration;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 能力相关配置（{@code ai-interview.ai}）——全应用唯一绑定入口。
 *
 * <p>历史原因曾存在两份同名配置类（{@code com.aimeeting.interview.ai.AiProperties}
 * 与本类），两者字段高度重复、默认值不一致，且按类型注入时容易注入到错误实例，
 * 属于典型的配置漂移。现已统一收敛到本类：AI 层（guard/provider）与
 * 运维层（health/config）共用同一份配置实例。
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
     * 评分一致性采样双评比例（0~1，M2）。0 表示关闭（默认），1 表示全量双评。
     *
     * <p>命中采样时对同一答案追加一次静默副评，仅用于观测评分偏差，不影响主评分数与 SSE 事件序。
     */
    private double consistencySampleRate = 0.0;

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

    /**
     * 取某业务阶段的超时（毫秒）。
     *
     * @param bizType 业务类型
     * @return 该阶段的超时毫秒数
     */
    public long stageTimeoutMillis(AiBizType bizType) {
        return switch (bizType) {
            case QUESTION -> Duration.ofSeconds(questionTimeoutSeconds).toMillis();
            case EVALUATE -> Duration.ofSeconds(evaluateTimeoutSeconds).toMillis();
            case FOLLOW_UP -> Duration.ofSeconds(followUpTimeoutSeconds).toMillis();
            case RESUME -> Duration.ofSeconds(resumeTimeoutSeconds).toMillis();
            case REPORT -> Duration.ofSeconds(reportTimeoutSeconds).toMillis();
        };
    }

    /**
     * 取业务使用的模型：报告生成走 reportModel（质量优先），其余走通用 model。
     *
     * @param bizType 业务类型
     * @return 模型名
     */
    public String modelFor(AiBizType bizType) {
        return bizType == AiBizType.REPORT ? reportModel : model;
    }
}

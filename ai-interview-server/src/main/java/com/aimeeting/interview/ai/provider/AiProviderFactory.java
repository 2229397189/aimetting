package com.aimeeting.interview.ai.provider;

import com.aimeeting.interview.config.AiProperties;
import com.aimeeting.interview.ai.model.AiBizType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 供应商工厂：根据配置返回真实或 Mock 供应商。
 *
 * <p>规则：{@code provider=mock} 或 apiKey 为空 -> MockAiProvider；否则 OpenAiCompatProvider。
 * 真实供应商就绪时 {@code isMockMode()} 返回 false。
 */
@Slf4j
@Configuration
public class AiProviderFactory {

    private final AiProperties props;
    private final ObjectMapper objectMapper;

    public AiProviderFactory(AiProperties props, ObjectMapper objectMapper) {
        this.props = props;
        this.objectMapper = objectMapper;
    }

    @Bean
    public AiProvider aiProvider() {
        boolean useMock = "mock".equalsIgnoreCase(props.getProvider())
                || props.getApiKey() == null || props.getApiKey().isBlank();
        if (useMock) {
            log.warn("[AiProviderFactory] 使用 Mock 供应商（provider={} / apiKey 缺失），AI 结果由规则引擎生成",
                    props.getProvider());
            return new MockAiProvider();
        }
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(props.getConnectTimeoutSeconds(), TimeUnit.SECONDS)
                .writeTimeout(props.getWriteTimeoutSeconds(), TimeUnit.SECONDS)
                .readTimeout(props.getReportTimeoutSeconds(), TimeUnit.SECONDS)
                .build();
        log.info("[AiProviderFactory] 使用真实供应商 deepseek，baseUrl={}", props.getBaseUrl());
        return new OpenAiCompatProvider(props.getBaseUrl(), props.getApiKey(), client,
                objectMapper, props.getModel());
    }

    public boolean isMockMode() {
        return "mock".equalsIgnoreCase(props.getProvider())
                || props.getApiKey() == null || props.getApiKey().isBlank();
    }

    public String currentModel(AiBizType type) {
        return props.modelFor(type);
    }
}

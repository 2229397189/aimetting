package com.aimeeting.interview.ai.model;

import java.util.UUID;

/**
 * AI 请求封装：系统提示 + 用户提示 + 采样参数 + 是否 JSON 模式。
 */
public class AiRequest {

    private final AiBizType bizType;
    private final String systemPrompt;
    private final String userPrompt;
    private final Double temperature;
    private final Integer maxTokens;
    private final String model;
    private final boolean jsonMode;
    private final String requestId;

    private AiRequest(Builder b) {
        this.bizType = b.bizType;
        this.systemPrompt = b.systemPrompt;
        this.userPrompt = b.userPrompt;
        this.temperature = b.temperature;
        this.maxTokens = b.maxTokens;
        this.model = b.model;
        this.jsonMode = b.jsonMode;
        this.requestId = b.requestId != null ? b.requestId : UUID.randomUUID().toString().replace("-", "");
    }

    public static Builder builder() {
        return new Builder();
    }

    public AiBizType getBizType() {
        return bizType;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public String getUserPrompt() {
        return userPrompt;
    }

    public Double getTemperature() {
        return temperature;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public String getModel() {
        return model;
    }

    public boolean isJsonMode() {
        return jsonMode;
    }

    public String getRequestId() {
        return requestId;
    }

    public static final class Builder {
        private AiBizType bizType;
        private String systemPrompt;
        private String userPrompt;
        private Double temperature;
        private Integer maxTokens;
        private String model;
        private boolean jsonMode;
        private String requestId;

        public Builder bizType(AiBizType v) {
            this.bizType = v;
            return this;
        }

        public Builder systemPrompt(String v) {
            this.systemPrompt = v;
            return this;
        }

        public Builder userPrompt(String v) {
            this.userPrompt = v;
            return this;
        }

        public Builder temperature(Double v) {
            this.temperature = v;
            return this;
        }

        public Builder maxTokens(Integer v) {
            this.maxTokens = v;
            return this;
        }

        public Builder model(String v) {
            this.model = v;
            return this;
        }

        public Builder jsonMode(boolean v) {
            this.jsonMode = v;
            return this;
        }

        public Builder requestId(String v) {
            this.requestId = v;
            return this;
        }

        public AiRequest build() {
            return new AiRequest(this);
        }
    }
}

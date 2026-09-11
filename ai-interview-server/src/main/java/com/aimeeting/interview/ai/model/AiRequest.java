package com.aimeeting.interview.ai.model;

import com.aimeeting.interview.ai.agent.AgentId;
import java.util.UUID;

/**
 * AI 请求封装：系统提示 + 用户提示 + 采样参数 + 是否 JSON 模式 + 业务 Agent 标识。
 */
public class AiRequest {

    private final AiBizType bizType;
    private final AgentId agent;
    private final String systemPrompt;
    private final String userPrompt;
    private final Double temperature;
    private final Integer maxTokens;
    private final String model;
    private final boolean jsonMode;
    private final String requestId;

    private AiRequest(Builder b) {
        this.bizType = b.bizType;
        this.agent = b.agent;
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

    /**
     * 业务 Agent 标识（M3，可空）。
     *
     * <p>为空时由 {@code AiGuardService} 按 {@link AiBizType} 兜底推导，保证日志落库的 agent_id 不留空。
     *
     * @return Agent 标识，未显式声明时为 null
     */
    public AgentId getAgent() {
        return agent;
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
        private AgentId agent;
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

        /**
         * 显式声明业务 Agent（M3 可观测）。
         *
         * @param v Agent 标识
         * @return builder
         */
        public Builder agent(AgentId v) {
            this.agent = v;
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

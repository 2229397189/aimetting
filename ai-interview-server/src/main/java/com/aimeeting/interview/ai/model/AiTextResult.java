package com.aimeeting.interview.ai.model;

/**
 * AI 阻塞调用返回结果。
 */
public class AiTextResult {

    private final String content;
    private final Integer promptTokens;
    private final Integer completionTokens;
    private final long costMs;
    private final String model;

    public AiTextResult(String content, Integer promptTokens, Integer completionTokens, long costMs, String model) {
        this.content = content;
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.costMs = costMs;
        this.model = model;
    }

    public String getContent() {
        return content;
    }

    public Integer getPromptTokens() {
        return promptTokens;
    }

    public Integer getCompletionTokens() {
        return completionTokens;
    }

    public long getCostMs() {
        return costMs;
    }

    public String getModel() {
        return model;
    }
}

package com.aimeeting.interview.question.domain;

/**
 * 题目来源枚举（与前端 QuestionSource 对应）。
 */
public enum QuestionSource {
    AI("AI生成"),
    BANK("精选题库"),
    SEED("内置题库"),
    ADMIN("管理员录入");

    private final String label;

    QuestionSource(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}

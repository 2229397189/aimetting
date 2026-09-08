package com.aimeeting.interview.interview.domain.model;

/**
 * 评分来源（与前端 {@code EvaluatedBy}、{@code t_session_answer.evaluated_by} 对应）。
 */
public enum EvaluatedBy {

    /** AI 评分。 */
    AI,

    /** 规则引擎降级评分。 */
    RULE;

    /**
     * 解析来源名，非法值回退为 AI。
     *
     * @param name 来源名
     * @return 枚举
     */
    public static EvaluatedBy of(String name) {
        if (name == null || name.isBlank()) {
            return AI;
        }
        for (EvaluatedBy value : values()) {
            if (value.name().equalsIgnoreCase(name.trim())) {
                return value;
            }
        }
        return AI;
    }
}

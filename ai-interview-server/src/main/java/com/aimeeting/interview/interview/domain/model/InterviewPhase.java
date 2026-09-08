package com.aimeeting.interview.interview.domain.model;

/**
 * Δ1 增量：三阶段面试流程（与前端 {@code InterviewPhase} 一致）。
 */
public enum InterviewPhase {

    /** 技术面。 */
    TECHNICAL("技术面"),

    /** 项目面。 */
    PROJECT("项目面"),

    /** 行为面。 */
    BEHAVIORAL("行为面");

    private final String label;

    InterviewPhase(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /**
     * 解析阶段名，非法值回退为 TECHNICAL。
     *
     * @param name 阶段名
     * @return 阶段枚举
     */
    public static InterviewPhase of(String name) {
        if (name == null || name.isBlank()) {
            return TECHNICAL;
        }
        for (InterviewPhase phase : values()) {
            if (phase.name().equalsIgnoreCase(name.trim())) {
                return phase;
            }
        }
        return TECHNICAL;
    }
}

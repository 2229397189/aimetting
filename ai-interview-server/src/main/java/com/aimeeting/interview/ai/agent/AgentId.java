package com.aimeeting.interview.ai.agent;

import com.aimeeting.interview.ai.model.AiBizType;

/**
 * 业务 Agent 标识（{@code MULTI_AGENT_DESIGN.md} §3）。
 *
 * <p>每个 Agent 对应一类「角色化」的模型调用职责，用于 prompt 组装路由、成本/时延按 Agent 维度观测
 * （{@code t_ai_call_log.agent_id}）。
 *
 * <p>注意：本枚举只描述业务角色，**不含** LLM Router / Orchestrator——路由由确定性的
 * {@code InterviewWorkflowEngine} + 状态机承担。
 */
public enum AgentId {

    /** 出题 Agent：生成面试题（考察要点、难度、阶段）。 */
    INTERVIEWER,

    /** 评分 Agent：对答案打分、给点评、判定是否追问。 */
    EVALUATOR,

    /** 追问 Agent：生成追问问题。 */
    FOLLOW_UP,

    /** 简历分析 Agent：简历解析与画像。 */
    RESUME_ANALYST,

    /** 报告 Agent：五维评分、总结与行动建议。 */
    REPORTER;

    /**
     * 由既有 {@link AiBizType} 推导 Agent（M3 可观测性兜底）。
     *
     * <p>历史调用点未显式声明 Agent 时可据此归类，保证 {@code t_ai_call_log.agent_id} 不留空。
     *
     * @param bizType 业务类型（可空）
     * @return 对应 Agent；无法归类时返回 {@code null}
     */
    public static AgentId ofBizType(AiBizType bizType) {
        if (bizType == null) {
            return null;
        }
        return switch (bizType) {
            case QUESTION -> INTERVIEWER;
            case EVALUATE -> EVALUATOR;
            case FOLLOW_UP -> FOLLOW_UP;
            case RESUME -> RESUME_ANALYST;
            case REPORT -> REPORTER;
        };
    }
}

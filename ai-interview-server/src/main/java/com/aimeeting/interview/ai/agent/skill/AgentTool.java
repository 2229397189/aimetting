package com.aimeeting.interview.ai.agent.skill;

import com.aimeeting.interview.ai.agent.AgentContext;

/**
 * Agent 工具（{@code MULTI_AGENT_DESIGN.md} §3.1）。
 *
 * <p>设计取舍：**不依赖模型原生 function-calling**（项目统一纯 JSON 模式，原生工具调用会破坏
 * {@code response_format=json_object} 契约）。因此「工具」被建模为**运行前执行的 Java 方法**，
 * 结果以文本块形式注入 user prompt——JSON 模式安全、零中间件、可单测。
 */
public interface AgentTool {

    /**
     * 工具唯一标识（用于日志/可观测，不发给模型）。
     *
     * @return 工具 ID，如 {@code resume_match}
     */
    String toolId();

    /**
     * 工具说明（仅用于日志/可观测，不发给模型）。
     *
     * @return 说明文本
     */
    String description();

    /**
     * 执行工具，返回要注入 prompt 的格式化文本。
     *
     * <p>约定：失败或输入不足时返回 {@code null}（**不阻断主流程**），调用方跳过该片段即可。
     *
     * @param ctx Agent 运行上下文
     * @return 注入 prompt 的文本；无可用结果时返回 null
     */
    String execute(AgentContext ctx);
}

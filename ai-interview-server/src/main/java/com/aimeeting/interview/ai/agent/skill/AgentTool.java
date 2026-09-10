package com.aimeeting.interview.ai.agent.skill;

import com.aimeeting.interview.ai.agent.AgentContext;

/**
 * Agent 工具：在推理前基于上下文产出一段「工具上下文」文本，注入到提示中（可选能力）。
 */
public interface AgentTool {

    /** 工具唯一标识。 */
    String toolId();

    /** 工具用途描述（人类可读，便于调试）。 */
    String description();

    /** 基于上下文执行，返回追加进提示的文本（可空，空串会被注册表忽略）。 */
    String execute(AgentContext ctx);
}

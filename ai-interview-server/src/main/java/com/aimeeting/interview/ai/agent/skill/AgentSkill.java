package com.aimeeting.interview.ai.agent.skill;

import com.aimeeting.interview.ai.agent.AgentId;
import java.util.List;

/**
 * Agent 技能（{@code MULTI_AGENT_DESIGN.md} §3.1）。
 *
 * <p>一个 Skill = 一段可复用的角色指令片段（拼进 system prompt）+ 可选的一组 {@link AgentTool}
 * （运行前执行、结果注入 user prompt 上下文块）。
 */
public interface AgentSkill {

    /**
     * 技能唯一标识。
     *
     * @return 技能 ID，如 {@code action_plan}
     */
    String skillId();

    /**
     * 拼进 system prompt 的角色指令片段（纯文本）。
     *
     * @return 指令片段
     */
    String systemFragment();

    /**
     * 该技能需要的工具，Agent 运行前依次执行，结果注入 user prompt 上下文块。
     *
     * @return 工具列表，默认空
     */
    default List<AgentTool> tools() {
        return List.of();
    }

    /**
     * 声明本技能适用于哪个 Agent（注册表据此归类）。
     *
     * @param agent 目标 Agent
     * @return 适用返回 true
     */
    boolean supports(AgentId agent);
}

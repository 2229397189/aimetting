package com.aimeeting.interview.ai.agent.skill;

import com.aimeeting.interview.ai.agent.AgentContext;
import com.aimeeting.interview.ai.agent.AgentId;
import java.util.List;

/**
 * Agent 技能：可插拔地往某个 Agent 的系统提示中追加「能力片段」，可选地提供工具上下文。
 *
 * <p>设计参考 {@code InterviewSessionStateMachine}——纯静态友好、无外部依赖，仅由
 * {@link SkillRegistry} 收集并按 {@link AgentId} 组织。</p>
 */
public interface AgentSkill {

    /** 技能唯一标识。 */
    String skillId();

    /** 追加进系统提示的能力片段（可空，空串会被注册表忽略）。 */
    String systemFragment();

    /** 该技能提供的工具（默认无）。 */
    default List<AgentTool> tools() {
        return List.of();
    }

    /** 该技能是否适用于给定 Agent。 */
    boolean supports(AgentId agent);
}

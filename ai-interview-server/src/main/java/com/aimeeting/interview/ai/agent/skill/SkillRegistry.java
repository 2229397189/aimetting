package com.aimeeting.interview.ai.agent.skill;

import com.aimeeting.interview.ai.agent.AgentContext;
import com.aimeeting.interview.ai.agent.AgentId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Agent 技能注册表：收集 Spring 注入的全部 {@link AgentSkill}，按 {@link AgentId} 分组。
 *
 * <p>提供两个纯逻辑方法：
 * <ul>
 *   <li>{@link #assembleSystem(AgentId)}：拼接该 Agent 所有技能的系统片段（以 "\n" 连接）。</li>
 *   <li>{@link #assembleToolContext(AgentId, AgentContext)}：拼接该 Agent 所有工具上下文（以 "\n" 连接）。</li>
 * </ul>
 * 设计参考 {@code InterviewSessionStateMachine}——无外部依赖，纯静态友好。
 */
@Component
public class SkillRegistry {

    private final Map<AgentId, List<AgentSkill>> byAgent = new EnumMap<>(AgentId.class);

    /** Spring 注入全部技能；无技能时传 {@code null} 或空列表均安全。 */
    public SkillRegistry(List<AgentSkill> skills) {
        if (skills == null) {
            return;
        }
        for (AgentSkill skill : skills) {
            if (skill == null) {
                continue;
            }
            for (AgentId id : AgentId.values()) {
                if (skill.supports(id)) {
                    byAgent.computeIfAbsent(id, k -> new ArrayList<>()).add(skill);
                }
            }
        }
    }

    /** 该 Agent 生效的技能列表（不可变视角，无则返回空列表）。 */
    public List<AgentSkill> skillsOf(AgentId id) {
        return byAgent.getOrDefault(id, List.of());
    }

    /** 拼接该 Agent 所有技能的系统片段。 */
    public String assembleSystem(AgentId id) {
        StringBuilder sb = new StringBuilder();
        for (AgentSkill skill : skillsOf(id)) {
            String fragment = skill.systemFragment();
            if (fragment != null && !fragment.isBlank()) {
                if (sb.length() > 0) {
                    sb.append("\n");
                }
                sb.append(fragment);
            }
        }
        return sb.toString();
    }

    /** 拼接该 Agent 所有工具的执行结果（工具上下文）。 */
    public String assembleToolContext(AgentId id, AgentContext ctx) {
        StringBuilder sb = new StringBuilder();
        for (AgentSkill skill : skillsOf(id)) {
            for (AgentTool tool : skill.tools()) {
                if (tool == null) {
                    continue;
                }
                String result = tool.execute(ctx);
                if (result != null && !result.isBlank()) {
                    if (sb.length() > 0) {
                        sb.append("\n");
                    }
                    sb.append(result);
                }
            }
        }
        return sb.toString();
    }
}

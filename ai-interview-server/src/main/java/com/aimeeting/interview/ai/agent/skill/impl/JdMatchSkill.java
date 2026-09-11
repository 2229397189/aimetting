package com.aimeeting.interview.ai.agent.skill.impl;

import com.aimeeting.interview.ai.agent.AgentId;
import com.aimeeting.interview.ai.agent.skill.AgentSkill;
import com.aimeeting.interview.ai.agent.skill.AgentTool;
import com.aimeeting.interview.ai.agent.tool.impl.ResumeMatchTool;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 岗位匹配技能（ReporterAgent，{@code MULTI_AGENT_DESIGN.md} §4.5、§9 M3「JdMatchSkill（按需）」）。
 *
 * <p>把会话上的目标岗位 JD 与候选人简历摘要做确定性匹配，命中/缺口片段由
 * {@link ResumeMatchTool} 生成并注入 user prompt，使报告的改进建议与行动建议对齐岗位要求。
 */
@Component
@RequiredArgsConstructor
public class JdMatchSkill implements AgentSkill {

    private final ResumeMatchTool resumeMatchTool;

    @Override
    public String skillId() {
        return "jd_match";
    }

    @Override
    public String systemFragment() {
        return "若上下文提供了【岗位匹配片段】，请让 improvements 与 actions 优先对齐其中的「简历缺口」关键词，"
                + "并明确候选人需要在哪些岗位上要求的技能上补强；若未提供该片段，则按面试表现本身给出建议。";
    }

    /**
     * 本技能声明的工具：岗位匹配（JD × 简历）。
     *
     * @return 工具列表
     */
    @Override
    public List<AgentTool> tools() {
        return List.of(resumeMatchTool);
    }

    @Override
    public boolean supports(AgentId agent) {
        return agent == AgentId.REPORTER;
    }
}

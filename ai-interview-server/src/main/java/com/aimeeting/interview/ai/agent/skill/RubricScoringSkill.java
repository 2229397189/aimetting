package com.aimeeting.interview.ai.agent.skill;

import com.aimeeting.interview.ai.agent.AgentId;
import org.springframework.stereotype.Component;

/**
 * 评分锚点技能：为 {@code EVALUATOR} 追加「先定位锚段再给分」的评分锚点片段。
 */
@Component
public class RubricScoringSkill implements AgentSkill {

    @Override
    public String skillId() {
        return "rubric";
    }

    @Override
    public String systemFragment() {
        return "评分锚点(先定位锚段再给分): 80-100=正确且能结合项目踩坑/对比方案/边界; "
                + "60-79=正确但偏书面无落地; 40-59=要点覆盖一半; 20-39=明显错误; 0-19=未作答。";
    }

    @Override
    public boolean supports(AgentId agent) {
        return agent == AgentId.EVALUATOR;
    }
}

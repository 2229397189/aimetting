package com.aimeeting.interview.ai.agent.skill;

import com.aimeeting.interview.ai.agent.AgentId;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * 纯逻辑单测：SkillRegistry 的技能分组与系统片段拼接（不启动 Spring）。
 */
class SkillRegistryTest {

    @Test
    void skillsOf_evaluator_containsRubricSkill() {
        SkillRegistry registry = new SkillRegistry(java.util.List.of(new RubricScoringSkill()));
        boolean hasRubric = registry.skillsOf(AgentId.EVALUATOR).stream()
                .anyMatch(s -> "rubric".equals(s.skillId()));
        assertTrue(hasRubric, "EVALUATOR 应挂载 rubric 技能");
    }

    @Test
    void assembleSystem_evaluator_containsAnchor() {
        SkillRegistry registry = new SkillRegistry(java.util.List.of(new RubricScoringSkill()));
        String system = registry.assembleSystem(AgentId.EVALUATOR);
        assertTrue(system.contains("80-100"), "系统片段应包含评分锚点 80-100");
    }

    @Test
    void skillsOf_followUp_isEmpty() {
        SkillRegistry registry = new SkillRegistry(java.util.List.of(new RubricScoringSkill()));
        assertTrue(registry.skillsOf(AgentId.FOLLOW_UP).isEmpty(), "FOLLOW_UP 不应挂载 rubric 技能");
    }

    @Test
    void emptyRegistry_safe() {
        SkillRegistry registry = new SkillRegistry(java.util.List.of());
        assertFalse(registry.assembleSystem(AgentId.EVALUATOR).contains("80-100"));
    }
}

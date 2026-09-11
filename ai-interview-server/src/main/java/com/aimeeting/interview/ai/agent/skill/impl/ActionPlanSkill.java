package com.aimeeting.interview.ai.agent.skill.impl;

import com.aimeeting.interview.ai.agent.AgentId;
import com.aimeeting.interview.ai.agent.skill.AgentSkill;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * 报告行动建议技能（ReporterAgent / {@code MULTI_AGENT_DESIGN.md} §4.5、§9 M3）。
 *
 * <p>M3 细化点：把「后续行动」从泛泛的学习建议，细化为**可执行项**——每条行动必须包含
 * 「动作 + 具体做法 + 时间窗 + 验收标准」，并优先覆盖五维中得分最低的维度与岗位 JD 缺口。
 * 同时提供确定性的 {@link #refine(List)} 做结构清洗（去空白 / 去重 / 限长），
 * **不做语义删除**，避免把 AI 报告误判为不合格而降级为规则报告。
 */
@Component
public class ActionPlanSkill implements AgentSkill {

    /** 行动建议条数上限（与报告校验阈值 actions ≥ 3 兼容）。 */
    public static final int MAX_ACTIONS = 5;

    /** 单条行动建议最大长度（与 t_interview_report.actions 列宽裕度匹配）。 */
    private static final int MAX_ACTION_LENGTH = 120;

    @Override
    public String skillId() {
        return "action_plan";
    }

    @Override
    public String systemFragment() {
        return "后续行动建议(actions)必须细化：给出 3-5 条可执行项，每条按「动作 + 具体做法 + 时间窗 + 验收标准」组织"
                + "（例：2 周内用 XX 技术重写 XX 模块，并以单测覆盖率 ≥70% 验收）；"
                + "优先针对五维中得分最低的维度与岗位 JD 缺口给出行动；"
                + "禁止出现「多练习」「加强学习」「继续努力」这类无动作、无验收标准的空话。";
    }

    @Override
    public boolean supports(AgentId agent) {
        return agent == AgentId.REPORTER;
    }

    /**
     * 结构清洗：去首尾空白 / 丢弃空白项 / 去重（保持原序）/ 截断单条长度 / 限制条数。
     *
     * <p>只做结构处理，不按语义剔除条目——报告链路对 {@code actions.size() < 3} 会整体降级为规则报告，
     * 语义过滤存在把合格报告打成降级的风险。
     *
     * @param actions 原始行动建议
     * @return 清洗后的行动建议（可能为空列表，永不为 null）
     */
    public List<String> refine(List<String> actions) {
        if (actions == null || actions.isEmpty()) {
            return new ArrayList<>();
        }
        Set<String> seen = new LinkedHashSet<>();
        for (String action : actions) {
            if (action == null) {
                continue;
            }
            String text = action.trim().replaceAll("\\s+", " ");
            if (text.isEmpty()) {
                continue;
            }
            if (text.length() > MAX_ACTION_LENGTH) {
                text = text.substring(0, MAX_ACTION_LENGTH);
            }
            seen.add(text);
            if (seen.size() >= MAX_ACTIONS) {
                break;
            }
        }
        return new ArrayList<>(seen);
    }
}

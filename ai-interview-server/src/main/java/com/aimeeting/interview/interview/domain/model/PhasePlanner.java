package com.aimeeting.interview.interview.domain.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 三阶段题量规划器（Δ1）。
 *
 * <p>与前端 {@code utils/dict.ts#splitPhase / calcPhase} 完全一致：
 * 默认 50% / 30% / 20%，每阶段至少 1 题，总数守恒。
 */
public final class PhasePlanner {

    private PhasePlanner() {
    }

    /**
     * 按总题量拆分三阶段题量。
     *
     * @param totalQuestion 总题量
     * @return 各阶段题量（TECHNICAL / PROJECT / BEHAVIORAL）
     */
    public static Map<InterviewPhase, Integer> splitPhase(int totalQuestion) {
        int total = Math.max(1, totalQuestion);
        int technical = (int) Math.round(total * 0.5);
        int project = (int) Math.round(total * 0.3);
        int behavioral = total - technical - project;
        if (technical < 1) {
            technical = 1;
        }
        if (project < 1) {
            project = 1;
        }
        behavioral = total - technical - project;
        if (behavioral < 1) {
            behavioral = 1;
            if (technical >= project) {
                technical = total - project - behavioral;
            } else {
                project = total - technical - behavioral;
            }
        }
        int sum = technical + project + behavioral;
        if (sum != total) {
            technical += total - sum;
        }
        Map<InterviewPhase, Integer> plan = new LinkedHashMap<>();
        plan.put(InterviewPhase.TECHNICAL, Math.max(1, technical));
        plan.put(InterviewPhase.PROJECT, Math.max(1, project));
        plan.put(InterviewPhase.BEHAVIORAL, Math.max(1, behavioral));
        return plan;
    }

    /**
     * 计算题号所属阶段。
     *
     * @param questionNo    题号（1-based）
     * @param totalQuestion 总题量
     * @return 所属阶段
     */
    public static InterviewPhase phaseOf(int questionNo, int totalQuestion) {
        Map<InterviewPhase, Integer> plan = splitPhase(totalQuestion);
        int no = Math.max(1, questionNo);
        if (no <= plan.get(InterviewPhase.TECHNICAL)) {
            return InterviewPhase.TECHNICAL;
        }
        if (no <= plan.get(InterviewPhase.TECHNICAL) + plan.get(InterviewPhase.PROJECT)) {
            return InterviewPhase.PROJECT;
        }
        return InterviewPhase.BEHAVIORAL;
    }

    /**
     * 按总题量拆分三阶段题量，并以阶段名（字符串）为键返回（用于 JSON 持久化）。
     *
     * @param totalQuestion 总题量
     * @return 各阶段题量（TECHNICAL / PROJECT / BEHAVIORAL 字符串键）
     */
    public static Map<String, Integer> planToStringMap(int totalQuestion) {
        Map<InterviewPhase, Integer> plan = splitPhase(totalQuestion);
        Map<String, Integer> out = new LinkedHashMap<>();
        plan.forEach((k, v) -> out.put(k.name(), v));
        return out;
    }
}

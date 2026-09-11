package com.aimeeting.interview.ai.agent;

import java.util.List;
import java.util.function.Supplier;
import lombok.Builder;
import lombok.Getter;

/**
 * Agent 运行上下文：跨 Agent 共享、由编排器（{@code InterviewWorkflowEngine}）注入的不可变上下文。
 *
 * <p>字段为各 Agent 需要的最小可用并集：
 * <ul>
 *   <li>{@code resumeDigest} / {@code jdText} / {@code history}：服务于出题 / 评分 / 工作流编排；</li>
 *   <li>{@code referencePoints} / {@code originalAnswer} / {@code followUpCount} / {@code maxFollowUp}：
 *       服务于追问 Agent。</li>
 * </ul>
 * 后续 Agent 按需扩展，不破坏既有字段。
 */
@Getter
@Builder
public class AgentContext {

    /** 用户 ID。 */
    private final Long userId;

    /** 会话 ID。 */
    private final Long sessionId;

    /** 面试阶段：TECHNICAL / PROJECT / BEHAVIORAL（可空）。 */
    private final String phase;

    /** 候选人简历摘要（ResumeAnalystAgent 产物，可为 null）。 */
    private final String resumeDigest;

    /** 题目的考察要点（追问 Agent 使用，可空）。 */
    private final List<String> referencePoints;

    /** 候选人原始回答（追问 Agent 使用，可空）。 */
    private final String originalAnswer;

    /** 当前已追问次数（追问 Agent 使用）。 */
    private final int followUpCount;

    /** 单题最大追问次数（追问 Agent 使用）。 */
    private final int maxFollowUp;

    /**
     * 目标岗位 JD（懒加载，避免每次拼装 prompt 都查库）。
     *
     * <p>注意：{@link Supplier} 可能返回 null 或空白串，调用方需自行判空。
     */
    private final Supplier<String> jdText;

    /** 本题已发生的 题/答/评 轮次摘要（供 FollowUpAgent 消费，可为空）。 */
    private final List<String> history;

    /**
     * 取 JD 文本（懒加载 + 异常兜底，失败视为无 JD）。
     *
     * @return JD 文本；无则返回 null
     */
    public String jdTextOrNull() {
        if (jdText == null) {
            return null;
        }
        try {
            String text = jdText.get();
            return text == null || text.isBlank() ? null : text;
        } catch (Exception e) {
            return null;
        }
    }
}

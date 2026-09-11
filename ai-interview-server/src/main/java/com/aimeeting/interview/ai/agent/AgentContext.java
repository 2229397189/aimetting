package com.aimeeting.interview.ai.agent;

import java.util.List;
import java.util.function.Supplier;
import lombok.Builder;
import lombok.Getter;

/**
 * Agent 运行上下文：跨 Agent 共享、由编排器（{@code InterviewWorkflowEngine}）注入。
 *
 * <p>字段对齐 {@code MULTI_AGENT_DESIGN.md} §3.2 的最小可用子集：M3 阶段先以
 * {@code resumeDigest}（简历摘要文本）、{@code jdText}（懒加载 JD）、{@code history}
 * （题/答/评轮次摘要）承载上下文；待 M1 落地强类型 {@code ResumeProfile} / {@code ChatTurn}
 * 后替换为强类型字段，本类的 builder 契约保持不变。
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

package com.aimeeting.interview.ai.agent;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * Agent 运行上下文：一次推理所需的共享元信息（不可变，仅 getter）。
 *
 * <p>当前仅包含 {@code FollowUpAgent} 所需的字段；后续 Agent 按需扩展，不破坏既有字段。</p>
 */
@Getter
@Builder
public class AgentContext {

    private Long userId;

    private Long sessionId;

    private String phase;

    private String resumeDigest;

    private List<String> referencePoints;

    private String originalAnswer;

    private int followUpCount;

    private int maxFollowUp;
}

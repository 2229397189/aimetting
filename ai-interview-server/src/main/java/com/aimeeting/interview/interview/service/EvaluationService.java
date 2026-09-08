package com.aimeeting.interview.interview.service;

import com.aimeeting.interview.interview.service.model.EvaluationContext;
import com.aimeeting.interview.interview.service.model.EvaluationResult;

/**
 * 评分服务：经 AiGuardService 流式评分；AI 失败或返回非法时降级规则引擎。
 */
public interface EvaluationService {

    /**
     * 流式评分。
     *
     * @param userId 用户 ID
     * @param ctx    评分上下文
     * @param sink   点评增量下沉（推给 SSE）
     * @return 最终结构化结果
     */
    EvaluationResult evaluate(Long userId, EvaluationContext ctx, AiStreamSink sink);
}

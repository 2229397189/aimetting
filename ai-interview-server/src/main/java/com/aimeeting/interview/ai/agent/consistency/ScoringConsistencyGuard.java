package com.aimeeting.interview.ai.agent.consistency;

import com.aimeeting.interview.ai.guard.AiGuardService;
import com.aimeeting.interview.config.AiProperties;
import com.aimeeting.interview.interview.service.AiStreamSink;
import com.aimeeting.interview.interview.service.EvaluationService;
import com.aimeeting.interview.interview.service.model.EvaluationContext;
import com.aimeeting.interview.interview.service.model.EvaluationResult;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 评分一致性守卫（{@code MULTI_AGENT_DESIGN.md} §4.6、§7.2、§9 M2「可选」）。
 *
 * <p>策略：按 {@code ai-interview.ai.consistency-sample-rate}（默认 {@code 0.0}，即**关闭**）对答案
 * 采样做**双评**：主评走流式（保证 SSE 点评实时下沉，用户体验不变），副评走非流式（静默，仅用于一致性观测）。
 * 两次评分偏差超过阈值时记 WARN 日志，用于持续观测评分稳定性；**不改分、不改流程、不影响 SSE 事件序**。
 *
 * <p>采样率为 0 时本组件与直接调用 {@link EvaluationService} 完全等价（仅多一次随机数比较）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScoringConsistencyGuard {

    /** 双评分差绝对值超过该阈值记为不一致（0-100 分制）。 */
    private static final int DIVERGENCE_THRESHOLD = 20;

    private final EvaluationService evaluationService;

    private final AiGuardService aiGuardService;

    private final AiProperties aiProperties;

    /**
     * 评分入口：始终返回主评结果；命中采样时额外做一次静默副评并记录偏差。
     *
     * @param userId  用户 ID（用于额度归集与可观测）
     * @param context 评分上下文
     * @param sink    流式点评下沉器（主评使用）
     * @return 主评结果
     */
    public EvaluationResult evaluate(Long userId, EvaluationContext context, AiStreamSink sink) {
        EvaluationResult primary = evaluationService.evaluate(userId, context, sink);
        if (!shouldSample()) {
            return primary;
        }
        try {
            EvaluationResult secondary = evaluationService.evaluate(userId, context, new AiStreamSink() {
                @Override
                public void acceptDelta(String delta) {
                    // 副评静默：丢弃所有增量，避免污染主评的 SSE 点评流
                }

                @Override
                public void acceptFinish() {
                    // 副评静默：不产生任何外部副作用
                }
            });
            logDivergence(userId, context, primary, secondary);
        } catch (Throwable t) {
            // 副评属观测行为，任何异常都不得影响主流程
            log.debug("[Consistency] 采样副评失败，忽略: {}", t.getMessage());
        }
        return primary;
    }

    private boolean shouldSample() {
        double rate = aiProperties.getConsistencySampleRate();
        if (rate <= 0) {
            return false;
        }
        if (rate >= 1) {
            return true;
        }
        return ThreadLocalRandom.current().nextDouble() < rate;
    }

    private void logDivergence(Long userId, EvaluationContext context,
                               EvaluationResult primary, EvaluationResult secondary) {
        Integer a = primary.getScore();
        Integer b = secondary.getScore();
        if (a == null || b == null) {
            log.warn("[Consistency] 采样双评存在空分, sessionId={}, questionNo={}, primary={}, secondary={}",
                    context.getSessionId(), context.getQuestionNo(), a, b);
            return;
        }
        int gap = Math.abs(a - b);
        if (gap > DIVERGENCE_THRESHOLD) {
            log.warn("[Consistency] 采样双评偏差过大, userId={}, sessionId={}, questionNo={}, "
                            + "primary={}, secondary={}, gap={}",
                    userId, context.getSessionId(), context.getQuestionNo(), a, b, gap);
        } else {
            log.info("[Consistency] 采样双评一致, sessionId={}, questionNo={}, primary={}, secondary={}, gap={}",
                    context.getSessionId(), context.getQuestionNo(), a, b, gap);
        }
    }

    /** 供编排器/调用方读取的观测字段占位（采样率、阈值），避免魔法值散落。 */
    public double sampleRate() {
        return aiProperties.getConsistencySampleRate();
    }
}

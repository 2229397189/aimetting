package com.aimeeting.interview.interview.service.impl;

import com.aimeeting.interview.ai.AiProperties;
import com.aimeeting.interview.ai.fallback.RuleEvaluator;
import com.aimeeting.interview.ai.guard.AiGuardService;
import com.aimeeting.interview.ai.model.AiBizType;
import com.aimeeting.interview.ai.model.AiRequest;
import com.aimeeting.interview.ai.model.AiStage;
import com.aimeeting.interview.ai.model.AiStreamListener;
import com.aimeeting.interview.ai.model.AiTextResult;
import com.aimeeting.interview.ai.parser.AiJsonParser;
import com.aimeeting.interview.ai.parser.AiOutputValidator;
import com.aimeeting.interview.common.util.Md5Util;
import com.aimeeting.interview.interview.domain.model.EvaluatedBy;
import com.aimeeting.interview.interview.prompt.InterviewPrompts;
import com.aimeeting.interview.interview.service.AiStreamSink;
import com.aimeeting.interview.interview.service.EvaluationService;
import com.aimeeting.interview.interview.service.model.EvaluationContext;
import com.aimeeting.interview.interview.service.model.EvaluationResult;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 评分服务实现：AI 流式评分 + 规则引擎降级（BR-13）。
 *
 * <p>降级触发条件：AI 调用失败（超时 / 熔断 / 舱壁 / 网络）或返回内容非法（非 JSON / 分数越界）。
 * 降级时 {@code evaluatedBy=RULE}、{@code degraded=true}，HTTP 仍为 200。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EvaluationServiceImpl implements EvaluationService {

    /** 改进答案的触发分数（Δ1：低于 80 分给出改进参考答案）。 */
    private static final int IMPROVE_THRESHOLD = 80;

    private final AiGuardService aiGuardService;

    private final AiProperties aiProperties;

    @Override
    public EvaluationResult evaluate(Long userId, EvaluationContext ctx, AiStreamSink sink) {
        AiRequest request = AiRequest.builder()
                .bizType(AiBizType.EVALUATE)
                .systemPrompt(InterviewPrompts.evaluateSystem())
                .userPrompt(InterviewPrompts.evaluateUser(ctx.getQuestionTitle(), ctx.getReferencePoints(),
                        ctx.getAnswer(), ctx.isFollowUp()))
                .temperature(aiProperties.getTemperature())
                .maxTokens(aiProperties.getMaxTokens())
                .model(aiProperties.modelFor(AiBizType.EVALUATE))
                .jsonMode(false)
                .build();

        String singleFlightKey = "eva:" + ctx.getSessionId() + ":" + ctx.getSessionQuestionId() + ":"
                + Md5Util.md5Safe(ctx.getAnswer()) + "@" + Md5Util.md5Safe(ctx.getQuestionTitle())
                + (ctx.isFollowUp() ? ":fu" : "");

        StringBuilder streamedBody = new StringBuilder();
        AtomicReference<AiTextResult> resultRef = new AtomicReference<>();
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        long waitMillis = aiProperties.stageTimeoutMillis(AiBizType.EVALUATE) + 30_000L;

        try {
            aiGuardService.executeStream(AiStage.EVALUATION, singleFlightKey, userId, request,
                    new AiStreamListener() {
                        @Override
                        public void onDelta(String delta) {
                            if (delta == null || delta.isEmpty()) {
                                return;
                            }
                            streamedBody.append(delta);
                            if (sink != null) {
                                sink.acceptDelta(delta);
                            }
                        }

                        @Override
                        public void onComplete(AiTextResult result) {
                            resultRef.set(result);
                            latch.countDown();
                        }

                        @Override
                        public void onError(Throwable t) {
                            errorRef.set(t);
                            latch.countDown();
                        }
                    });
            latch.await(waitMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.warn("[Evaluation] 评分调用异常: {}", e.getMessage());
        }

        AiTextResult result = resultRef.get();
        if (result != null) {
            try {
                EvaluationResult parsed = fromAi(result.getContent(), streamedBody.toString(), ctx);
                finish(sink);
                return parsed;
            } catch (Exception e) {
                log.warn("[Evaluation] AI 返回内容非法，转规则降级, sessionId={}, msg={}",
                        ctx.getSessionId(), e.getMessage());
            }
        } else {
            log.warn("[Evaluation] AI 评分失败，转规则降级, sessionId={}, err={}",
                    ctx.getSessionId(), errorRef.get() == null ? "timeout" : errorRef.get().getMessage());
        }
        EvaluationResult degraded = fallback(ctx, sink, streamedBody.toString());
        finish(sink);
        return degraded;
    }

    /**
     * 解析 AI 输出的结构化 JSON 段。
     */
    private EvaluationResult fromAi(String json, String streamedBody, EvaluationContext ctx) {
        JsonNode node = AiJsonParser.parse(json);
        int score = AiOutputValidator.scoreInRange(node, "score");
        List<String> highlights = toStringArray(node, "highlights");
        List<String> gaps = toStringArray(node, "gaps");
        boolean needFollowUp = node.path("needFollowUp").asBoolean(false);
        String followUpQuestion = node.path("followUpQuestion").asText("");
        String improvedAnswer = node.path("improvedAnswer").asText("");
        if (improvedAnswer.isBlank() && score < IMPROVE_THRESHOLD) {
            improvedAnswer = buildImprovedAnswer(ctx);
        }
        String comment = streamedBody == null || streamedBody.isBlank()
                ? "AI 评分完成，综合得分 " + score + " 分。" : streamedBody;

        return EvaluationResult.builder()
                .score(score)
                .comment(comment)
                .highlights(highlights)
                .gaps(gaps)
                .needFollowUp(needFollowUp && followUpQuestion != null && !followUpQuestion.isBlank())
                .followUpQuestion(followUpQuestion)
                .improvedAnswer(improvedAnswer)
                .evaluatedBy(EvaluatedBy.AI)
                .degraded(false)
                .build();
    }

    /**
     * 规则引擎降级评分。
     */
    private EvaluationResult fallback(EvaluationContext ctx, AiStreamSink sink, String streamedBody) {
        RuleEvaluator.RuleScore ruleScore = RuleEvaluator.fallbackEvaluate(ctx.getReferencePoints(), ctx.getAnswer());
        if (streamedBody == null || streamedBody.isBlank()) {
            if (sink != null) {
                sink.acceptDelta(ruleScore.comment);
            }
        }
        String improvedAnswer = ruleScore.score < IMPROVE_THRESHOLD ? buildImprovedAnswer(ctx) : "";
        return EvaluationResult.builder()
                .score(ruleScore.score)
                .comment(streamedBody == null || streamedBody.isBlank() ? ruleScore.comment : streamedBody)
                .highlights(ruleScore.highlights)
                .gaps(ruleScore.gaps)
                .needFollowUp(ruleScore.needFollowUp
                        && ruleScore.followUpQuestion != null && !ruleScore.followUpQuestion.isBlank())
                .followUpQuestion(ruleScore.followUpQuestion)
                .improvedAnswer(improvedAnswer)
                .evaluatedBy(EvaluatedBy.RULE)
                .degraded(true)
                .build();
    }

    private static void finish(AiStreamSink sink) {
        if (sink != null) {
            sink.acceptFinish();
        }
    }

    private static List<String> toStringArray(JsonNode node, String field) {
        List<String> out = new ArrayList<>();
        JsonNode array = node.path(field);
        if (array.isArray()) {
            array.forEach(item -> {
                String text = item.asText("");
                if (!text.isBlank()) {
                    out.add(text);
                }
            });
        }
        return out;
    }

    /**
     * 基于考察要点生成改进参考答案（Δ1）。
     */
    private static String buildImprovedAnswer(EvaluationContext ctx) {
        List<String> points = ctx.getReferencePoints();
        StringBuilder sb = new StringBuilder("建议按以下结构重新组织回答：\n");
        if (points == null || points.isEmpty()) {
            sb.append("1. 先给出核心定义；2. 说明适用场景；3. 结合一个真实项目例子说明落地方式；4. 总结注意事项。");
            return sb.toString();
        }
        int index = 1;
        for (String point : points) {
            sb.append(index++).append(". ").append(point).append("；\n");
        }
        sb.append(index).append(". 结合一次实际项目经历说明如何落地与踩过的坑。");
        return sb.toString();
    }
}

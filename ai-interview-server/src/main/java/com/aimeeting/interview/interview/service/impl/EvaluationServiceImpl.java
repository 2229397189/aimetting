package com.aimeeting.interview.interview.service.impl;

import com.aimeeting.interview.ai.agent.AgentContext;
import com.aimeeting.interview.ai.agent.AgentId;
import com.aimeeting.interview.ai.agent.InterviewAgent;
import com.aimeeting.interview.ai.agent.skill.SkillRegistry;
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
import com.aimeeting.interview.config.AiProperties;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 评分服务实现：AI 流式评分 + 规则引擎降级（BR-13）。
 *
 * <p>AI 评分使用纯 JSON 模式（{@code response_format=json_object}）：模型只返回结构化 JSON，
 * 其中 {@code comment} 字段即面向用户的点评正文。增量阶段只聚合 JSON 片段，不在前端实时透传
 * 原始 JSON；评分完成时统一把 {@code comment} 作为点评下发，保证前端展示的是可读文本而非 JSON。</p>
 *
 * <p>降级触发条件：AI 调用失败（超时 / 熔断 / 舱壁 / 网络）或返回内容非法（非 JSON / 分数越界）。
 * 降级时 {@code evaluatedBy=RULE}、{@code degraded=true}，HTTP 仍为 200。
 */
@Slf4j
@Service
public class EvaluationServiceImpl implements EvaluationService, InterviewAgent<EvaluationContext, EvaluationResult> {

    /** 改进答案的触发分数（低于 80 分给出改进参考答案）。 */
    private static final int IMPROVE_THRESHOLD = 80;

    private final AiGuardService aiGuardService;

    private final AiProperties aiProperties;

    /** 可选：为 Evaluator 追加评分锚点等技能片段；无技能注册时为 null。 */
    private final SkillRegistry skillRegistry;

    @Autowired(required = false)
    public EvaluationServiceImpl(AiGuardService aiGuardService, AiProperties aiProperties, SkillRegistry skillRegistry) {
        this.aiGuardService = aiGuardService;
        this.aiProperties = aiProperties;
        this.skillRegistry = skillRegistry;
    }

    @Override
    public AgentId id() {
        return AgentId.EVALUATOR;
    }

    @Override
    public AiBizType bizType() {
        return AiBizType.EVALUATE;
    }

    @Override
    public AiStage stage() {
        return AiStage.EVALUATION;
    }

    /**
     * Agent 契约入口：从 {@link AgentContext} 取 userId，委托既有 {@link #evaluate} 逻辑（无递归）。
     */
    @Override
    public EvaluationResult run(AgentContext ctx, EvaluationContext input) {
        return evaluate(ctx == null ? null : ctx.getUserId(), input, null);
    }

    @Override
    public EvaluationResult evaluate(Long userId, EvaluationContext ctx, AiStreamSink sink) {
        final String resumeDigest = ctx.getResumeDigest();
        AiRequest request = AiRequest.builder()
                .bizType(AiBizType.EVALUATE)
                .systemPrompt(InterviewPrompts.evaluateSystem()
                        + (skillRegistry != null ? "\n" + skillRegistry.assembleSystem(AgentId.EVALUATOR) : ""))
                .userPrompt(InterviewPrompts.evaluateUser(ctx.getQuestionTitle(), ctx.getReferencePoints(),
                        ctx.getAnswer(), ctx.isFollowUp(), resumeDigest))
                .temperature(aiProperties.temperatureFor(AiBizType.EVALUATE))
                .maxTokens(aiProperties.getMaxTokens())
                .model(aiProperties.modelFor(AiBizType.EVALUATE))
                .jsonMode(true)
                .build();

        String singleFlightKey = "eva:" + ctx.getSessionId() + ":" + ctx.getSessionQuestionId() + ":"
                + Md5Util.md5Safe(ctx.getAnswer()) + "@" + Md5Util.md5Safe(ctx.getQuestionTitle())
                + (ctx.isFollowUp() ? ":fu" : "");

        long waitMillis = aiProperties.stageTimeoutMillis(AiBizType.EVALUATE) + 30_000L;

        // 业务级保险：仅当「AI 确实返回了内容、但解析不出合法 JSON」时外层重试一次再降级，
        // 避免偶发噪声导致评分静默降级（与 resume 解析同源风险）。
        // 传输层失败（网络 / 402 / 超时 / 熔断）不再外层重试：AiGuardService 内部已按退避策略重试过，
        // 外层再重试只会放大 token 成本，且对 402 这类永久性错误无效。
        EvaluationResult parsed = null;
        for (int attempt = 1; attempt <= 2 && parsed == null; attempt++) {
            String sfKey = attempt == 1 ? singleFlightKey : singleFlightKey + ":retry";
            AtomicReference<AiTextResult> resultRef = new AtomicReference<>();
            AtomicReference<Throwable> errorRef = new AtomicReference<>();
            CountDownLatch latch = new CountDownLatch(1);
            try {
                aiGuardService.executeStream(AiStage.EVALUATION, sfKey, userId, request,
                        new AiStreamListener() {
                            @Override
                            public void onDelta(String delta) {
                                // 纯 JSON 模式：增量仅为 JSON 片段，不实时透传为点评，结束后统一下发 comment
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
                log.warn("[Evaluation] 评分调用异常(第 {} 次): {}", attempt, e.getMessage());
            }
            AiTextResult result = resultRef.get();
            if (result == null) {
                log.warn("[Evaluation] AI 评分调用失败（传输层，内部已重试，不再外层重试）, sessionId={}, err={}",
                        ctx.getSessionId(), errorRef.get() == null ? "timeout" : errorRef.get().getMessage());
                break;
            }
            try {
                parsed = fromAi(result.getContent(), ctx);
            } catch (Exception e) {
                log.warn("[Evaluation] AI 返回内容非法(第 {} 次), sessionId={}, msg={}",
                        attempt, ctx.getSessionId(), e.getMessage());
            }
        }

        if (parsed != null) {
            // 统一下发 AI 点评正文（纯 JSON 中的 comment 字段）
            if (sink != null && parsed.getComment() != null && !parsed.getComment().isBlank()) {
                sink.acceptDelta(parsed.getComment());
            }
            finish(sink);
            return parsed;
        }
        EvaluationResult degraded = fallback(ctx, sink);
        finish(sink);
        return degraded;
    }

    /**
     * 解析 AI 输出的结构化 JSON：纯 JSON 模式，comment 为点评正文，score 等字段同前。
     */
    private EvaluationResult fromAi(String json, EvaluationContext ctx) {
        JsonNode node = AiJsonParser.parse(json);
        int score = AiOutputValidator.scoreInRange(node, "score");
        List<String> highlights = toStringArray(node, "highlights");
        List<String> gaps = toStringArray(node, "gaps");
        boolean needFollowUp = node.path("needFollowUp").asBoolean(false);
        // followUpQuestion 字段仍解析，但生成已解耦到 FollowUpAgent，此处不再填充（保持 schema 不变）
        String followUpQuestion = node.path("followUpQuestion").asText("");
        String improvedAnswer = node.path("improvedAnswer").asText("");
        if (improvedAnswer.isBlank() && score < IMPROVE_THRESHOLD) {
            improvedAnswer = buildImprovedAnswer(ctx);
        }
        String comment = node.path("comment").asText("");
        if (comment == null || comment.isBlank()) {
            comment = "AI 评分完成，综合得分 " + score + " 分。";
        }

        // authenticity 为可选字段：仅当回答涉及简历项目/实习时由 AI 给出，缺失则置 null
        Integer authenticity = null;
        JsonNode authNode = node.get("authenticity");
        if (authNode != null && authNode.isNumber()) {
            int a = authNode.asInt();
            authenticity = a < 0 ? 0 : Math.min(a, 100);
        }

        return EvaluationResult.builder()
                .score(score)
                .comment(comment)
                .highlights(highlights)
                .gaps(gaps)
                .needFollowUp(needFollowUp)
                .followUpQuestion(null)
                .improvedAnswer(improvedAnswer)
                .authenticity(authenticity)
                .evaluatedBy(EvaluatedBy.AI)
                .degraded(false)
                .build();
    }

    /**
     * 规则引擎降级评分。
     */
    private EvaluationResult fallback(EvaluationContext ctx, AiStreamSink sink) {
        RuleEvaluator.RuleScore ruleScore = RuleEvaluator.fallbackEvaluate(ctx.getReferencePoints(), ctx.getAnswer());
        if (sink != null) {
            sink.acceptDelta(ruleScore.comment);
        }
        String improvedAnswer = ruleScore.score < IMPROVE_THRESHOLD ? buildImprovedAnswer(ctx) : "";
        return EvaluationResult.builder()
                .score(ruleScore.score)
                .comment(ruleScore.comment)
                .highlights(ruleScore.highlights)
                .gaps(ruleScore.gaps)
                .needFollowUp(ruleScore.needFollowUp)
                .followUpQuestion(null)
                .improvedAnswer(improvedAnswer)
                .authenticity(RuleEvaluator.fallbackAuthenticity(ctx.getAnswer(), ctx.getResumeDigest()))
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

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
import com.aimeeting.interview.ai.model.AiTextResult;
import com.aimeeting.interview.ai.parser.AiJsonParser;
import com.aimeeting.interview.common.util.Md5Util;
import com.aimeeting.interview.config.AiProperties;
import com.aimeeting.interview.interview.service.model.FollowUpQuestion;
import com.aimeeting.interview.interview.service.model.FollowUpReq;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 追问生成 Agent：把「是否追问」的判断（Evaluator）与「追问问题文本」的生成解耦。
 *
 * <p>设计要点：
 * <ul>
 *   <li>当已追问次数 {@code >=} 上限时直接返回 {@code probeType=STOP}（LimitGuard），不发请求；</li>
 *   <li>否则走 {@link AiGuardService#execute} 阻塞式调用（追问问题短，无需流式）；</li>
 *   <li>任何异常一律降级为规则引擎产出，保证 HTTP 200 不抛错。</li>
 * </ul>
 */
@Slf4j
@Service
public class FollowUpAgentServiceImpl implements InterviewAgent<FollowUpReq, FollowUpQuestion> {

    private final AiGuardService aiGuardService;

    private final AiProperties aiProperties;

    /** 可选：无技能注册时为 null，调用处做空守卫。 */
    private final SkillRegistry skillRegistry;

    @Autowired(required = false)
    public FollowUpAgentServiceImpl(AiGuardService aiGuardService, AiProperties aiProperties,
                                    SkillRegistry skillRegistry) {
        this.aiGuardService = aiGuardService;
        this.aiProperties = aiProperties;
        this.skillRegistry = skillRegistry;
    }

    @Override
    public AgentId id() {
        return AgentId.FOLLOW_UP;
    }

    @Override
    public AiBizType bizType() {
        return AiBizType.FOLLOW_UP;
    }

    @Override
    public AiStage stage() {
        return AiStage.FOLLOW_UP_GEN;
    }

    @Override
    public FollowUpQuestion run(AgentContext ctx, FollowUpReq req) {
        int count = req.getFollowUpCount();
        int max = req.getMaxFollowUp();
        if (count >= max) {
            // LimitGuard：已达上限，不再发请求
            return new FollowUpQuestion("", decideProbeType(count, max), count);
        }
        String sfKey = "fu:" + req.getSessionId() + ":" + req.getSessionQuestionId() + ":"
                + Md5Util.md5Safe(req.getOriginalAnswer()) + ":" + count;
        String systemPrompt = "你是面试追问教练，基于候选人原答案的薄弱点生成1个深探针问题，"
                + "或当已连续追问时改问另一子方向。"
                + (skillRegistry != null ? skillRegistry.assembleSystem(AgentId.FOLLOW_UP) : "");
        String userPrompt = "题面:" + nullToEmpty(req.getQuestionTitle())
                + "\n考察要点:" + (req.getReferencePoints() == null ? "" : String.join("、", req.getReferencePoints()))
                + "\n原答案:" + nullToEmpty(req.getOriginalAnswer())
                + "\n已追问次数:" + count + "/" + max
                + "\n只输出JSON:{question,probeType}";
        AiRequest request = AiRequest.builder()
                .bizType(AiBizType.FOLLOW_UP)
                .systemPrompt(systemPrompt)
                .userPrompt(userPrompt)
                .temperature(aiProperties.temperatureFor(AiBizType.FOLLOW_UP))
                .maxTokens(aiProperties.getMaxTokens())
                .model(aiProperties.modelFor(AiBizType.FOLLOW_UP))
                .jsonMode(true)
                .build();
        try {
            return aiGuardService.execute(AiStage.FOLLOW_UP_GEN, sfKey, req.getUserId(), request, parser(count));
        } catch (Exception e) {
            log.warn("[FollowUpAgent] 追问生成失败，降级为规则引擎, sessionId={}, err={}",
                    req.getSessionId(), e.getMessage());
            String fallbackQuestion = RuleEvaluator.fallbackEvaluate(req.getReferencePoints(),
                    req.getOriginalAnswer()).followUpQuestion;
            return new FollowUpQuestion(fallbackQuestion, "DEEPEN", count);
        }
    }

    private static Function<AiTextResult, FollowUpQuestion> parser(int count) {
        return result -> {
            JsonNode node = AiJsonParser.parse(result.getContent());
            String question = node.path("question").asText("");
            String probeType = node.path("probeType").asText("DEEPEN");
            return new FollowUpQuestion(question, probeType, count);
        };
    }

    /** 探针类型判定（供单测验证的 LimitGuard 逻辑）。 */
    static String decideProbeType(int count, int max) {
        return count >= max ? "STOP" : "DEEPEN";
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}

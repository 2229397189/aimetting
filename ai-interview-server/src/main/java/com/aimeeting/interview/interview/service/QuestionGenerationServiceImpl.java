package com.aimeeting.interview.interview.service;

import com.aimeeting.interview.ai.AiProperties;
import com.aimeeting.interview.ai.fallback.RuleEvaluator;
import com.aimeeting.interview.ai.guard.AiGuardService;
import com.aimeeting.interview.ai.model.AiBizType;
import com.aimeeting.interview.ai.model.AiErrorType;
import com.aimeeting.interview.ai.model.AiRequest;
import com.aimeeting.interview.ai.model.AiStage;
import com.aimeeting.interview.ai.model.AiTextResult;
import com.aimeeting.interview.ai.parser.AiJsonParser;
import com.aimeeting.interview.ai.provider.AiProviderFactory;
import com.aimeeting.interview.common.convention.errorcode.BaseErrorCode;
import com.aimeeting.interview.common.convention.exception.RemoteException;
import com.aimeeting.interview.common.util.JsonUtil;
import com.aimeeting.interview.common.util.Md5Util;
import com.aimeeting.interview.interview.service.model.GeneratedQuestion;
import com.aimeeting.interview.question.dao.entity.QuestionDO;
import com.aimeeting.interview.question.dao.mapper.QuestionMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 出题服务：优先 AI（单飞 + 会话内题目去重），失败降级题库随机抽题。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionGenerationServiceImpl implements QuestionGenerationService {

    private static final String SYS_PROMPT = "你是一道技术面试出题助手。请只输出严格 JSON，不要包含额外说明或 Markdown 围栏。"
            + "字段：title(题目), referencePoints(考察要点字符串数组), analysis(参考答案要点)。";

    private final AiGuardService aiGuard;
    private final AiProviderFactory providerFactory;
    private final AiProperties aiProperties;
    private final QuestionMapper questionMapper;

    @Override
    public GeneratedQuestion generate(Long userId, Long sessionId, String direction, String difficulty,
                                      int questionNo, String resumeDigest, Set<String> excludeTitleMd5) {
        Set<String> exclude = excludeTitleMd5 == null ? new HashSet<>() : excludeTitleMd5;
        String sfKey = "q:" + sessionId + ":" + questionNo + ":" + direction + ":" + difficulty;
        try {
            String userPrompt = buildUserPrompt(direction, difficulty, questionNo, resumeDigest);
            AiRequest req = AiRequest.builder()
                    .bizType(AiBizType.QUESTION)
                    .systemPrompt(SYS_PROMPT)
                    .userPrompt(userPrompt)
                    .model(providerFactory.currentModel(AiBizType.QUESTION))
                    .temperature(aiProperties.getTemperature())
                    .maxTokens(aiProperties.getMaxTokens())
                    .jsonMode(true)
                    .build();
            return aiGuard.execute(AiStage.QUESTION_GEN, sfKey, userId, req,
                    result -> parseGenerated(result.getContent(), difficulty));
        } catch (Throwable t) {
            log.warn("[QGen] AI 出题失败，降级题库抽题: {}", t.getMessage());
            return fallback(direction, difficulty, exclude);
        }
    }

    private GeneratedQuestion parseGenerated(String content, String difficulty) {
        JsonNode node = AiJsonParser.parse(content);
        String title = node.path("title").asText(null);
        if (title == null || title.isBlank()) {
            throw new RemoteException("AI 返回题目缺少 title", BaseErrorCode.REMOTE_ERROR, AiErrorType.INVALID_RESPONSE);
        }
        List<String> referencePoints = parseStringArray(node.get("referencePoints"));
        String analysis = node.path("analysis").asText("");
        return new GeneratedQuestion(title, referencePoints, analysis, difficulty, "AI", false);
    }

    private GeneratedQuestion fallback(String direction, String difficulty, Set<String> exclude) {
        List<QuestionDO> bank = questionMapper.selectRandom(direction, difficulty, null, 5);
        for (QuestionDO q : bank) {
            if (!exclude.contains(Md5Util.md5(q.getTitle()))) {
                return new GeneratedQuestion(q.getTitle(),
                        JsonUtil.parse(q.getReferencePoints(), new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}),
                        q.getAnalysis(), difficulty, "BANK", true);
            }
        }
        return new GeneratedQuestion(RuleEvaluator.fallbackQuestionTitle(direction, difficulty),
                RuleEvaluator.fallbackReferencePoints(), "", difficulty, "BANK", true);
    }

    private String buildUserPrompt(String direction, String difficulty, int questionNo, String resumeDigest) {
        StringBuilder sb = new StringBuilder();
        sb.append("方向：").append(direction).append("；难度：").append(difficulty)
                .append("；题号：").append(questionNo).append("。\n");
        if (resumeDigest != null && !resumeDigest.isBlank()) {
            sb.append("候选人背景：").append(resumeDigest).append("\n");
        }
        sb.append("请出一道考察该方向核心知识的题目，并给出 3-5 个考察要点与参考答案要点。");
        return sb.toString();
    }

    private List<String> parseStringArray(JsonNode node) {
        List<String> list = new ArrayList<>();
        if (node != null && node.isArray()) {
            node.forEach(n -> list.add(n.asText()));
        }
        return list;
    }

    /** 屏蔽未使用警告（保留便于扩展）。 */
    private static AiTextResult unused(AiTextResult r) {
        return r;
    }
}

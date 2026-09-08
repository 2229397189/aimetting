package com.aimeeting.interview.interview.service;

import com.aimeeting.interview.interview.domain.model.InterviewPhase;
import com.aimeeting.interview.interview.service.model.GeneratedQuestion;
import com.aimeeting.interview.question.domain.Difficulty;
import com.aimeeting.interview.question.domain.Direction;
import java.util.Set;

/**
 * 出题服务：优先 AI 生成（single-flight + 会话内去重），失败降级题库随机抽题。
 */
public interface QuestionGenerationService {

    /**
     * 生成一道题。
     *
     * @param userId           用户 ID（AI 日志）
     * @param sessionId        会话 ID（单飞键）
     * @param direction        方向
     * @param difficulty       难度
     * @param questionNo       题号
     * @param resumeDigest     简历摘要（可空）
     * @param excludeTitleMd5  会话内已有题干的 MD5（去重）
     * @return 生成结果
     */
    GeneratedQuestion generate(Long userId, Long sessionId, String direction, String difficulty,
                               int questionNo, String resumeDigest, Set<String> excludeTitleMd5);

    /**
     * 生成一道题（带阶段与 JD 上下文）。
     *
     * @param userId          用户 ID
     * @param sessionId       会话 ID
     * @param direction       方向
     * @param difficulty      难度
     * @param questionNo      题号
     * @param resumeDigest    简历摘要（可空）
     * @param excludeTitleMd5 已有题干 MD5
     * @param phase           阶段（可空）
     * @param jdText          目标岗位 JD（可空）
     * @param excludeTitles   已有题干原文（用于 prompt 去重提示）
     * @return 生成结果
     */
    default GeneratedQuestion generate(Long userId, Long sessionId, Direction direction, Difficulty difficulty,
                               int questionNo, String resumeDigest, Set<String> excludeTitleMd5,
                               InterviewPhase phase, String jdText, java.util.List<String> excludeTitles) {
        return generate(userId, sessionId, direction == null ? null : direction.name(),
                difficulty == null ? null : difficulty.name(), questionNo, resumeDigest, excludeTitleMd5);
    }
}

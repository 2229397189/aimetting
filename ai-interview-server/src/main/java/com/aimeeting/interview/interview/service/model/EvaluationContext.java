package com.aimeeting.interview.interview.service.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 评分上下文。
 *
 * @param sessionId          会话 ID
 * @param sessionQuestionId  会话题目 ID
 * @param questionNo         题号
 * @param questionTitle      题干
 * @param referencePoints    考察要点
 * @param answer             用户答案
 * @param isFollowUp         是否为追问答案
 * @param phase              所属阶段（用于 prompt 措辞）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationContext {

    private Long sessionId;

    private Long sessionQuestionId;

    private Integer questionNo;

    private String questionTitle;

    private List<String> referencePoints;

    private String answer;

    private boolean isFollowUp;

    private String phase;
}

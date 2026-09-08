package com.aimeeting.interview.interview.service.model;

import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 答题幂等回放快照（BR-07）。
 *
 * <p>重复提交（同 {@code X-Client-Token}）时直接回放本对象，
 * 保证 LLM 调用次数不增加。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerReplay implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 答案 ID。 */
    private Long answerId;

    /** 会话题目 ID。 */
    private Long sessionQuestionId;

    /** 题号。 */
    private Integer questionNo;

    /** 得分。 */
    private Integer score;

    /** 点评全文。 */
    private String comment;

    /** 亮点。 */
    private List<String> highlights;

    /** 不足。 */
    private List<String> gaps;

    /** 改进后参考答案。 */
    private String improvedAnswer;

    /** 评分来源 AI | RULE。 */
    private String evaluatedBy;

    /** 是否降级。 */
    private boolean degraded;

    /** 下一步动作：NEXT_QUESTION | FOLLOW_UP | COMPLETED | WAIT。 */
    private String nextAction;

    /** 会话当前状态。 */
    private String status;

    /** 当前题号。 */
    private Integer currentIndex;

    /** 总题量。 */
    private Integer totalQuestion;

    /** 报告 ID（M6 接入后非空）。 */
    private Long reportId;

    /** 追问问题。 */
    private String followUpQuestion;

    /** 已追问次数。 */
    private Integer followUpCount;

    /** 追问上限。 */
    private Integer maxFollowUp;
}

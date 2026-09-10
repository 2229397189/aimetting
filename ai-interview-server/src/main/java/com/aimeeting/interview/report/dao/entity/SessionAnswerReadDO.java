package com.aimeeting.interview.report.dao.entity;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 答题记录只读投影（t_session_answer），供报告聚合使用，不写入。
 */
@Data
public class SessionAnswerReadDO {

    private Long id;

    private Long sessionId;

    private Long sessionQuestionId;

    private Long userId;

    private String content;

    private Integer isFollowUp;

    private Long parentAnswerId;

    private Integer score;

    private String comment;

    private String highlights;

    private String gaps;

    private String improvedAnswer;

    private String evaluatedBy;

    /** 简历经历真实性/参与度判断（0-100，可空）。 */
    private Integer authenticity;

    private Integer followUpCount;

    private Integer skipped;

    private LocalDateTime createTime;
}

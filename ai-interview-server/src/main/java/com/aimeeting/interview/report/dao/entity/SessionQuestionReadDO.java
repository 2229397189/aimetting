package com.aimeeting.interview.report.dao.entity;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 会话题目只读投影（t_session_question），供报告聚合使用，不写入。
 */
@Data
public class SessionQuestionReadDO {

    private Long id;

    private Long sessionId;

    private Integer questionNo;

    private Long questionId;

    private String title;

    private String referencePoints;

    private String source;

    private String difficulty;

    private String phase;

    private Integer skipped;

    private LocalDateTime createTime;
}

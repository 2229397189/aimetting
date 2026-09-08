package com.aimeeting.interview.report.dao.entity;

import lombok.Data;

/**
 * 会话元信息只读投影（t_interview_session），供报告聚合使用。
 */
@Data
public class SessionMetaReadDO {

    private Long id;

    private String sessionNo;

    private String directions;

    private String difficulty;

    private Long userId;
}

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

    /** 会话绑定的目标岗位 JD（M3 岗位匹配工具的数据来源，可空）。 */
    private String jdText;

    /** 会话绑定的简历 ID（M3 岗位匹配工具的数据来源，可空）。 */
    private Long resumeId;
}

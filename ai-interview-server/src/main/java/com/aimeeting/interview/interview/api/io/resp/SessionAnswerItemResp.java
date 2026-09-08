package com.aimeeting.interview.interview.api.io.resp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * 会话答案项（与前端 {@code SessionAnswerItem} 一致）。
 */
@Data
public class SessionAnswerItemResp {

    private Long answerId;

    private Long sessionQuestionId;

    private Integer questionNo;

    private String content;

    private Integer score;

    private String comment;

    private List<String> highlights = new ArrayList<>();

    private List<String> gaps = new ArrayList<>();

    /** Δ1：改进后的参考答案（得分 &lt; 80 时给出）。 */
    private String improvedAnswer;

    private Boolean isFollowUp;

    private Long parentAnswerId;

    private Integer followUpCount;

    private Boolean skipped;

    private String evaluatedBy;

    /** 是否来自规则降级。 */
    private Boolean degraded;

    private LocalDateTime createdAt;
}

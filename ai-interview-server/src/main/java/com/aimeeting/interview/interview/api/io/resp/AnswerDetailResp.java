package com.aimeeting.interview.interview.api.io.resp;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * 单条答题与评分详情（幂等回放校验用，与前端 answerDetail 一致）。
 */
@Data
public class AnswerDetailResp {

    private Long answerId;

    private Long sessionQuestionId;

    private String content;

    private Integer score;

    private String comment;

    private List<String> highlights = new ArrayList<>();

    private List<String> gaps = new ArrayList<>();

    private String improvedAnswer;

    private String evaluatedBy;

    /** 是否来自规则降级。 */
    private Boolean degraded;
}

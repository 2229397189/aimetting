package com.aimeeting.interview.interview.api.io.resp;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * 会话题目项（与前端 {@code SessionQuestionItem} 一致）。
 */
@Data
public class SessionQuestionItemResp {

    private Long sessionQuestionId;

    private Integer questionNo;

    private Long questionId;

    private String title;

    private List<String> referencePoints = new ArrayList<>();

    private String difficulty;

    private String source;

    /** Δ1：TECHNICAL | PROJECT | BEHAVIORAL。 */
    private String phase;

    /** 原答案。 */
    private SessionAnswerItemResp answer;

    /** 追问答案链。 */
    private List<SessionAnswerItemResp> followUps = new ArrayList<>();
}

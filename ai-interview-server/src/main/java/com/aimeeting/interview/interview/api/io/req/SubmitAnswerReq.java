package com.aimeeting.interview.interview.api.io.req;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 提交答案请求（SSE，与前端 {@code SubmitAnswerReq} 一致）。
 */
@Data
public class SubmitAnswerReq {

    /** 会话题目 ID。 */
    @NotNull(message = "sessionQuestionId 不能为空")
    private Long sessionQuestionId;

    /** 答案正文（10~5000 字，BR-03）。 */
    @NotNull(message = "答案内容不能为空")
    private String content;

    /** 幂等令牌（同 X-Client-Token）。 */
    private String clientToken;

    /** 追问答案对应的父答案 ID。 */
    private Long parentAnswerId;

    /** 是否为追问答案。 */
    private Boolean isFollowUp;
}

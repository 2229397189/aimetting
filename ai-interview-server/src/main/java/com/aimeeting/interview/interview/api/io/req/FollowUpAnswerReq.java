package com.aimeeting.interview.interview.api.io.req;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 提交追问答案请求（SSE）。
 */
@Data
public class FollowUpAnswerReq {

    /** 会话题目 ID。 */
    @NotNull(message = "sessionQuestionId 不能为空")
    private Long sessionQuestionId;

    /** 追问答案正文。 */
    @NotNull(message = "答案内容不能为空")
    private String content;

    /** 父答案 ID。 */
    private Long parentAnswerId;

    /** 幂等令牌。 */
    private String clientToken;
}

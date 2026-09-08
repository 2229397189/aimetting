package com.aimeeting.interview.interview.api.io.resp;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 会话对话流水项（与前端 messages 接口一致）。
 */
@Data
public class MessageItemResp {

    /** 题目或答案 ID。 */
    private Long id;

    /** INTERVIEWER（题目 / 追问）| CANDIDATE（答案）。 */
    private String role;

    private String content;

    private Integer questionNo;

    private Integer score;

    private LocalDateTime createdAt;
}

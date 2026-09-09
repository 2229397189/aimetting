package com.aimeeting.interview.interview.api.io.resp;

import java.time.LocalDateTime;
import java.util.List;
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

    /** 亮点（AI 评分细分，库表已落库，无则 null）。 */
    private List<String> highlights;

    /** 不足（AI 评分细分，库表已落库，无则 null）。 */
    private List<String> gaps;

    /** Δ1：改进后的参考答案（得分 &lt; 80 时给出，无则 null）。 */
    private String improvedAnswer;

    /** 追问问题（库表无独立字段，从 comment 的 ===JSON=== 部分解析，无则 null）。 */
    private String followUpQuestion;

    private LocalDateTime createdAt;
}

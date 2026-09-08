package com.aimeeting.interview.interview.service.sse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * follow_up 事件 payload（判定需要追问，与前端 {@code FollowUpPayload} 逐字一致）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FollowUpPayload {

    private Long answerId;

    private Long parentAnswerId;

    private Long sessionQuestionId;

    private String title;

    private Integer followUpCount;

    private Integer maxFollowUp;
}

package com.aimeeting.interview.interview.service.sse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * comment 事件 payload（与前端 {@code CommentPayload} 逐字一致）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentPayload {

    private Long answerId;

    private String delta;

    private Boolean finish;

    private String improvedAnswerDelta;
}

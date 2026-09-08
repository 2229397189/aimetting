package com.aimeeting.interview.interview.service.sse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * done 事件 payload（流正常结束，与前端 {@code DonePayload} 逐字一致）。
 *
 * <p>构造参数顺序：nextAction, status, currentIndex, totalQuestion, answerId, score, reportId, replayed。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DonePayload {

    private String nextAction;

    private String status;

    private Integer currentIndex;

    private Integer totalQuestion;

    private Long answerId;

    private Integer score;

    private Long reportId;

    private Boolean replayed;
}

package com.aimeeting.interview.interview.service.sse;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * score 事件 payload（评分解析完成，与前端 {@code ScorePayload} 逐字一致）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScorePayload {

    private Long answerId;

    private Integer score;

    private String evaluatedBy;

    private List<String> highlights;

    private List<String> gaps;

    /** Δ1 增量：改进后的参考答案。 */
    private String improvedAnswer;

    private Boolean degraded;
}

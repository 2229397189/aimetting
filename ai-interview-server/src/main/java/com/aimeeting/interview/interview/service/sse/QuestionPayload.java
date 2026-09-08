package com.aimeeting.interview.interview.service.sse;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * question 事件 payload（与前端 {@code QuestionPayload} 逐字一致）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionPayload {

    private Integer questionNo;

    private Long sessionQuestionId;

    private String title;

    private List<String> referencePoints;

    private String difficulty;

    private String source;

    private Integer totalQuestion;

    private String phase;
}

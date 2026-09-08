package com.aimeeting.interview.interview.service.model;

import com.aimeeting.interview.interview.domain.model.EvaluatedBy;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 评分结果（AI 或规则降级）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationResult {

    /** 0-100 整数。 */
    private Integer score;

    /** Markdown 正文点评。 */
    private String comment;

    private List<String> highlights;

    private List<String> gaps;

    /** 是否需要追问。 */
    private Boolean needFollowUp;

    /** 追问问题（needFollowUp 为 true 时非空）。 */
    private String followUpQuestion;

    /** AI | RULE。 */
    private EvaluatedBy evaluatedBy;

    /** 改进后的参考答案（得分 &lt; 80 时给出）。 */
    private String improvedAnswer;

    /** true 表示走了降级。 */
    private boolean degraded;
}

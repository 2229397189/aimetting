package com.aimeeting.interview.question.api.io.resp;

import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 题目详情返回（QuestionResp + 录入人 / 更新时间）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class QuestionDetailResp extends QuestionResp {
    private Long createdBy;
    private LocalDateTime updatedAt;
}

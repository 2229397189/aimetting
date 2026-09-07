package com.aimeeting.interview.question.api.io.resp;

import lombok.Data;

/**
 * 批量导入结果。
 */
@Data
public class QuestionImportResult {
    private Integer successCount;
    private Integer failCount;
}

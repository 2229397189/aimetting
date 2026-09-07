package com.aimeeting.interview.question.api.io.resp;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/**
 * 题目列表 / 详情返回（与前端 QuestionResp 对应）。
 */
@Data
public class QuestionResp {
    private Long id;
    private String direction;
    private String difficulty;
    private String title;
    private List<String> referencePoints;
    private List<String> tags;
    private String analysis;
    private String source;
    private Integer status;
    private LocalDateTime createdAt;
}

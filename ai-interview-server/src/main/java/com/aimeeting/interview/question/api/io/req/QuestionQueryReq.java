package com.aimeeting.interview.question.api.io.req;

import lombok.Data;

/**
 * 题目分页查询请求。
 */
@Data
public class QuestionQueryReq {
    private String direction;
    private String difficulty;
    private String keyword;
    private Integer pageNum = 1;
    private Integer pageSize = 10;
}

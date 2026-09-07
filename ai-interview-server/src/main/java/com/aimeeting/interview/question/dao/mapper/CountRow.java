package com.aimeeting.interview.question.dao.mapper;

import lombok.Data;

/**
 * 分组计数结果行（方向 / 难度维度统计复用）。
 */
@Data
public class CountRow {
    private String k;
    private Long cnt;
}

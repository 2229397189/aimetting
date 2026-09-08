package com.aimeeting.interview.ai.dao.mapper;

import lombok.Data;

/**
 * 分组计数行：{@code k} 为分组键，{@code cnt} 为条数。
 */
@Data
public class CountRow {

    /** 分组键（错误类型 / 业务类型）。 */
    private String k;

    /** 条数。 */
    private Long cnt;
}

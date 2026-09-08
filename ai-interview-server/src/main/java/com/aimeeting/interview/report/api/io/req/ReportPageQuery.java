package com.aimeeting.interview.report.api.io.req;

import com.aimeeting.interview.common.convention.result.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 报告分页查询请求（继承通用分页参数）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ReportPageQuery extends PageQuery {
}

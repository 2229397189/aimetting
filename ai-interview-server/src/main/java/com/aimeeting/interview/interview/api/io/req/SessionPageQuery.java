package com.aimeeting.interview.interview.api.io.req;

import com.aimeeting.interview.common.convention.result.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 会话分页查询条件（与前端 {@code SessionPageQuery} 一致）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SessionPageQuery extends PageQuery {

    /** 状态过滤（可空）。 */
    private String status;

    /** 方向过滤（可空，t_interview_session.directions LIKE）。 */
    private String direction;
}

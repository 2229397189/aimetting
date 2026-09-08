package com.aimeeting.interview.admin.api.io.req;

import lombok.Data;

import java.io.Serializable;

/**
 * 管理端更新用户状态请求。
 */
@Data
public class AdminUpdateStatusReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 1 启用，0 禁用。 */
    private Integer status;
}

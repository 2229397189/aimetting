package com.aimeeting.interview.admin.api.io.resp;

import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 管理端用户项（与前端 AdminUserResp 对应）。
 */
@Data
public class AdminUserResp implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String username;

    private String nickname;

    private String email;

    private String role;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime lastLoginAt;
}

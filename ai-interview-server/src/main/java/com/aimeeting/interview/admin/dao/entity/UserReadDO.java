package com.aimeeting.interview.admin.dao.entity;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 用户只读投影（t_user），供管理端查询，不写入。
 */
@Data
public class UserReadDO {

    private Long id;

    private String username;

    private String nickname;

    private String email;

    private String role;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime lastLoginAt;
}

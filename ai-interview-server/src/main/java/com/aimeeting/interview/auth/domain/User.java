package com.aimeeting.interview.auth.domain;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 用户领域实体（纯 Java，无 Spring 依赖）。
 *
 * <p>与 {@code UserDO} 的区别：DO 面向表结构（含逻辑删除字段），
 * 领域实体面向业务语义，提供 {@link #isDisabled()} / {@link #isAdmin()} 等领域判断。
 */
@Data
public class User {

    /** 用户 ID。 */
    private Long id;

    /** 用户名（唯一）。 */
    private String username;

    /** BCrypt 密码摘要，绝不明文存储。 */
    private String passwordHash;

    /** 邮箱。 */
    private String email;

    /** 昵称。 */
    private String nickname;

    /** 头像 URL。 */
    private String avatar;

    /** 角色：USER / ADMIN。 */
    private String role;

    /** 状态：1 正常，0 禁用。 */
    private Integer status;

    /** 最近登录时间。 */
    private LocalDateTime lastLoginAt;

    /** 创建时间。 */
    private LocalDateTime createTime;

    /**
     * 账号是否被禁用。
     *
     * @return status == 0 时返回 true
     */
    public boolean isDisabled() {
        return status != null && status == 0;
    }

    /**
     * 是否管理员。
     *
     * @return role == ADMIN 时返回 true
     */
    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }

    /**
     * 构造默认角色/状态的初始化器：新注册用户默认为启用状态的普通用户。
     */
    public void initDefault() {
        if (this.role == null || this.role.isBlank()) {
            this.role = "USER";
        }
        if (this.status == null) {
            this.status = 1;
        }
        if (this.nickname == null || this.nickname.isBlank()) {
            this.nickname = this.username;
        }
    }
}

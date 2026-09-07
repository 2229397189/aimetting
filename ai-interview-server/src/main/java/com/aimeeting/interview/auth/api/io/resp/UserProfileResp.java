package com.aimeeting.interview.auth.api.io.resp;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户资料返回体（U-06）：合并 {@code t_user} 与 {@code t_user_profile} 的展示字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.ALWAYS)
public class UserProfileResp implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户 ID。 */
    private Long userId;

    /** 用户名。 */
    private String username;

    /** 昵称。 */
    private String nickname;

    /** 头像。 */
    private String avatar;

    /** 邮箱。 */
    private String email;

    /** 角色。 */
    private String role;

    /** 目标岗位。 */
    private String targetPosition;

    /** 工作年限。 */
    private Integer workYears;

    /** 自我介绍。 */
    private String intro;

    /** 手机号。 */
    private String phone;

    /** 最近登录时间。 */
    private LocalDateTime lastLoginAt;

    /** 注册时间。 */
    private LocalDateTime createTime;
}

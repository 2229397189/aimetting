package com.aimeeting.interview.auth.api.io.resp;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 注册返回体（U-01）：返回 userId / username 与令牌，注册后免二次登录。
 *
 * <p>说明：架构文档 3.2 的 resp 清单未单列本类，
 * 但注册接口需同时返回用户标识与令牌（PRD U-01 + 注册后置免登），故补充。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterResp implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 新用户 ID。 */
    private Long userId;

    /** 用户名。 */
    private String username;

    /** 昵称。 */
    private String nickname;

    /** 角色。 */
    private String role;

    /** 注册后直接下发的令牌。 */
    private TokenResp token;
}

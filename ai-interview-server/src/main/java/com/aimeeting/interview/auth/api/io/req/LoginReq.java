package com.aimeeting.interview.auth.api.io.req;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;
import lombok.Data;

/**
 * 用户登录请求（U-02）：支持用户名或邮箱 + 密码。
 */
@Data
public class LoginReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 登录账号：用户名或邮箱。 */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /** 明文密码。 */
    @NotBlank(message = "密码不能为空")
    private String password;
}

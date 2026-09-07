package com.aimeeting.interview.auth.api.io.req;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import lombok.Data;

/**
 * 用户注册请求（U-01）。
 *
 * <p>用户名 4-20 位且只允许字母、数字、下划线；密码策略由 {@code PasswordPolicy} 二次校验。
 */
@Data
public class RegisterReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户名：4-20 位字母/数字/下划线，且首字符必须为字母。 */
    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]{3,19}$", message = "用户名需为 4-20 位字母/数字/下划线，且以字母开头")
    private String username;

    /** 明文密码：8-20 位且含字母与数字。 */
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 20, message = "密码长度需为 8-20 位")
    private String password;

    /** 邮箱，可选。 */
    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱长度不能超过 128")
    private String email;

    /** 昵称，可选，默认与用户名一致。 */
    @Size(max = 64, message = "昵称长度不能超过 64")
    private String nickname;
}

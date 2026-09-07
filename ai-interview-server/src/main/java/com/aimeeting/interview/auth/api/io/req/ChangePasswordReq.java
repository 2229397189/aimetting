package com.aimeeting.interview.auth.api.io.req;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;
import lombok.Data;

/**
 * 修改密码请求（U-07）：需校验原密码。
 */
@Data
public class ChangePasswordReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 原密码。 */
    @NotBlank(message = "原密码不能为空")
    private String oldPassword;

    /** 新密码：8-20 位且含字母与数字。 */
    @NotBlank(message = "新密码不能为空")
    private String newPassword;
}

package com.aimeeting.interview.auth.api.io.req;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;
import lombok.Data;

/**
 * 刷新令牌请求（U-04）。
 */
@Data
public class RefreshTokenReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /** refreshToken。 */
    @NotBlank(message = "refreshToken 不能为空")
    private String refreshToken;
}

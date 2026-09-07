package com.aimeeting.interview.auth.api.io.resp;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 令牌返回体。
 *
 * <p>{@code tokenType} 固定为 {@code Bearer}，便于前端直接拼接 Authorization 头。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenResp implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 访问令牌。 */
    private String accessToken;

    /** 刷新令牌。 */
    private String refreshToken;

    /** 令牌类型，固定 Bearer。 */
    private String tokenType;

    /** accessToken 有效期（秒）。 */
    private Long accessExpireSeconds;

    /** refreshToken 有效期（秒）。 */
    private Long refreshExpireSeconds;
}

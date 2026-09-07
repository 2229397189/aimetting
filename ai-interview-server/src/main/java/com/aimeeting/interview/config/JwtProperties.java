package com.aimeeting.interview.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置（{@code ai-interview.jwt}）。
 *
 * <p>有效期：accessToken 2h、refreshToken 7d（BR-19）。
 * 密钥长度需 &gt;= 32 字节（jjwt HS256 强制要求）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai-interview.jwt")
public class JwtProperties {

    /** HS256 签名密钥，至少 32 字节。 */
    private String secret = "ai-meeting-interview-platform-default-jwt-secret-key-please-change-me-2024";

    /** accessToken 有效期（秒），默认 7200。 */
    private long accessExpireSeconds = 7200L;

    /** refreshToken 有效期（秒），默认 604800。 */
    private long refreshExpireSeconds = 604800L;

    /** 签发者。 */
    private String issuer = "ai-interview-server";
}

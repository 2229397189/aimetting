package com.aimeeting.interview.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aimeeting.interview.auth.service.JwtClaims;
import com.aimeeting.interview.auth.service.JwtTokenProvider;
import com.aimeeting.interview.auth.service.TokenType;
import com.aimeeting.interview.common.convention.errorcode.BaseErrorCode;
import com.aimeeting.interview.common.convention.exception.ClientException;
import com.aimeeting.interview.config.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link JwtTokenProvider} 单测：生成 -&gt; 解析往返、过期、类型区分。
 *
 * <p>不依赖 Spring 容器：直接 {@code new JwtTokenProvider(props)}。
 */
class JwtTokenProviderTest {

    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("ai-meeting-interview-platform-default-jwt-secret-key-please-change-me-2024");
        properties.setAccessExpireSeconds(7200L);
        properties.setRefreshExpireSeconds(604800L);
        properties.setIssuer("ai-interview-server");
        provider = new JwtTokenProvider(properties);
    }

    @Test
    @DisplayName("accessToken 生成后可解析出完整的 userId / username / role，且是合法三段 JWT")
    void generateAndParseAccessToken() {
        String token = provider.generateAccessToken(1001L, "luqiang", "USER");

        // 合法 JWT 必须是 header.payload.signature 三段
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length, "JWT 应为三段结构");

        JwtClaims claims = provider.parse(token);
        assertEquals(1001L, claims.userId());
        assertEquals("luqiang", claims.username());
        assertEquals("USER", claims.role());
        assertEquals(TokenType.ACCESS, claims.type());
    }

    @Test
    @DisplayName("refreshToken 与 accessToken 类型可区分，refresh 不携带角色")
    void refreshTokenTypeDiffers() {
        String accessToken = provider.generateAccessToken(1002L, "demo", "USER");
        String refreshToken = provider.generateRefreshToken(1002L);

        assertEquals(TokenType.ACCESS, provider.typeOf(accessToken));
        assertEquals(TokenType.REFRESH, provider.typeOf(refreshToken));

        JwtClaims refreshClaims = provider.parse(refreshToken);
        assertEquals(1002L, refreshClaims.userId());
        assertEquals(TokenType.REFRESH, refreshClaims.type());
    }

    @Test
    @DisplayName("令牌过期后解析抛 ClientException(A0202)")
    void expiredTokenThrows() {
        JwtProperties shortLived = new JwtProperties();
        shortLived.setSecret("ai-meeting-interview-platform-default-jwt-secret-key-please-change-me-2024");
        shortLived.setAccessExpireSeconds(-1L);
        JwtTokenProvider shortLivedProvider = new JwtTokenProvider(shortLived);

        String token = shortLivedProvider.generateAccessToken(1003L, "expired", "USER");
        ClientException exception = assertThrows(ClientException.class,
                () -> shortLivedProvider.parse(token));
        assertEquals(BaseErrorCode.TOKEN_EXPIRED.code(), exception.getErrorCode());
    }

    @Test
    @DisplayName("非法令牌与空令牌抛 ClientException(A0201)")
    void invalidTokenThrows() {
        ClientException blankException = assertThrows(ClientException.class,
                () -> provider.parse(""));
        assertEquals(BaseErrorCode.TOKEN_MISSING.code(), blankException.getErrorCode());

        ClientException garbageException = assertThrows(ClientException.class,
                () -> provider.parse("not.a.jwt"));
        assertEquals(BaseErrorCode.TOKEN_MISSING.code(), garbageException.getErrorCode());
    }

    @Test
    @DisplayName("jti 唯一且可解析，剩余秒数在有效期内大于 0")
    void jtiAndRemainingSeconds() {
        String first = provider.generateAccessToken(1004L, "u1", "USER");
        String second = provider.generateAccessToken(1004L, "u1", "USER");

        String firstJti = provider.jtiOf(first);
        String secondJti = provider.jtiOf(second);
        assertNotNull(firstJti);
        assertNotNull(secondJti);
        assertTrue(!firstJti.equals(secondJti), "同一用户两次签发的 jti 应不同");

        long remain = provider.remainingSeconds(first);
        assertTrue(remain > 0L && remain <= provider.getAccessExpireSeconds(),
                "剩余有效期应在 (0, 7200] 区间，实际=" + remain);

        assertEquals(7200L, provider.getAccessExpireSeconds());
        assertEquals(604800L, provider.getRefreshExpireSeconds());
    }
}

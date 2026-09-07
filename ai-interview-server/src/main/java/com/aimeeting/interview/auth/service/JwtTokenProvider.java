package com.aimeeting.interview.auth.service;

import com.aimeeting.interview.common.convention.errorcode.BaseErrorCode;
import com.aimeeting.interview.common.convention.exception.ClientException;
import com.aimeeting.interview.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * JWT 令牌签发与解析（jjwt 0.12.6）。
 *
 * <p>关键设计：
 * <ul>
 *   <li>HS256 对称签名；密钥长度必须 &gt;= 32 字节（由 {@link Keys#hmacShaKeyFor} 校验）。</li>
 *   <li>每个令牌带唯一 {@code jti}，供退出登录时加入短期黑名单（U-05）。</li>
 *   <li>{@code type} 声明区分 access / refresh，refresh 令牌无法直接访问业务接口。</li>
 *   <li>解析失败按原因区分错误码：过期 -&gt; A0202，其余 -&gt; A0201。</li>
 * </ul>
 */
@Slf4j
@Component
public class JwtTokenProvider {

    /** 载荷中的令牌类型键。 */
    public static final String CLAIM_TYPE = "type";

    /** 载荷中的角色键。 */
    public static final String CLAIM_ROLE = "role";

    /** 载荷中的用户名键。 */
    public static final String CLAIM_USERNAME = "username";

    private final JwtProperties jwtProperties;

    private final SecretKey secretKey;

    /**
     * @param jwtProperties JWT 配置
     */
    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 签发 accessToken。
     *
     * @param userId   用户 ID
     * @param username 用户名
     * @param role     角色
     * @return JWT 字符串
     */
    public String generateAccessToken(Long userId, String username, String role) {
        return buildToken(userId, username, role, TokenType.ACCESS,
                jwtProperties.getAccessExpireSeconds());
    }

    /**
     * 签发 refreshToken（仅携带 userId 与类型，不携带角色，降低泄露影响面）。
     *
     * @param userId 用户 ID
     * @return JWT 字符串
     */
    public String generateRefreshToken(Long userId) {
        return buildToken(userId, null, null, TokenType.REFRESH,
                jwtProperties.getRefreshExpireSeconds());
    }

    /**
     * 解析并校验令牌。
     *
     * @param token JWT 字符串
     * @return 令牌载荷
     * @throws ClientException 过期抛 A0202；签名错误/格式错误抛 A0201
     */
    public JwtClaims parse(String token) {
        if (token == null || token.isBlank()) {
            throw new ClientException(BaseErrorCode.TOKEN_MISSING);
        }
        try {
            Claims claims = parseClaims(token);
            Long userId = Long.valueOf(claims.getSubject());
            String username = claims.get(CLAIM_USERNAME, String.class);
            String role = claims.get(CLAIM_ROLE, String.class);
            TokenType type = TokenType.of(claims.get(CLAIM_TYPE, String.class));
            return new JwtClaims(userId, username, role, type);
        } catch (ExpiredJwtException e) {
            throw new ClientException(BaseErrorCode.TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("[JWT] 令牌解析失败: {}", e.getMessage());
            throw new ClientException(BaseErrorCode.TOKEN_MISSING);
        }
    }

    /**
     * 读取令牌类型（不校验过期之外的其它约束）。
     *
     * @param token JWT 字符串
     * @return 令牌类型，解析失败返回 null
     */
    public TokenType typeOf(String token) {
        try {
            String type = parseClaims(token).get(CLAIM_TYPE, String.class);
            return TokenType.of(type);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 读取令牌的 jti（JWT ID），用于退出登录黑名单。
     *
     * @param token JWT 字符串
     * @return jti，解析失败返回 null
     */
    public String jtiOf(String token) {
        try {
            return parseClaims(token).getId();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 计算令牌剩余有效秒数（用于黑名单 TTL，避免黑名单存活超过令牌本身）。
     *
     * @param token JWT 字符串
     * @return 剩余秒数，已过期或解析失败返回 0
     */
    public long remainingSeconds(String token) {
        try {
            Date expiration = parseClaims(token).getExpiration();
            long remain = expiration.getTime() - System.currentTimeMillis();
            return remain <= 0L ? 0L : remain / 1000L;
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * accessToken 有效期（秒）。
     *
     * @return 7200
     */
    public long getAccessExpireSeconds() {
        return jwtProperties.getAccessExpireSeconds();
    }

    /**
     * refreshToken 有效期（秒）。
     *
     * @return 604800
     */
    public long getRefreshExpireSeconds() {
        return jwtProperties.getRefreshExpireSeconds();
    }

    private String buildToken(Long userId, String username, String role,
                              TokenType type, long expireSeconds) {
        Instant now = Instant.now();
        Instant expireAt = now.plusSeconds(expireSeconds);
        return Jwts.builder()
                .id(UUID.randomUUID().toString().replace("-", ""))
                .subject(String.valueOf(userId))
                .claim(CLAIM_USERNAME, username)
                .claim(CLAIM_ROLE, role)
                .claim(CLAIM_TYPE, type.name())
                .issuer(jwtProperties.getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expireAt))
                .signWith(secretKey)
                .compact();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

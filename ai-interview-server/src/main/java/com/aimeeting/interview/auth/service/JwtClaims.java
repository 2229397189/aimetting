package com.aimeeting.interview.auth.service;

/**
 * JWT 载荷的业务视图。
 *
 * @param userId   用户 ID
 * @param username 用户名
 * @param role     角色：USER / ADMIN
 * @param type     令牌类型
 */
public record JwtClaims(Long userId, String username, String role, TokenType type) {
}

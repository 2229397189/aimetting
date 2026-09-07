package com.aimeeting.interview.auth.service;

import com.aimeeting.interview.auth.api.io.req.LoginReq;
import com.aimeeting.interview.auth.api.io.req.RefreshTokenReq;
import com.aimeeting.interview.auth.api.io.req.RegisterReq;
import com.aimeeting.interview.auth.api.io.resp.LoginResp;
import com.aimeeting.interview.auth.api.io.resp.RegisterResp;
import com.aimeeting.interview.auth.api.io.resp.TokenResp;
import com.aimeeting.interview.auth.api.io.resp.UserProfileResp;
import com.aimeeting.interview.auth.dao.entity.UserDO;
import com.aimeeting.interview.auth.dao.repository.UserRepository;
import com.aimeeting.interview.auth.domain.LoginFailPolicy;
import com.aimeeting.interview.auth.domain.PasswordPolicy;
import com.aimeeting.interview.common.cache.CacheService;
import com.aimeeting.interview.common.convention.errorcode.BaseErrorCode;
import com.aimeeting.interview.common.convention.exception.ClientException;
import com.aimeeting.interview.common.convention.exception.ServiceException;
import com.aimeeting.interview.config.SecurityProperties;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 认证服务实现。
 *
 * <p>关键业务规则：
 * <ul>
 *   <li>注册：用户名唯一、邮箱唯一（U-01）；密码 BCrypt(10) 落库，日志绝不打印明文。</li>
 *   <li>登录：支持用户名/邮箱双通道；失败计数达 5 次锁定 5 分钟（BR-20）；
 *       禁用账号拒绝登录（B0103）。</li>
 *   <li>刷新：仅接受 {@code type=REFRESH} 的令牌，否则返回 A0203。</li>
 *   <li>登出：把 accessToken 的 jti 加入黑名单，TTL = 令牌剩余有效期。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    /** Bearer 前缀。 */
    private static final String BEARER_PREFIX = "Bearer ";

    private final UserRepository userRepository;

    private final UserService userService;

    private final JwtTokenProvider jwtTokenProvider;

    private final TokenBlacklistService tokenBlacklistService;

    private final CacheService cacheService;

    private final SecurityProperties securityProperties;

    @Override
    public RegisterResp register(RegisterReq req) {
        String username = req.getUsername().trim();
        String email = req.getEmail() == null ? null : req.getEmail().trim();
        // 密码策略：8-20 位且同时含字母与数字（BR-18）
        PasswordPolicy.validate(req.getPassword());

        if (userRepository.existsByUsername(username)) {
            throw new ClientException("用户名已存在", BaseErrorCode.USER_EXIST);
        }
        if (email != null && !email.isEmpty() && userRepository.existsByEmail(email)) {
            throw new ClientException("邮箱已被占用", BaseErrorCode.USER_EXIST);
        }

        UserDO user = new UserDO();
        user.setUsername(username);
        user.setPasswordHash(PasswordPolicy.encode(req.getPassword()));
        user.setEmail(email == null || email.isEmpty() ? null : email);
        user.setNickname(req.getNickname() == null || req.getNickname().isBlank()
                ? username : req.getNickname().trim());
        user.setRole("USER");
        user.setStatus(1);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.insert(user);

        // 初始化一条空资料，保证 GET /api/user/profile 永远有记录
        userService.initProfile(user.getId());

        log.info("[Auth] 用户注册成功, userId={}, username={}", user.getId(), username);
        return RegisterResp.builder()
                .userId(user.getId())
                .username(username)
                .nickname(user.getNickname())
                .role(user.getRole())
                .token(buildTokenResp(user.getId(), username, user.getRole()))
                .build();
    }

    @Override
    public LoginResp login(LoginReq req) {
        String account = req.getUsername().trim();
        String failKey = LoginFailPolicy.cacheKey(account);

        // 1. 锁定检查（BR-20）
        int failCount = cacheService.get(failKey, Integer.class).orElse(0);
        if (LoginFailPolicy.shouldLock(failCount)) {
            throw new ClientException(BaseErrorCode.ACCOUNT_LOCKED);
        }

        // 2. 账号存在性与密码校验（不区分「用户不存在」与「密码错误」，统一 A0101）
        UserDO user = userRepository.findByUsernameOrEmail(account);
        if (user == null || !PasswordPolicy.matches(req.getPassword(), user.getPasswordHash())) {
            long current = cacheService.increment(failKey, LoginFailPolicy.LOCK_DURATION);
            if (LoginFailPolicy.shouldLock((int) current)) {
                throw new ClientException(BaseErrorCode.ACCOUNT_LOCKED);
            }
            throw new ClientException(BaseErrorCode.PASSWORD_ERROR);
        }

        // 3. 禁用校验
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new ServiceException(BaseErrorCode.ACCOUNT_DISABLED);
        }

        // 4. 登录成功：清空失败计数 + 刷新最近登录时间
        cacheService.remove(failKey);
        userRepository.updateLastLoginAt(user.getId(), LocalDateTime.now());

        log.info("[Auth] 用户登录成功, userId={}, username={}", user.getId(), user.getUsername());
        return LoginResp.builder()
                .token(buildTokenResp(user.getId(), user.getUsername(), user.getRole()))
                .user(userService.getProfileResp(user.getId()))
                .build();
    }

    @Override
    public TokenResp refresh(RefreshTokenReq req) {
        String refreshToken = req.getRefreshToken().trim();
        JwtClaims claims;
        try {
            claims = jwtTokenProvider.parse(refreshToken);
        } catch (ClientException e) {
            // refreshToken 过期或非法统一按「登录状态失效」处理（U-04）
            throw new ClientException(BaseErrorCode.REFRESH_TOKEN_EXPIRED);
        }
        if (claims.type() != TokenType.REFRESH) {
            throw new ClientException("令牌类型不正确", BaseErrorCode.REFRESH_TOKEN_EXPIRED);
        }
        UserDO user = userRepository.findById(claims.userId());
        if (user == null || (user.getStatus() != null && user.getStatus() == 0)) {
            throw new ClientException(BaseErrorCode.REFRESH_TOKEN_EXPIRED);
        }
        // 换发新的 accessToken，refreshToken 原样返回，避免强制用户重新登录
        String newAccessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getUsername(), user.getRole());
        return TokenResp.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .accessExpireSeconds(jwtTokenProvider.getAccessExpireSeconds())
                .refreshExpireSeconds(jwtTokenProvider.getRefreshExpireSeconds())
                .build();
    }

    @Override
    public void logout(String bearerToken) {
        String token = extractToken(bearerToken);
        if (token == null) {
            return;
        }
        String jti = jwtTokenProvider.jtiOf(token);
        long remainSeconds = jwtTokenProvider.remainingSeconds(token);
        tokenBlacklistService.blacklist(jti, remainSeconds);
    }

    /**
     * 构造令牌返回体。
     *
     * @param userId   用户 ID
     * @param username 用户名
     * @param role     角色
     * @return 令牌返回体
     */
    private TokenResp buildTokenResp(Long userId, String username, String role) {
        return TokenResp.builder()
                .accessToken(jwtTokenProvider.generateAccessToken(userId, username, role))
                .refreshToken(jwtTokenProvider.generateRefreshToken(userId))
                .tokenType("Bearer")
                .accessExpireSeconds(jwtTokenProvider.getAccessExpireSeconds())
                .refreshExpireSeconds(jwtTokenProvider.getRefreshExpireSeconds())
                .build();
    }

    /**
     * 从 Authorization 头中提取裸 token。
     *
     * @param authorization Authorization 头
     * @return 裸 token，无法提取时返回 null
     */
    private String extractToken(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            return null;
        }
        String prefix = securityProperties.getTokenPrefix() == null
                ? BEARER_PREFIX : securityProperties.getTokenPrefix();
        String value = authorization.trim();
        if (value.regionMatches(true, 0, prefix, 0, prefix.length())) {
            return value.substring(prefix.length()).trim();
        }
        return value;
    }
}

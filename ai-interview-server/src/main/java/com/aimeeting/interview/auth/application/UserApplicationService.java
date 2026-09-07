package com.aimeeting.interview.auth.application;

import com.aimeeting.interview.auth.api.io.req.ChangePasswordReq;
import com.aimeeting.interview.auth.api.io.req.UpdateProfileReq;
import com.aimeeting.interview.auth.api.io.resp.UserProfileResp;
import com.aimeeting.interview.auth.api.io.resp.UserStatsResp;
import com.aimeeting.interview.auth.service.JwtTokenProvider;
import com.aimeeting.interview.auth.service.TokenBlacklistService;
import com.aimeeting.interview.auth.service.UserService;
import com.aimeeting.interview.config.JwtProperties;
import com.aimeeting.interview.config.SecurityProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户应用层：资料读写、修改密码、数据概览的用例编排。
 *
 * <p>关键规则：修改密码成功后把当前 accessToken 拉黑（U-07 —— 旧 token 立即失效，
 * 需重新登录），避免用户改密后旧令牌仍在有效期内可继续访问。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserApplicationService {

    private final UserService userService;

    private final TokenBlacklistService tokenBlacklistService;

    private final JwtTokenProvider jwtTokenProvider;

    private final JwtProperties jwtProperties;

    private final SecurityProperties securityProperties;

    /**
     * 查询当前登录用户资料（U-06）。
     *
     * @param userId 用户 ID
     * @return 资料
     */
    public UserProfileResp getProfile(Long userId) {
        return userService.getProfileResp(userId);
    }

    /**
     * 修改用户资料（U-06）。
     *
     * @param userId 用户 ID
     * @param req    修改请求
     * @return 修改后的资料
     */
    @Transactional(rollbackFor = Exception.class)
    public UserProfileResp updateProfile(Long userId, UpdateProfileReq req) {
        return userService.updateProfile(userId, req);
    }

    /**
     * 修改密码（U-07）：成功后当前令牌立即失效。
     *
     * @param userId        用户 ID
     * @param req           修改密码请求
     * @param authorization 当前 Authorization 请求头，可为 null
     */
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long userId, ChangePasswordReq req, String authorization) {
        userService.changePassword(userId, req);
        String token = extractToken(authorization);
        if (token != null) {
            String jti = jwtTokenProvider.jtiOf(token);
            if (jti != null) {
                tokenBlacklistService.blacklist(jti, jwtProperties.getAccessExpireSeconds());
            }
        }
    }

    /**
     * 个人数据概览（U-08）。
     *
     * @param userId 用户 ID
     * @return 统计信息
     */
    public UserStatsResp getStats(Long userId) {
        return userService.getStats(userId);
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
                ? "Bearer " : securityProperties.getTokenPrefix();
        String value = authorization.trim();
        if (value.regionMatches(true, 0, prefix, 0, prefix.length())) {
            return value.substring(prefix.length()).trim();
        }
        return value;
    }
}

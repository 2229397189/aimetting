package com.aimeeting.interview.auth.application;

import com.aimeeting.interview.auth.api.io.req.LoginReq;
import com.aimeeting.interview.auth.api.io.req.RefreshTokenReq;
import com.aimeeting.interview.auth.api.io.req.RegisterReq;
import com.aimeeting.interview.auth.api.io.resp.LoginResp;
import com.aimeeting.interview.auth.api.io.resp.RegisterResp;
import com.aimeeting.interview.auth.api.io.resp.TokenResp;
import com.aimeeting.interview.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 认证应用层：注册 / 登录 / 刷新 / 登出的用例编排与事务边界。
 *
 * <p>Controller 只做参数绑定与 {@code Result} 包装；本层负责：
 * <ul>
 *   <li>注册事务：写 {@code t_user} + 初始化 {@code t_user_profile} 必须原子。</li>
 *   <li>登录/刷新/登出：无写库或仅写缓存，不加事务，降低锁竞争。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthApplicationService {

    private final AuthService authService;

    /**
     * 注册（U-01）：用户主表与资料表同事务落库。
     *
     * @param req 注册请求
     * @return 注册结果（含 userId 与令牌）
     */
    @Transactional(rollbackFor = Exception.class)
    public RegisterResp register(RegisterReq req) {
        return authService.register(req);
    }

    /**
     * 登录（U-02）。
     *
     * @param req 登录请求
     * @return 登录结果（令牌 + 用户资料）
     */
    public LoginResp login(LoginReq req) {
        return authService.login(req);
    }

    /**
     * 刷新令牌（U-04）。
     *
     * @param req 刷新请求
     * @return 新令牌
     */
    public TokenResp refresh(RefreshTokenReq req) {
        return authService.refresh(req);
    }

    /**
     * 退出登录（U-05）：当前 accessToken 的 jti 进入短期黑名单。
     *
     * @param authorization Authorization 请求头
     */
    public void logout(String authorization) {
        authService.logout(authorization);
    }
}

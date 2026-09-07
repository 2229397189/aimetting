package com.aimeeting.interview.auth.service;

import com.aimeeting.interview.auth.api.io.req.ChangePasswordReq;
import com.aimeeting.interview.auth.api.io.req.LoginReq;
import com.aimeeting.interview.auth.api.io.req.RefreshTokenReq;
import com.aimeeting.interview.auth.api.io.req.RegisterReq;
import com.aimeeting.interview.auth.api.io.req.UpdateProfileReq;
import com.aimeeting.interview.auth.api.io.resp.LoginResp;
import com.aimeeting.interview.auth.api.io.resp.RegisterResp;
import com.aimeeting.interview.auth.api.io.resp.TokenResp;

/**
 * 认证服务：注册 / 登录 / 刷新 / 登出。
 *
 * <p>实现类位于 {@link AuthServiceImpl}，事务边界由 application 层把控。
 */
public interface AuthService {

    /**
     * 用户注册。
     *
     * @param req 注册请求
     * @return 注册结果（含 userId 与令牌）
     */
    RegisterResp register(RegisterReq req);

    /**
     * 用户登录。
     *
     * @param req 登录请求
     * @return 登录结果（令牌 + 用户资料）
     */
    LoginResp login(LoginReq req);

    /**
     * 用 refreshToken 换发新的 accessToken。
     *
     * @param req 刷新请求
     * @return 新令牌
     */
    TokenResp refresh(RefreshTokenReq req);

    /**
     * 退出登录：把当前 accessToken 的 jti 加入短期黑名单。
     *
     * @param bearerToken 形如 {@code Bearer xxx} 的 Authorization 头，允许为 null
     */
    void logout(String bearerToken);
}

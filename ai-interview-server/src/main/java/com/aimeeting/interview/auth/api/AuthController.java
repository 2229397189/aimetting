package com.aimeeting.interview.auth.api;

import com.aimeeting.interview.auth.api.io.req.LoginReq;
import com.aimeeting.interview.auth.api.io.req.RefreshTokenReq;
import com.aimeeting.interview.auth.api.io.req.RegisterReq;
import com.aimeeting.interview.auth.api.io.resp.LoginResp;
import com.aimeeting.interview.auth.api.io.resp.RegisterResp;
import com.aimeeting.interview.auth.api.io.resp.TokenResp;
import com.aimeeting.interview.auth.application.AuthApplicationService;
import com.aimeeting.interview.common.convention.result.Result;
import com.aimeeting.interview.common.convention.result.Results;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口：注册 / 登录 / 登出 / 刷新令牌。
 *
 * <p>分层约束：Controller 只做参数绑定、调用 application、包装 {@code Result}，
 * 不写任何业务判断。
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "认证", description = "注册 / 登录 / 登出 / 刷新令牌")
public class AuthController {

    private final AuthApplicationService authApplicationService;

    /**
     * 用户注册（公开）。
     *
     * @param req 注册请求
     * @return 注册结果（userId / username / token）
     */
    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "用户名 4-20 位，密码 8-20 位且含字母与数字")
    public Result<RegisterResp> register(@Valid @RequestBody RegisterReq req) {
        return Results.success(authApplicationService.register(req));
    }

    /**
     * 用户登录（公开）。
     *
     * @param req 登录请求（用户名或邮箱 + 密码）
     * @return 令牌与用户资料
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "支持用户名或邮箱登录，连续失败 5 次锁定 5 分钟")
    public Result<LoginResp> login(@Valid @RequestBody LoginReq req) {
        return Results.success(authApplicationService.login(req));
    }

    /**
     * 退出登录（需鉴权）。
     *
     * @param authorization Authorization 请求头
     * @return 空成功返回体
     */
    @PostMapping("/logout")
    @Operation(summary = "退出登录", description = "当前 accessToken 加入短期黑名单")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        authApplicationService.logout(authorization);
        return Results.success();
    }

    /**
     * 刷新令牌（公开）。
     *
     * @param req 刷新请求
     * @return 新 accessToken
     */
    @PostMapping("/token/refresh")
    @Operation(summary = "刷新令牌", description = "用 refreshToken 换发新的 accessToken")
    public Result<TokenResp> refresh(@Valid @RequestBody RefreshTokenReq req) {
        return Results.success(authApplicationService.refresh(req));
    }
}

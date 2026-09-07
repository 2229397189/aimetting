package com.aimeeting.interview.auth.api;

import com.aimeeting.interview.auth.api.io.req.ChangePasswordReq;
import com.aimeeting.interview.auth.api.io.req.UpdateProfileReq;
import com.aimeeting.interview.auth.api.io.resp.UserProfileResp;
import com.aimeeting.interview.auth.api.io.resp.UserStatsResp;
import com.aimeeting.interview.auth.application.UserApplicationService;
import com.aimeeting.interview.common.convention.annotation.CurrentUser;
import com.aimeeting.interview.common.convention.context.UserContext;
import com.aimeeting.interview.common.convention.result.Result;
import com.aimeeting.interview.common.convention.result.Results;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户接口：资料查看/修改、修改密码、数据概览。
 *
 * <p>登录态通过 {@code @CurrentUser UserContext} 注入，
 * 由 {@code CurrentUserMethodArgumentResolver} 从 request attribute 读取。
 */
@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "用户", description = "个人资料 / 修改密码 / 数据概览")
public class UserController {

    private final UserApplicationService userApplicationService;

    /**
     * 查询当前用户资料。
     *
     * @param userContext 当前登录态
     * @return 用户资料
     */
    @GetMapping("/profile")
    @Operation(summary = "查询当前用户资料")
    public Result<UserProfileResp> getProfile(@CurrentUser UserContext userContext) {
        return Results.success(userApplicationService.getProfile(userContext.getUserId()));
    }

    /**
     * 修改当前用户资料。
     *
     * @param userContext 当前登录态
     * @param req         修改请求
     * @return 修改后的资料
     */
    @PutMapping("/profile")
    @Operation(summary = "修改当前用户资料", description = "仅更新非空字段")
    public Result<UserProfileResp> updateProfile(@CurrentUser UserContext userContext,
                                                 @Valid @RequestBody UpdateProfileReq req) {
        return Results.success(userApplicationService.updateProfile(userContext.getUserId(), req));
    }

    /**
     * 修改密码，成功后当前令牌失效需重新登录。
     *
     * @param userContext   当前登录态
     * @param req           修改密码请求
     * @param authorization Authorization 请求头
     * @return 空成功返回体
     */
    @PostMapping("/password")
    @Operation(summary = "修改密码", description = "校验原密码，成功后当前令牌失效")
    public Result<Void> changePassword(@CurrentUser UserContext userContext,
                                       @Valid @RequestBody ChangePasswordReq req,
                                       @RequestHeader(value = "Authorization", required = false)
                                       String authorization) {
        userApplicationService.changePassword(userContext.getUserId(), req, authorization);
        return Results.success();
    }

    /**
     * 个人数据概览。
     *
     * @param userContext 当前登录态
     * @return 统计信息
     */
    @GetMapping("/stats")
    @Operation(summary = "个人数据概览", description = "累计场次 / 完成场次 / 平均分 / 最近 7 日趋势")
    public Result<UserStatsResp> stats(@CurrentUser UserContext userContext) {
        return Results.success(userApplicationService.getStats(userContext.getUserId()));
    }
}

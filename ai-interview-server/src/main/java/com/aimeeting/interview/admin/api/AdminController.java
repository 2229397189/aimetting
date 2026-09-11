package com.aimeeting.interview.admin.api;

import com.aimeeting.interview.admin.api.io.req.AdminUpdateStatusReq;
import com.aimeeting.interview.admin.api.io.resp.AiCallLogResp;
import com.aimeeting.interview.admin.api.io.resp.AiHealthResp;
import com.aimeeting.interview.admin.api.io.resp.AdminUserResp;
import com.aimeeting.interview.admin.api.io.resp.OverviewStatsResp;
import com.aimeeting.interview.admin.api.io.resp.SessionTrendResp;
import com.aimeeting.interview.admin.service.AdminService;
import com.aimeeting.interview.common.convention.annotation.CurrentUser;
import com.aimeeting.interview.common.convention.context.UserContext;
import com.aimeeting.interview.common.convention.errorcode.BaseErrorCode;
import com.aimeeting.interview.common.convention.exception.ClientException;
import com.aimeeting.interview.common.convention.result.PageInfo;
import com.aimeeting.interview.common.convention.result.PageQuery;
import com.aimeeting.interview.common.convention.result.Result;
import com.aimeeting.interview.common.convention.result.Results;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端接口（/api/admin，全部需要 ADMIN 角色）。
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "管理端", description = "用户管理 / 平台统计 / AI 调用日志与健康")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    @Operation(summary = "用户分页列表")
    public Result<PageInfo<AdminUserResp>> users(@CurrentUser UserContext user,
                                                PageQuery q,
                                                @RequestParam(value = "keyword", required = false) String keyword) {
        requireAdmin(user);
        return Results.success(adminService.users(q, keyword));
    }

    @PutMapping("/users/{id}/status")
    @Operation(summary = "启用 / 禁用用户", description = "status: 1 启用，0 禁用")
    public Result<Boolean> updateUserStatus(@CurrentUser UserContext user,
                                           @PathVariable Long id,
                                           @RequestBody AdminUpdateStatusReq req) {
        requireAdmin(user);
        adminService.updateUserStatus(id, req.getStatus());
        return Results.success(true);
    }

    @GetMapping("/statistics/overview")
    @Operation(summary = "平台总览统计")
    public Result<OverviewStatsResp> overview(@CurrentUser UserContext user) {
        requireAdmin(user);
        return Results.success(adminService.overview());
    }

    @GetMapping("/statistics/sessions")
    @Operation(summary = "会话趋势与方向分布")
    public Result<SessionTrendResp> sessionTrend(@CurrentUser UserContext user,
                                               @RequestParam(value = "days", defaultValue = "7") int days) {
        requireAdmin(user);
        return Results.success(adminService.sessionTrend(days));
    }

    @GetMapping("/ai-calls")
    @Operation(summary = "AI 调用日志分页")
    public Result<PageInfo<AiCallLogResp>> aiCalls(@CurrentUser UserContext user,
                                                 PageQuery q,
                                                 @RequestParam(value = "bizType", required = false) String bizType,
                                                 @RequestParam(value = "agentId", required = false) String agentId,
                                                 @RequestParam(value = "success", required = false) Boolean success) {
        requireAdmin(user);
        return Results.success(adminService.aiCalls(q, bizType, agentId, success));
    }

    @GetMapping("/ai/health")
    @Operation(summary = "AI 服务健康状态")
    public Result<AiHealthResp> aiHealth(@CurrentUser UserContext user) {
        requireAdmin(user);
        return Results.success(adminService.aiHealth());
    }

    private void requireAdmin(UserContext user) {
        if (user == null || !user.isAdmin()) {
            throw new ClientException(BaseErrorCode.FORBIDDEN);
        }
    }
}

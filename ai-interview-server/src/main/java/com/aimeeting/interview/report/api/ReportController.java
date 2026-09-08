package com.aimeeting.interview.report.api;

import com.aimeeting.interview.common.convention.annotation.CurrentUser;
import com.aimeeting.interview.common.convention.result.PageInfo;
import com.aimeeting.interview.common.convention.result.Result;
import com.aimeeting.interview.common.convention.result.Results;
import com.aimeeting.interview.report.api.io.req.ReportPageQuery;
import com.aimeeting.interview.report.api.io.resp.ReportBriefResp;
import com.aimeeting.interview.report.api.io.resp.ReportDetailResp;
import com.aimeeting.interview.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 面试报告接口（/api/reports）。
 *
 * <p>全部需要登录态；详情/删除/导出会校验归属（越权 OWNERSHIP_DENIED）。
 */
@Slf4j
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "报告", description = "面试报告生成 / 查询 / 列表 / 删除 / 导出")
public class ReportController {

    private final ReportService reportService;

    @PostMapping("/{sessionId}/generate")
    @Operation(summary = "生成报告", description = "幂等，返回 reportId")
    public Result<Long> generate(@CurrentUser Long userId, @PathVariable Long sessionId) {
        return Results.success(reportService.generate(userId, sessionId));
    }

    @GetMapping("/{sessionId}")
    @Operation(summary = "按会话查询报告")
    public Result<ReportDetailResp> getBySession(@CurrentUser Long userId, @PathVariable Long sessionId) {
        return Results.success(reportService.getBySession(userId, sessionId));
    }

    @GetMapping("/id/{id}")
    @Operation(summary = "按报告 ID 查询详情")
    public Result<ReportDetailResp> getById(@CurrentUser Long userId, @PathVariable Long id) {
        return Results.success(reportService.getById(userId, id));
    }

    @GetMapping
    @Operation(summary = "报告分页列表")
    public Result<PageInfo<ReportBriefResp>> page(@CurrentUser Long userId, ReportPageQuery q) {
        return Results.success(reportService.page(userId, q));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除报告")
    public Result<Boolean> remove(@CurrentUser Long userId, @PathVariable Long id) {
        reportService.remove(userId, id);
        return Results.success(true);
    }

    @GetMapping("/{id}/export")
    @Operation(summary = "导出 Markdown 文本")
    public Result<String> exportMarkdown(@CurrentUser Long userId, @PathVariable Long id) {
        return Results.success(reportService.exportMarkdown(userId, id));
    }
}

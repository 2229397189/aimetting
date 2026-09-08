package com.aimeeting.interview.resume.api;

import com.aimeeting.interview.common.convention.annotation.CurrentUser;
import com.aimeeting.interview.common.convention.context.UserContext;
import com.aimeeting.interview.common.convention.result.PageInfo;
import com.aimeeting.interview.common.convention.result.PageQuery;
import com.aimeeting.interview.common.convention.result.Result;
import com.aimeeting.interview.common.convention.result.Results;
import com.aimeeting.interview.resume.api.io.req.ResumeParseReq;
import com.aimeeting.interview.resume.api.io.resp.ResumeDetailResp;
import com.aimeeting.interview.resume.api.io.resp.ResumeResp;
import com.aimeeting.interview.resume.service.ResumeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 简历接口（/api/resume）。
 *
 * <p>全部需要登录态；详情/删除/设默认会校验归属，越权返回 OWNERSHIP_DENIED（B0303）。
 */
@Slf4j
@RestController
@RequestMapping("/api/resume")
@RequiredArgsConstructor
@Tag(name = "简历", description = "简历解析 / 上传 / 列表 / 详情 / 删除 / 设默认")
public class ResumeController {

    private final ResumeService resumeService;

    @PostMapping("/parse")
    @Operation(summary = "解析简历文本", description = "文本 → AI 解析 + 评分 + 建议，返回 resumeId")
    public Result<Long> parse(@CurrentUser Long userId, @RequestBody ResumeParseReq req) {
        return Results.success(resumeService.parse(userId, req));
    }

    @PostMapping("/upload")
    @Operation(summary = "上传简历文件", description = "TXT / MD / PDF（≤5MB），返回 resumeId")
    public Result<Long> upload(@CurrentUser Long userId,
                              @RequestParam("file") MultipartFile file,
                              @RequestParam(value = "title", required = false) String title) {
        return Results.success(resumeService.upload(userId, file, title));
    }

    @GetMapping
    @Operation(summary = "简历分页列表")
    public Result<PageInfo<ResumeResp>> page(@CurrentUser Long userId, PageQuery q) {
        return Results.success(resumeService.page(userId, q));
    }

    @GetMapping("/{id}")
    @Operation(summary = "简历详情", description = "含解析结果与评分")
    public Result<ResumeDetailResp> detail(@CurrentUser Long userId, @PathVariable Long id) {
        return Results.success(resumeService.detail(userId, id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除简历")
    public Result<Boolean> remove(@CurrentUser Long userId, @PathVariable Long id) {
        resumeService.remove(userId, id);
        return Results.success(true);
    }

    @PutMapping("/{id}/default")
    @Operation(summary = "设为默认简历")
    public Result<Boolean> markDefault(@CurrentUser Long userId, @PathVariable Long id) {
        resumeService.markDefault(userId, id);
        return Results.success(true);
    }
}

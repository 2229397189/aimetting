package com.aimeeting.interview.question.api;

import com.aimeeting.interview.common.convention.annotation.CurrentUser;
import com.aimeeting.interview.common.convention.context.UserContext;
import com.aimeeting.interview.common.convention.errorcode.BaseErrorCode;
import com.aimeeting.interview.common.convention.exception.ClientException;
import com.aimeeting.interview.common.convention.result.Result;
import com.aimeeting.interview.common.convention.result.Results;
import com.aimeeting.interview.question.api.io.req.QuestionQueryReq;
import com.aimeeting.interview.question.api.io.req.QuestionSaveReq;
import com.aimeeting.interview.question.api.io.req.RandomQuestionReq;
import com.aimeeting.interview.question.api.io.resp.DirectionsResp;
import com.aimeeting.interview.question.api.io.resp.QuestionDetailResp;
import com.aimeeting.interview.question.api.io.resp.QuestionImportResult;
import com.aimeeting.interview.question.api.io.resp.QuestionResp;
import com.aimeeting.interview.question.service.QuestionService;
import com.aimeeting.interview.common.convention.result.PageInfo;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 题库接口（/api/questions）。
 *
 * <p>除 {@code /directions} 为公开接口外，其余均需要登录；
 * 写操作（save / update / remove / import）额外要求 ADMIN 角色。
 */
@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @GetMapping
    public Result<PageInfo<QuestionResp>> page(QuestionQueryReq req) {
        return Results.success(questionService.page(req));
    }

    @GetMapping("/{id}")
    public Result<QuestionDetailResp> detail(@PathVariable Long id) {
        return Results.success(questionService.detail(id));
    }

    @PostMapping
    public Result<Long> save(@CurrentUser UserContext user,
                             @Valid @RequestBody QuestionSaveReq req) {
        requireAdmin(user);
        return Results.success(questionService.save(req, user.getUserId()));
    }

    @PutMapping("/{id}")
    public Result<Boolean> update(@CurrentUser UserContext user,
                                  @PathVariable Long id,
                                  @Valid @RequestBody QuestionSaveReq req) {
        requireAdmin(user);
        return Results.success(questionService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> remove(@CurrentUser UserContext user, @PathVariable Long id) {
        requireAdmin(user);
        return Results.success(questionService.remove(id));
    }

    @PostMapping("/random")
    public Result<List<QuestionResp>> random(@RequestBody RandomQuestionReq req) {
        return Results.success(questionService.random(req));
    }

    @GetMapping("/directions")
    public Result<DirectionsResp> directions() {
        return Results.success(questionService.directions());
    }

    @PostMapping("/import")
    public Result<QuestionImportResult> importQuestions(@CurrentUser UserContext user,
                                                       @RequestBody List<QuestionSaveReq> list) {
        requireAdmin(user);
        return Results.success(questionService.importQuestions(list, user.getUserId()));
    }

    private void requireAdmin(UserContext user) {
        if (user == null || !user.isAdmin()) {
            throw new ClientException(BaseErrorCode.FORBIDDEN);
        }
    }
}

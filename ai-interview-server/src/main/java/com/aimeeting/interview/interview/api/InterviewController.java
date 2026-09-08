package com.aimeeting.interview.interview.api;

import com.aimeeting.interview.common.convention.annotation.CurrentUser;
import com.aimeeting.interview.common.convention.context.UserContext;
import com.aimeeting.interview.common.convention.result.PageInfo;
import com.aimeeting.interview.common.convention.result.Result;
import com.aimeeting.interview.common.convention.result.Results;
import com.aimeeting.interview.common.util.MdcUtil;
import com.aimeeting.interview.interview.api.io.req.CreateSessionReq;
import com.aimeeting.interview.interview.api.io.req.SessionPageQuery;
import com.aimeeting.interview.interview.api.io.resp.MessageItemResp;
import com.aimeeting.interview.interview.api.io.resp.QuestionRespStream;
import com.aimeeting.interview.interview.api.io.resp.SessionBriefResp;
import com.aimeeting.interview.interview.api.io.resp.SessionDetailResp;
import com.aimeeting.interview.interview.api.io.resp.SessionStatusResp;
import com.aimeeting.interview.interview.service.InterviewSessionService;
import com.aimeeting.interview.interview.service.SseEmitterManager;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 面试会话接口（/api/interview/sessions），严格对齐前端 {@code src/api/interview.ts}。
 */
@RestController
@RequestMapping("/api/interview/sessions")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewSessionService interviewSessionService;

    private final SseEmitterManager sseEmitterManager;

    /** 创建会话（status=INIT）。 */
    @PostMapping
    public Result<Long> create(@CurrentUser UserContext user, @Valid @RequestBody CreateSessionReq req) {
        return Results.success(interviewSessionService.create(user.getUserId(), req));
    }

    /** 会话分页列表。 */
    @GetMapping
    public Result<PageInfo<SessionBriefResp>> page(@CurrentUser UserContext user, SessionPageQuery query) {
        return Results.success(interviewSessionService.page(user.getUserId(), query));
    }

    /** 会话详情。 */
    @GetMapping("/{id}")
    public Result<SessionDetailResp> detail(@CurrentUser UserContext user, @PathVariable Long id) {
        return Results.success(interviewSessionService.detail(user.getUserId(), id));
    }

    /** 开始面试：INIT → ASKING，阻塞式产出首题。 */
    @PostMapping("/{id}/start")
    public Result<QuestionRespStream> start(@CurrentUser UserContext user, @PathVariable Long id) {
        return Results.success(interviewSessionService.start(user.getUserId(), id));
    }

    /** SSE 取下一题（GET）。 */
    @GetMapping("/{id}/next-question")
    public SseEmitter nextQuestion(@CurrentUser UserContext user, @PathVariable Long id,
                                   @RequestParam(required = false, defaultValue = "0") long lastSeq) {
        SseEmitter emitter = sseEmitterManager.create(MdcUtil.getRequestId(), id);
        interviewSessionService.streamNextQuestion(user.getUserId(), id, emitter);
        return emitter;
    }

    /** 暂停会话。 */
    @PostMapping("/{id}/pause")
    public Result<Void> pause(@CurrentUser UserContext user, @PathVariable Long id) {
        interviewSessionService.pause(user.getUserId(), id);
        return Results.success();
    }

    /** 恢复会话。 */
    @PostMapping("/{id}/resume")
    public Result<Void> resume(@CurrentUser UserContext user, @PathVariable Long id) {
        interviewSessionService.resume(user.getUserId(), id);
        return Results.success();
    }

    /** 结束会话并触发报告生成，返回 reportId。 */
    @PostMapping("/{id}/finish")
    public Result<Long> finish(@CurrentUser UserContext user, @PathVariable Long id) {
        return Results.success(interviewSessionService.finish(user.getUserId(), id));
    }

    /** 删除会话（逻辑删除）。 */
    @DeleteMapping("/{id}")
    public Result<Void> remove(@CurrentUser UserContext user, @PathVariable Long id) {
        interviewSessionService.remove(user.getUserId(), id);
        return Results.success();
    }

    /** 轻量轮询会话状态与进度。 */
    @GetMapping("/{id}/status")
    public Result<SessionStatusResp> status(@CurrentUser UserContext user, @PathVariable Long id) {
        return Results.success(interviewSessionService.status(user.getUserId(), id));
    }

    /** 会话对话流水。 */
    @GetMapping("/{id}/messages")
    public Result<List<MessageItemResp>> messages(@CurrentUser UserContext user, @PathVariable Long id) {
        return Results.success(interviewSessionService.messages(user.getUserId(), id));
    }

    /** 跳过当前题（记 0 分）。 */
    @PostMapping("/{id}/questions/{sessionQuestionId}/skip")
    public Result<Void> skip(@CurrentUser UserContext user, @PathVariable Long id,
                             @PathVariable Long sessionQuestionId) {
        interviewSessionService.skip(user.getUserId(), id, sessionQuestionId);
        return Results.success();
    }
}

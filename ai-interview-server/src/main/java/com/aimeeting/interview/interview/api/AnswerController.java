package com.aimeeting.interview.interview.api;

import com.aimeeting.interview.common.convention.annotation.CurrentUser;
import com.aimeeting.interview.common.convention.context.UserContext;
import com.aimeeting.interview.common.convention.result.Result;
import com.aimeeting.interview.common.convention.result.Results;
import com.aimeeting.interview.common.util.MdcUtil;
import com.aimeeting.interview.interview.api.io.req.FollowUpAnswerReq;
import com.aimeeting.interview.interview.api.io.req.SubmitAnswerReq;
import com.aimeeting.interview.interview.api.io.resp.AnswerDetailResp;
import com.aimeeting.interview.interview.service.AnswerService;
import com.aimeeting.interview.interview.service.InterviewSessionService;
import com.aimeeting.interview.interview.service.SseEmitterManager;
import jakarta.validation.Valid;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 答题接口（/api/interview/.../answers），严格对齐前端 {@code src/api/interview.ts}：
 * <ul>
 *   <li>POST /api/interview/sessions/{id}/answers（SSE 提交原答案）</li>
 *   <li>POST /api/interview/sessions/{id}/answers/follow-up（SSE 提交追问答案）</li>
 *   <li>GET  /api/interview/answers/{answerId}（查询单条答题详情）</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/interview")
@RequiredArgsConstructor
public class AnswerController {

    private final AnswerService answerService;

    private final InterviewSessionService interviewSessionService;

    private final SseEmitterManager sseEmitterManager;

    /** 提交答案（SSE 流式评分）。 */
    @PostMapping("/sessions/{id}/answers")
    public SseEmitter submit(@CurrentUser UserContext user, @PathVariable("id") Long id,
                             @RequestHeader(value = "X-Client-Token", required = false) String headerToken,
                             @Valid @RequestBody SubmitAnswerReq req) {
        interviewSessionService.checkOwner(user.getUserId(), id);
        String clientToken = (headerToken != null && !headerToken.isBlank()) ? headerToken : req.getClientToken();
        SseEmitter emitter = sseEmitterManager.create(MdcUtil.getRequestId(), id);
        CompletableFuture.runAsync(() -> answerService.submit(user.getUserId(), id, req, clientToken, emitter));
        return emitter;
    }

    /** 提交追问答案（SSE 流式评分）。 */
    @PostMapping("/sessions/{id}/answers/follow-up")
    public SseEmitter submitFollowUp(@CurrentUser UserContext user, @PathVariable("id") Long id,
                                     @RequestHeader(value = "X-Client-Token", required = false) String headerToken,
                                     @Valid @RequestBody FollowUpAnswerReq req) {
        interviewSessionService.checkOwner(user.getUserId(), id);
        String clientToken = (headerToken != null && !headerToken.isBlank()) ? headerToken : req.getClientToken();
        SseEmitter emitter = sseEmitterManager.create(MdcUtil.getRequestId(), id);
        CompletableFuture.runAsync(() -> answerService.submitFollowUp(user.getUserId(), id, req, clientToken, emitter));
        return emitter;
    }

    /** 查询单条答题与评分详情（越权抛 B0303）。 */
    @GetMapping("/answers/{answerId}")
    public Result<AnswerDetailResp> answerDetail(@CurrentUser UserContext user, @PathVariable Long answerId) {
        return Results.success(interviewSessionService.answerDetail(user.getUserId(), answerId));
    }
}

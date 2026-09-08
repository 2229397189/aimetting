package com.aimeeting.interview.interview.service;

import com.aimeeting.interview.interview.api.io.req.FollowUpAnswerReq;
import com.aimeeting.interview.interview.api.io.req.SubmitAnswerReq;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 答题服务（SSE + 幂等 + 状态机 + 追问判定）。
 */
public interface AnswerService {

    /**
     * 提交答案（SSE）：幂等 → 落库 → EVALUATING → 流式评分 → 追问判定 → done。
     *
     * @param userId      用户 ID
     * @param sessionId   会话 ID
     * @param req         提交请求
     * @param clientToken 幂等令牌（X-Client-Token）
     * @param emitter     SSE 发射器
     */
    void submit(Long userId, Long sessionId, SubmitAnswerReq req, String clientToken, SseEmitter emitter);

    /**
     * 提交追问答案（SSE）。
     *
     * @param userId      用户 ID
     * @param sessionId   会话 ID
     * @param req         提交请求
     * @param clientToken 幂等令牌
     * @param emitter     SSE 发射器
     */
    void submitFollowUp(Long userId, Long sessionId, FollowUpAnswerReq req, String clientToken, SseEmitter emitter);
}

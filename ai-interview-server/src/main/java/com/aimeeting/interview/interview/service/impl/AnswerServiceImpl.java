package com.aimeeting.interview.interview.service.impl;

import com.aimeeting.interview.interview.api.io.req.FollowUpAnswerReq;
import com.aimeeting.interview.interview.api.io.req.SubmitAnswerReq;
import com.aimeeting.interview.interview.service.AnswerService;
import com.aimeeting.interview.interview.service.InterviewWorkflowEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 答题服务实现（M5）。
 *
 * <p>编排逻辑（幂等 → 落库 → EVALUATING → 流式评分 → 追问判定 → done）已于 M2 抽离至
 * {@link InterviewWorkflowEngine}，本类仅保留「请求对象 → 引擎入参」的适配职责，行为不变。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnswerServiceImpl implements AnswerService {

    private final InterviewWorkflowEngine interviewWorkflowEngine;

    @Override
    public void submit(Long userId, Long sessionId, SubmitAnswerReq req, String clientToken, SseEmitter emitter) {
        interviewWorkflowEngine.submit(userId, sessionId, req.getSessionQuestionId(), req.getContent(),
                req.getParentAnswerId(), Boolean.TRUE.equals(req.getIsFollowUp()), clientToken, emitter);
    }

    @Override
    public void submitFollowUp(Long userId, Long sessionId, FollowUpAnswerReq req,
                               String clientToken, SseEmitter emitter) {
        interviewWorkflowEngine.submit(userId, sessionId, req.getSessionQuestionId(), req.getContent(),
                req.getParentAnswerId(), true, clientToken, emitter);
    }
}

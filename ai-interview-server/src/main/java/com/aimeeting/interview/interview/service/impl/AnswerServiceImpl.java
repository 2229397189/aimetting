package com.aimeeting.interview.interview.service.impl;

import com.aimeeting.interview.common.convention.errorcode.BaseErrorCode;
import com.aimeeting.interview.common.convention.exception.AbstractException;
import com.aimeeting.interview.common.convention.exception.ServiceException;
import com.aimeeting.interview.common.idempotent.IdempotencyService;
import com.aimeeting.interview.common.idempotent.IdempotentStage;
import com.aimeeting.interview.common.idempotent.TryStartResult;
import com.aimeeting.interview.common.util.JsonUtil;
import com.aimeeting.interview.common.util.MdcUtil;
import com.aimeeting.interview.config.InterviewProperties;
import com.aimeeting.interview.interview.api.io.req.FollowUpAnswerReq;
import com.aimeeting.interview.interview.api.io.req.SubmitAnswerReq;
import com.aimeeting.interview.interview.api.io.resp.AnswerDetailResp;
import com.aimeeting.interview.interview.dao.entity.InterviewSessionDO;
import com.aimeeting.interview.interview.dao.entity.SessionAnswerDO;
import com.aimeeting.interview.interview.dao.entity.SessionQuestionDO;
import com.aimeeting.interview.interview.dao.mapper.InterviewSessionMapper;
import com.aimeeting.interview.interview.dao.mapper.SessionAnswerMapper;
import com.aimeeting.interview.interview.dao.mapper.SessionQuestionMapper;
import com.aimeeting.interview.interview.domain.model.FollowUpLimitPolicy;
import com.aimeeting.interview.interview.domain.model.ScorePolicy;
import com.aimeeting.interview.interview.domain.state.InterviewSessionStateMachine;
import com.aimeeting.interview.interview.domain.state.SessionStatus;
import com.aimeeting.interview.interview.service.AnswerService;
import com.aimeeting.interview.interview.service.AiStreamSink;
import com.aimeeting.interview.interview.service.EvaluationService;
import com.aimeeting.interview.interview.service.SseEmitterManager;
import com.aimeeting.interview.interview.service.model.AnswerReplay;
import com.aimeeting.interview.interview.service.model.EvaluationContext;
import com.aimeeting.interview.interview.service.model.EvaluationResult;
import com.aimeeting.interview.interview.service.sse.CommentPayload;
import com.aimeeting.interview.interview.service.sse.DonePayload;
import com.aimeeting.interview.interview.service.sse.FollowUpPayload;
import com.aimeeting.interview.interview.service.sse.ProgressPayload;
import com.aimeeting.interview.interview.service.sse.ScorePayload;
import com.aimeeting.interview.interview.service.sse.SseEnvelope;
import com.aimeeting.interview.interview.service.sse.SseEventType;
import com.fasterxml.jackson.core.type.TypeReference;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 答题服务实现：幂等 → 落库 → EVALUATING → 流式评分 → 追问判定 → done（§3.5 / §5 / §6）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnswerServiceImpl implements AnswerService {

    private final InterviewSessionMapper sessionMapper;

    private final SessionQuestionMapper questionMapper;

    private final SessionAnswerMapper answerMapper;

    private final EvaluationService evaluationService;

    private final SseEmitterManager sseEmitterManager;

    private final IdempotencyService idempotencyService;

    private final InterviewProperties interviewProperties;

    @Override
    public void submit(Long userId, Long sessionId, SubmitAnswerReq req, String clientToken, SseEmitter emitter) {
        doSubmit(userId, sessionId, req.getSessionQuestionId(), req.getContent(),
                req.getParentAnswerId(), Boolean.TRUE.equals(req.getIsFollowUp()), clientToken, emitter);
    }

    @Override
    public void submitFollowUp(Long userId, Long sessionId, FollowUpAnswerReq req,
                               String clientToken, SseEmitter emitter) {
        doSubmit(userId, sessionId, req.getSessionQuestionId(), req.getContent(),
                req.getParentAnswerId(), true, clientToken, emitter);
    }

    private void doSubmit(Long userId, Long sessionId, Long sessionQuestionId, String content,
                          Long parentAnswerId, boolean isFollowUp, String clientToken, SseEmitter emitter) {
        String requestId = MdcUtil.getRequestId();
        AtomicLong seq = new AtomicLong(0);
        try {
            InterviewSessionDO session = loadAndCheckOwner(userId, sessionId);
            SessionQuestionDO q = questionMapper.selectById(sessionQuestionId);
            if (q == null || !q.getSessionId().equals(sessionId)) {
                throw new ServiceException("题目不存在或不属于该会话", BaseErrorCode.PARAM_ERROR);
            }

            String bizId = sessionId + "-" + sessionQuestionId + (isFollowUp ? "-fu-" + parentAnswerId : "");
            TryStartResult<AnswerReplay> idem = idempotencyService.tryStart(
                    IdempotentStage.ANSWER_SUBMIT, userId, bizId, clientToken,
                    new TypeReference<AnswerReplay>() {});
            if (idem.getStatus() == TryStartResult.Status.SUCCEEDED) {
                replay(seq, emitter, idem.getReplay(), requestId, sessionId);
                sseEmitterManager.complete(emitter);
                return;
            }
            if (idem.getStatus() == TryStartResult.Status.PROCESSING) {
                sseEmitterManager.error(emitter, BaseErrorCode.RATE_LIMITED.code(),
                        "正在处理中，请勿重复提交", requestId, sessionId);
                sseEmitterManager.complete(emitter);
                return;
            }

            SessionStatus cur = SessionStatus.valueOf(session.getStatus());
            if (cur != SessionStatus.ASKING && cur != SessionStatus.FOLLOW_UP) {
                throw new ServiceException("当前状态不允许提交答案", BaseErrorCode.ILLEGAL_STATUS_TRANSITION);
            }
            InterviewSessionStateMachine.check(cur, SessionStatus.EVALUATING);
            session.setStatus(SessionStatus.EVALUATING.name());
            sessionMapper.updateById(session);

            SessionAnswerDO ans = new SessionAnswerDO();
            ans.setSessionId(sessionId);
            ans.setSessionQuestionId(sessionQuestionId);
            ans.setUserId(userId);
            ans.setContent(content);
            ans.setIsFollowUp(isFollowUp ? 1 : 0);
            ans.setParentAnswerId(parentAnswerId);
            ans.setScore(null);
            ans.setEvaluatedBy(null);
            ans.setSkipped(0);
            ans.setFollowUpCount(0);
            ans.setClientToken(clientToken);
            answerMapper.insert(ans);
            Long answerId = ans.getId();

            sseEmitterManager.send(emitter, buildProgress(seq, "AI 正在评估你的回答…", requestId, sessionId, q.getQuestionNo()));

            EvaluationContext ctx = EvaluationContext.builder()
                    .sessionId(sessionId)
                    .sessionQuestionId(sessionQuestionId)
                    .questionNo(q.getQuestionNo())
                    .questionTitle(q.getTitle())
                    .referencePoints(parseList(q.getReferencePoints()))
                    .answer(content)
                    .isFollowUp(isFollowUp)
                    .phase(q.getPhase())
                    .build();

            StringBuilder commentBuf = new StringBuilder();
            AiStreamSink sink = new AiStreamSink() {
                @Override
                public void acceptDelta(String delta) {
                    commentBuf.append(delta);
                    sseEmitterManager.send(emitter, buildComment(seq, answerId, delta, false,
                            requestId, sessionId, q.getQuestionNo()));
                }

                @Override
                public void acceptFinish() {
                    sseEmitterManager.send(emitter, buildComment(seq, answerId, "", true,
                            requestId, sessionId, q.getQuestionNo()));
                }
            };
            EvaluationResult result = evaluationService.evaluate(userId, ctx, sink);

            ans.setScore(result.getScore());
            ans.setComment(result.getComment());
            ans.setHighlights(toJson(result.getHighlights()));
            ans.setGaps(toJson(result.getGaps()));
            ans.setImprovedAnswer(result.getImprovedAnswer());
            ans.setFollowUpQuestion(result.getFollowUpQuestion());
            ans.setEvaluatedBy(result.getEvaluatedBy() == null ? null : result.getEvaluatedBy().name());
            answerMapper.updateById(ans);

            int parentFollowUpCount = 0;
            if (isFollowUp && parentAnswerId != null) {
                SessionAnswerDO parent = answerMapper.selectById(parentAnswerId);
                if (parent != null) {
                    parentFollowUpCount = parent.getFollowUpCount() == null ? 1 : parent.getFollowUpCount() + 1;
                    parent.setFollowUpCount(parentFollowUpCount);
                    answerMapper.updateById(parent);
                }
            }

            int maxFollowUp = interviewProperties.getMaxFollowUp();
            boolean needFollowUp = Boolean.TRUE.equals(result.getNeedFollowUp());
            int effectiveCount = isFollowUp ? parentFollowUpCount : 0;
            String nextAction;
            SessionStatus nextStatus;
            if (needFollowUp && FollowUpLimitPolicy.canFollowUp(effectiveCount, maxFollowUp)) {
                nextAction = "FOLLOW_UP";
                nextStatus = SessionStatus.FOLLOW_UP;
            } else if (session.getCurrentIndex() < session.getTotalQuestion()) {
                nextAction = "NEXT_QUESTION";
                nextStatus = SessionStatus.ASKING;
            } else {
                nextAction = "COMPLETED";
                nextStatus = SessionStatus.COMPLETED;
            }
            InterviewSessionStateMachine.check(SessionStatus.EVALUATING, nextStatus);
            session.setStatus(nextStatus.name());
            if (nextStatus == SessionStatus.COMPLETED) {
                session.setFinishedAt(LocalDateTime.now());
                session.setScore(BigDecimal.valueOf(computeTotalScore(sessionId)));
            }
            sessionMapper.updateById(session);

            sseEmitterManager.send(emitter, buildScore(seq, answerId, result, requestId, sessionId, q.getQuestionNo()));

            int emitFollowUpCount = isFollowUp ? parentFollowUpCount : 1;
            if ("FOLLOW_UP".equals(nextAction)) {
                sseEmitterManager.send(emitter, buildFollowUp(seq, answerId, parentAnswerId, sessionQuestionId,
                        result.getFollowUpQuestion(), emitFollowUpCount, maxFollowUp, requestId, sessionId, q.getQuestionNo()));
            }

            AnswerReplay replay = AnswerReplay.builder()
                    .answerId(answerId)
                    .sessionQuestionId(sessionQuestionId)
                    .questionNo(q.getQuestionNo())
                    .score(result.getScore())
                    .comment(result.getComment())
                    .highlights(result.getHighlights())
                    .gaps(result.getGaps())
                    .improvedAnswer(result.getImprovedAnswer())
                    .evaluatedBy(ans.getEvaluatedBy())
                    .degraded(result.isDegraded())
                    .nextAction(nextAction)
                    .status(nextStatus.name())
                    .currentIndex(session.getCurrentIndex())
                    .totalQuestion(session.getTotalQuestion())
                    .reportId(null)
                    .followUpQuestion(result.getFollowUpQuestion())
                    .followUpCount(emitFollowUpCount)
                    .maxFollowUp(maxFollowUp)
                    .build();
            idempotencyService.markSuccess(IdempotentStage.ANSWER_SUBMIT, userId, bizId, clientToken, replay);

            sseEmitterManager.send(emitter, buildDone(seq, nextAction, nextStatus, session, answerId,
                    result.getScore(), requestId, sessionId, false));
            sseEmitterManager.complete(emitter);
        } catch (Throwable t) {
            log.warn("[Answer] 提交答案失败, sessionId={}, sessionQuestionId={}, err={}",
                    sessionId, sessionQuestionId, t.getMessage());
            sseEmitterManager.error(emitter, errorCode(t), "评分失败：" + t.getMessage(), requestId, sessionId);
            sseEmitterManager.complete(emitter);
            try {
                String bizId = sessionId + "-" + sessionQuestionId + (isFollowUp ? "-fu-" + parentAnswerId : "");
                idempotencyService.clear(IdempotentStage.ANSWER_SUBMIT, userId, bizId, clientToken);
            } catch (Throwable ignore) {
                // 释放处理中键失败时忽略，允许客户端重试
            }
        }
    }

    private void replay(AtomicLong seq, SseEmitter emitter, AnswerReplay r, String requestId, Long sessionId) {
        if (r.getComment() != null && !r.getComment().isBlank()) {
            sseEmitterManager.send(emitter, envelope(SseEventType.comment, seq.incrementAndGet(), requestId,
                    sessionId, r.getQuestionNo(), false, new CommentPayload(r.getAnswerId(), r.getComment(), true, null)));
        }
        if (r.getScore() != null) {
            sseEmitterManager.send(emitter, envelope(SseEventType.score, seq.incrementAndGet(), requestId,
                    sessionId, r.getQuestionNo(), r.isDegraded(), new ScorePayload(r.getAnswerId(), r.getScore(),
                    r.getEvaluatedBy(), r.getHighlights(), r.getGaps(), r.getImprovedAnswer(), r.isDegraded())));
        }
        if ("FOLLOW_UP".equals(r.getNextAction())) {
            sseEmitterManager.send(emitter, envelope(SseEventType.follow_up, seq.incrementAndGet(), requestId,
                    sessionId, r.getQuestionNo(), r.isDegraded(), new FollowUpPayload(r.getAnswerId(), null,
                    r.getSessionQuestionId(), r.getFollowUpQuestion(), r.getFollowUpCount(), r.getMaxFollowUp())));
        }
        sseEmitterManager.send(emitter, envelope(SseEventType.done, seq.incrementAndGet(), requestId,
                sessionId, r.getQuestionNo(), r.isDegraded(), new DonePayload(r.getNextAction(), r.getStatus(),
                r.getCurrentIndex(), r.getTotalQuestion(), r.getAnswerId(), r.getScore(), r.getReportId(), true)));
    }

    /* ------------------------------ 事件构造 ------------------------------ */

    private SseEnvelope envelope(SseEventType type, long seq, String requestId, Long sessionId,
                                 Integer questionNo, boolean degraded, Object payload) {
        return SseEnvelope.of(type, seq, requestId, sessionId, questionNo, degraded, payload);
    }

    private SseEnvelope buildProgress(AtomicLong seq, String text, String requestId, Long sessionId, Integer questionNo) {
        return envelope(SseEventType.progress, seq.incrementAndGet(), requestId, sessionId, questionNo, false,
                new ProgressPayload(text));
    }

    private SseEnvelope buildComment(AtomicLong seq, Long answerId, String delta, boolean finish,
                                     String requestId, Long sessionId, Integer questionNo) {
        return envelope(SseEventType.comment, seq.incrementAndGet(), requestId, sessionId, questionNo, false,
                new CommentPayload(answerId, delta, finish, null));
    }

    private SseEnvelope buildScore(AtomicLong seq, Long answerId, EvaluationResult r,
                                   String requestId, Long sessionId, Integer questionNo) {
        return envelope(SseEventType.score, seq.incrementAndGet(), requestId, sessionId, questionNo, r.isDegraded(),
                new ScorePayload(answerId, r.getScore(), r.getEvaluatedBy() == null ? null : r.getEvaluatedBy().name(),
                        r.getHighlights(), r.getGaps(), r.getImprovedAnswer(), r.isDegraded()));
    }

    private SseEnvelope buildFollowUp(AtomicLong seq, Long answerId, Long parentAnswerId, Long sessionQuestionId,
                                     String title, int followUpCount, int maxFollowUp, String requestId,
                                     Long sessionId, Integer questionNo) {
        return envelope(SseEventType.follow_up, seq.incrementAndGet(), requestId, sessionId, questionNo, false,
                new FollowUpPayload(answerId, parentAnswerId, sessionQuestionId, title, followUpCount, maxFollowUp));
    }

    private SseEnvelope buildDone(AtomicLong seq, String nextAction, SessionStatus status, InterviewSessionDO session,
                                  Long answerId, Integer score, String requestId, Long sessionId, boolean replayed) {
        return envelope(SseEventType.done, seq.incrementAndGet(), requestId, sessionId, session.getCurrentIndex(),
                false, new DonePayload(nextAction, status.name(), session.getCurrentIndex(),
                session.getTotalQuestion(), answerId, score, null, replayed));
    }

    /* ------------------------------ 共用工具 ------------------------------ */

    private InterviewSessionDO loadAndCheckOwner(Long userId, Long sessionId) {
        InterviewSessionDO session = sessionMapper.selectById(sessionId);
        if (session == null || (session.getDeleted() != null && session.getDeleted() == 1)) {
            throw new ServiceException("会话不存在", BaseErrorCode.PARAM_ERROR);
        }
        if (!session.getUserId().equals(userId)) {
            throw new ServiceException("无权访问该资源", BaseErrorCode.OWNERSHIP_DENIED);
        }
        return session;
    }

    private double computeTotalScore(Long sessionId) {
        List<SessionQuestionDO> questions = questionMapper.selectBySession(sessionId);
        List<Integer> questionScores = new ArrayList<>();
        for (SessionQuestionDO q : questions) {
            List<SessionAnswerDO> answers = answerMapper.selectByQuestion(sessionId, q.getId());
            SessionAnswerDO original = answers.stream().filter(a -> a.getParentAnswerId() == null).findFirst().orElse(null);
            if (original == null || original.getScore() == null) {
                questionScores.add(0);
                continue;
            }
            List<Integer> followUps = answers.stream()
                    .filter(a -> a.getParentAnswerId() != null && a.getScore() != null)
                    .map(SessionAnswerDO::getScore).collect(java.util.stream.Collectors.toList());
            questionScores.add(ScorePolicy.questionScore(original.getScore(), followUps));
        }
        return ScorePolicy.totalScore(questionScores);
    }

    private List<String> parseList(String json) {
        List<String> list = JsonUtil.parse(json, new TypeReference<List<String>>() {});
        return list == null ? new ArrayList<>() : list;
    }

    private String toJson(List<String> list) {
        return list == null ? null : JsonUtil.toJson(list);
    }

    private String errorCode(Throwable t) {
        if (t instanceof AbstractException ae) {
            return ae.getErrorCode();
        }
        return BaseErrorCode.SERVICE_ERROR.code();
    }
}

package com.aimeeting.interview.interview.service;

import com.aimeeting.interview.common.convention.errorcode.BaseErrorCode;
import com.aimeeting.interview.common.convention.exception.ClientException;
import com.aimeeting.interview.common.convention.exception.ServiceException;
import com.aimeeting.interview.common.convention.result.PageInfo;
import com.aimeeting.interview.common.util.JsonUtil;
import com.aimeeting.interview.common.util.SessionNoGenerator;
import com.aimeeting.interview.config.InterviewProperties;
import com.aimeeting.interview.interview.api.io.req.CreateSessionReq;
import com.aimeeting.interview.interview.api.io.req.SessionPageQuery;
import com.aimeeting.interview.interview.api.io.resp.MessageItemResp;
import com.aimeeting.interview.interview.api.io.resp.QuestionRespStream;
import com.aimeeting.interview.interview.api.io.resp.SessionAnswerItemResp;
import com.aimeeting.interview.interview.api.io.resp.SessionBriefResp;
import com.aimeeting.interview.interview.api.io.resp.SessionDetailResp;
import com.aimeeting.interview.interview.api.io.resp.SessionQuestionItemResp;
import com.aimeeting.interview.interview.api.io.resp.AnswerDetailResp;
import com.aimeeting.interview.interview.api.io.resp.SessionStatusResp;
import com.aimeeting.interview.interview.dao.entity.InterviewSessionDO;
import com.aimeeting.interview.interview.dao.entity.SessionAnswerDO;
import com.aimeeting.interview.interview.dao.entity.SessionQuestionDO;
import com.aimeeting.interview.interview.dao.mapper.InterviewSessionMapper;
import com.aimeeting.interview.interview.dao.mapper.SessionAnswerMapper;
import com.aimeeting.interview.interview.dao.mapper.SessionQuestionMapper;
import com.aimeeting.interview.interview.domain.model.InterviewPhase;
import com.aimeeting.interview.interview.domain.model.PhasePlanner;
import com.aimeeting.interview.interview.domain.model.ScorePolicy;
import com.aimeeting.interview.interview.domain.state.InterviewSessionStateMachine;
import com.aimeeting.interview.interview.domain.state.SessionStatus;
import com.aimeeting.interview.interview.service.model.GeneratedQuestion;
import com.aimeeting.interview.question.domain.Direction;
import com.aimeeting.interview.report.service.ReportService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 面试会话服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewSessionServiceImpl implements InterviewSessionService {

    private final InterviewSessionMapper sessionMapper;
    private final SessionQuestionMapper questionMapper;
    private final SessionAnswerMapper answerMapper;
    private final QuestionGenerationService questionGenerationService;
    private final InterviewProperties interviewProperties;
    private final SseEmitterManager sseEmitterManager;
    private final ReportService reportService;

    @Override
    public Long create(Long userId, CreateSessionReq req) {
        if (req.getDirections() == null || req.getDirections().isEmpty()) {
            throw new ClientException("方向不能为空", BaseErrorCode.PARAM_ERROR);
        }
        for (String d : req.getDirections()) {
            if (!Direction.isValid(d)) {
                throw new ClientException("方向不合法: " + d, BaseErrorCode.PARAM_ERROR);
            }
        }
        int total = req.getTotalQuestion() == null ? interviewProperties.getDefaultQuestionCount()
                : req.getTotalQuestion();
        int min = interviewProperties.getMinQuestionCount();
        int max = interviewProperties.getMaxQuestionCount();
        if (total < min || total > max) {
            throw new ClientException("题量需在 " + min + "~" + max + " 之间", BaseErrorCode.PARAM_ERROR);
        }

        InterviewSessionDO session = new InterviewSessionDO();
        session.setUserId(userId);
        session.setSessionNo(SessionNoGenerator.generate());
        session.setDirections(String.join(",", req.getDirections()));
        session.setDifficulty(req.getDifficulty());
        session.setTotalQuestion(total);
        session.setCurrentIndex(0);
        session.setStatus(SessionStatus.INIT.name());
        session.setPrevStatus(null);
        session.setJdText(req.getJdText());
        Map<String, Integer> plan = req.getPhasePlan() != null && !req.getPhasePlan().isEmpty()
                ? req.getPhasePlan() : PhasePlanner.planToStringMap(total);
        session.setPhasePlan(JsonUtil.toJson(plan));
        session.setStartedAt(null);
        session.setFinishedAt(null);
        sessionMapper.insert(session);
        return session.getId();
    }

    @Override
    public PageInfo<SessionBriefResp> page(Long userId, SessionPageQuery query) {
        List<InterviewSessionDO> all = sessionMapper.selectByUser(userId, query.getStatus(), query.getDirection());
        long total = all.size();
        long pn = query.safePageNum();
        long ps = query.safePageSize();
        long from = (pn - 1) * ps;
        long to = Math.min(from + ps, total);
        List<SessionBriefResp> list = new ArrayList<>();
        for (long i = from; i < to; i++) {
            list.add(toBrief(all.get((int) i)));
        }
        PageInfo<SessionBriefResp> pageInfo = new PageInfo<>();
        pageInfo.setList(list);
        pageInfo.setTotal(total);
        pageInfo.setPageNum(pn);
        pageInfo.setPageSize(ps);
        return pageInfo;
    }

    @Override
    public SessionDetailResp detail(Long userId, Long sessionId) {
        InterviewSessionDO session = loadAndCheckOwner(userId, sessionId);
        SessionDetailResp resp = toDetail(session);
        List<SessionQuestionDO> questions = questionMapper.selectBySession(sessionId);
        Map<Long, List<SessionAnswerDO>> answersByQ = answerMapper.selectBySession(sessionId).stream()
                .collect(Collectors.groupingBy(SessionAnswerDO::getSessionQuestionId));
        List<SessionQuestionItemResp> items = new ArrayList<>();
        for (SessionQuestionDO q : questions) {
            SessionQuestionItemResp item = new SessionQuestionItemResp();
            item.setSessionQuestionId(q.getId());
            item.setQuestionNo(q.getQuestionNo());
            item.setQuestionId(q.getQuestionId());
            item.setTitle(q.getTitle());
            item.setReferencePoints(parseList(q.getReferencePoints()));
            item.setDifficulty(q.getDifficulty());
            item.setSource(q.getSource());
            item.setPhase(q.getPhase());
            List<SessionAnswerDO> ans = answersByQ.getOrDefault(q.getId(), new ArrayList<>());
            SessionAnswerDO original = ans.stream().filter(a -> a.getParentAnswerId() == null).findFirst().orElse(null);
            if (original != null) {
                item.setAnswer(toAnswerItem(original));
                item.setFollowUps(ans.stream().filter(a -> a.getParentAnswerId() != null)
                        .map(this::toAnswerItem).collect(Collectors.toList()));
            }
            items.add(item);
        }
        resp.setQuestions(items);
        return resp;
    }

    @Override
    public QuestionRespStream start(Long userId, Long sessionId) {
        InterviewSessionDO session = loadAndCheckOwner(userId, sessionId);
        InterviewSessionStateMachine.check(SessionStatus.valueOf(session.getStatus()), SessionStatus.ASKING);
        List<String> directions = parseDirections(session.getDirections());
        String firstDirection = directions.get(0);
        GeneratedQuestion g = questionGenerationService.generate(userId, sessionId, firstDirection,
                session.getDifficulty(), 1, session.getJdText(), new java.util.HashSet<>());
        SessionQuestionDO q = new SessionQuestionDO();
        q.setSessionId(sessionId);
        q.setQuestionNo(1);
        q.setQuestionId(g.title() != null && g.title().startsWith("BANK#") ? null : null);
        q.setTitle(g.title());
        q.setReferencePoints(JsonUtil.toJson(g.referencePoints()));
        q.setSource(g.source());
        q.setDifficulty(g.difficulty());
        q.setPhase(PhasePlanner.phaseOf(1, session.getTotalQuestion()).name());
        q.setSkipped(0);
        questionMapper.insert(q);

        session.setStatus(SessionStatus.ASKING.name());
        session.setPrevStatus(SessionStatus.INIT.name());
        session.setCurrentIndex(1);
        session.setStartedAt(LocalDateTime.now());
        sessionMapper.updateById(session);

        return toQuestionStream(q, g, session.getTotalQuestion());
    }

    @Async
    @Override
    public void streamNextQuestion(Long userId, Long sessionId, SseEmitter emitter) {
        String requestId = com.aimeeting.interview.common.util.MdcUtil.getRequestId();
        try {
            InterviewSessionDO session = loadAndCheckOwner(userId, sessionId);
            SessionStatus cur = SessionStatus.valueOf(session.getStatus());
            if (cur != SessionStatus.ASKING && cur != SessionStatus.FOLLOW_UP) {
                sseEmitterManager.error(emitter, BaseErrorCode.ILLEGAL_STATUS_TRANSITION.code(),
                        "当前状态不允许获取下一题", requestId, sessionId);
                return;
            }
            int nextNo = session.getCurrentIndex() + 1;
            if (nextNo > session.getTotalQuestion()) {
                sseEmitterManager.error(emitter, BaseErrorCode.ILLEGAL_STATUS_TRANSITION.code(),
                        "已是最后一题", requestId, sessionId);
                return;
            }
            List<String> directions = parseDirections(session.getDirections());
            String direction = directions.get((nextNo - 1) % directions.size());
            GeneratedQuestion g = questionGenerationService.generate(userId, sessionId, direction,
                    session.getDifficulty(), nextNo, session.getJdText(), new java.util.HashSet<>());
            SessionQuestionDO q = new SessionQuestionDO();
            q.setSessionId(sessionId);
            q.setQuestionNo(nextNo);
            q.setTitle(g.title());
            q.setReferencePoints(JsonUtil.toJson(g.referencePoints()));
            q.setSource(g.source());
            q.setDifficulty(g.difficulty());
            q.setPhase(PhasePlanner.phaseOf(nextNo, session.getTotalQuestion()).name());
            q.setSkipped(0);
            questionMapper.insert(q);

            session.setCurrentIndex(nextNo);
            sessionMapper.updateById(session);

            sseEmitterManager.send(emitter, com.aimeeting.interview.interview.service.sse.SseEnvelope.of(
                    com.aimeeting.interview.interview.service.sse.SseEventType.question, 1L, requestId, sessionId, nextNo,
                    g.degraded(), toQuestionStream(q, g, session.getTotalQuestion())));
            sseEmitterManager.send(emitter, com.aimeeting.interview.interview.service.sse.SseEnvelope.of(
                    com.aimeeting.interview.interview.service.sse.SseEventType.done, 2L, requestId, sessionId, nextNo,
                    false, new com.aimeeting.interview.interview.service.sse.DonePayload(
                            "NEXT_QUESTION", SessionStatus.ASKING.name(), nextNo, session.getTotalQuestion(), null, null, null, false)));
            sseEmitterManager.complete(emitter);
        } catch (Throwable t) {
            log.warn("[InterviewSession] 出下一题失败: {}", t.getMessage());
            sseEmitterManager.error(emitter, BaseErrorCode.SERVICE_ERROR.code(), "出下一题失败：" + t.getMessage(),
                    requestId, sessionId);
        }
    }

    @Override
    public void pause(Long userId, Long sessionId) {
        InterviewSessionDO session = loadAndCheckOwner(userId, sessionId);
        SessionStatus cur = SessionStatus.valueOf(session.getStatus());
        InterviewSessionStateMachine.check(cur, SessionStatus.PAUSED);
        session.setPrevStatus(cur.name());
        session.setStatus(SessionStatus.PAUSED.name());
        sessionMapper.updateById(session);
    }

    @Override
    public void resume(Long userId, Long sessionId) {
        InterviewSessionDO session = loadAndCheckOwner(userId, sessionId);
        SessionStatus cur = SessionStatus.valueOf(session.getStatus());
        if (cur != SessionStatus.PAUSED) {
            throw new ServiceException("仅暂停状态可恢复", BaseErrorCode.ILLEGAL_STATUS_TRANSITION);
        }
        SessionStatus prev = session.getPrevStatus() == null ? SessionStatus.ASKING
                : SessionStatus.valueOf(session.getPrevStatus());
        InterviewSessionStateMachine.check(cur, prev);
        session.setStatus(prev.name());
        sessionMapper.updateById(session);
    }

    @Override
    public Long finish(Long userId, Long sessionId) {
        InterviewSessionDO session = loadAndCheckOwner(userId, sessionId);
        SessionStatus cur = SessionStatus.valueOf(session.getStatus());
        InterviewSessionStateMachine.check(cur, SessionStatus.COMPLETED);
        session.setStatus(SessionStatus.COMPLETED.name());
        session.setFinishedAt(LocalDateTime.now());
        session.setScore(BigDecimal.valueOf(computeTotalScore(sessionId)));
        sessionMapper.updateById(session);
        // 结束会话即生成报告（幂等），失败不阻塞 finish
        try {
            return reportService.generate(userId, sessionId);
        } catch (Exception e) {
            log.warn("[Session] finish 报告生成失败，仍返回 null: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public void remove(Long userId, Long sessionId) {
        InterviewSessionDO session = loadAndCheckOwner(userId, sessionId);
        session.setDeleted(1);
        sessionMapper.updateById(session);
    }

    @Override
    public SessionStatusResp status(Long userId, Long sessionId) {
        InterviewSessionDO session = loadAndCheckOwner(userId, sessionId);
        SessionStatusResp resp = new SessionStatusResp();
        resp.setId(session.getId());
        resp.setStatus(session.getStatus());
        resp.setCurrentIndex(session.getCurrentIndex());
        resp.setTotalQuestion(session.getTotalQuestion());
        resp.setScore(session.getScore());
        resp.setReportId(null);
        return resp;
    }

    @Override
    public List<MessageItemResp> messages(Long userId, Long sessionId) {
        loadAndCheckOwner(userId, sessionId);
        List<SessionQuestionDO> questions = questionMapper.selectBySession(sessionId);
        List<SessionAnswerDO> answers = answerMapper.selectBySession(sessionId);
        Map<Long, SessionAnswerDO> originalByQ = answers.stream()
                .filter(a -> a.getParentAnswerId() == null)
                .collect(Collectors.toMap(SessionAnswerDO::getSessionQuestionId, a -> a, (a, b) -> a));
        List<MessageItemResp> list = new ArrayList<>();
        for (SessionQuestionDO q : questions) {
            MessageItemResp qMsg = new MessageItemResp();
            qMsg.setId(q.getId());
            qMsg.setRole("INTERVIEWER");
            qMsg.setContent(q.getTitle());
            qMsg.setQuestionNo(q.getQuestionNo());
            qMsg.setCreatedAt(q.getCreateTime());
            list.add(qMsg);
            SessionAnswerDO ans = originalByQ.get(q.getId());
            if (ans != null) {
                MessageItemResp aMsg = new MessageItemResp();
                aMsg.setId(ans.getId());
                aMsg.setRole("CANDIDATE");
                aMsg.setContent(ans.getContent());
                aMsg.setQuestionNo(q.getQuestionNo());
                aMsg.setScore(ans.getScore());
                // 透出 AI 评分明细：highlights/gaps/improvedAnswer 取库表字段，followUpQuestion 解析 comment 兜底
                aMsg.setHighlights(parseList(ans.getHighlights()));
                aMsg.setGaps(parseList(ans.getGaps()));
                aMsg.setImprovedAnswer(ans.getImprovedAnswer());
                aMsg.setFollowUpQuestion(parseFollowUpQuestion(ans.getComment()));
                aMsg.setCreatedAt(ans.getCreateTime());
                list.add(aMsg);
            }
        }
        return list;
    }

    @Override
    public void skip(Long userId, Long sessionId, Long sessionQuestionId) {
        InterviewSessionDO session = loadAndCheckOwner(userId, sessionId);
        SessionStatus cur = SessionStatus.valueOf(session.getStatus());
        if (cur != SessionStatus.ASKING && cur != SessionStatus.FOLLOW_UP) {
            throw new ServiceException("当前状态不允许跳过", BaseErrorCode.ILLEGAL_STATUS_TRANSITION);
        }
        SessionQuestionDO q = questionMapper.selectById(sessionQuestionId);
        if (q == null || !q.getSessionId().equals(sessionId)) {
            throw new ServiceException("题目不存在或不属于该会话", BaseErrorCode.PARAM_ERROR);
        }
        SessionAnswerDO ans = new SessionAnswerDO();
        ans.setSessionId(sessionId);
        ans.setSessionQuestionId(sessionQuestionId);
        ans.setUserId(userId);
        ans.setContent("(已跳过)");
        ans.setIsFollowUp(0);
        ans.setParentAnswerId(null);
        ans.setScore(0);
        ans.setEvaluatedBy("RULE");
        ans.setSkipped(1);
        ans.setFollowUpCount(0);
        ans.setClientToken(null);
        answerMapper.insert(ans);
    }

    @Override
    public AnswerDetailResp answerDetail(Long userId, Long answerId) {
        SessionAnswerDO ans = answerMapper.selectById(answerId);
        if (ans == null || (ans.getDeleted() != null && ans.getDeleted() == 1)) {
            throw new ServiceException("答题不存在", BaseErrorCode.PARAM_ERROR);
        }
        InterviewSessionDO session = loadAndCheckOwner(userId, ans.getSessionId());
        AnswerDetailResp resp = new AnswerDetailResp();
        resp.setAnswerId(ans.getId());
        resp.setSessionQuestionId(ans.getSessionQuestionId());
        resp.setContent(ans.getContent());
        resp.setScore(ans.getScore());
        resp.setComment(ans.getComment());
        resp.setHighlights(parseList(ans.getHighlights()));
        resp.setGaps(parseList(ans.getGaps()));
        resp.setImprovedAnswer(ans.getImprovedAnswer());
        resp.setEvaluatedBy(ans.getEvaluatedBy());
        resp.setDegraded("RULE".equals(ans.getEvaluatedBy()));
        return resp;
    }

    /* ------------------------------ 供 AnswerService 复用 ------------------------------ */

    /** 加载会话并校验归属（越权抛 B0303）。 */
    public InterviewSessionDO loadAndCheckOwner(Long userId, Long sessionId) {
        InterviewSessionDO session = sessionMapper.selectById(sessionId);
        if (session == null || session.getDeleted() != null && session.getDeleted() == 1) {
            throw new ServiceException("会话不存在", BaseErrorCode.PARAM_ERROR);
        }
        if (!session.getUserId().equals(userId)) {
            throw new ServiceException("无权访问该资源", BaseErrorCode.OWNERSHIP_DENIED);
        }
        return session;
    }

    /** 仅校验归属，不返回会话（供控制器同步越权拦截）。 */
    public void checkOwner(Long userId, Long sessionId) {
        loadAndCheckOwner(userId, sessionId);
    }

    /** 状态流转（含状态机校验）。 */
    public void transit(Long sessionId, SessionStatus from, SessionStatus to) {
        InterviewSessionStateMachine.check(from, to);
        InterviewSessionDO session = sessionMapper.selectById(sessionId);
        session.setStatus(to.name());
        sessionMapper.updateById(session);
    }

    /** 计算会话总分（各题加权）。 */
    public double computeTotalScore(Long sessionId) {
        List<SessionQuestionDO> questions = questionMapper.selectBySession(sessionId);
        List<Integer> questionScores = new ArrayList<>();
        for (SessionQuestionDO q : questions) {
            List<SessionAnswerDO> ans = answerMapper.selectByQuestion(sessionId, q.getId());
            SessionAnswerDO original = ans.stream().filter(a -> a.getParentAnswerId() == null).findFirst().orElse(null);
            if (original == null || original.getScore() == null) {
                questionScores.add(0);
                continue;
            }
            List<Integer> followUps = ans.stream().filter(a -> a.getParentAnswerId() != null && a.getScore() != null)
                    .map(SessionAnswerDO::getScore).collect(Collectors.toList());
            questionScores.add(ScorePolicy.questionScore(original.getScore(), followUps));
        }
        return ScorePolicy.totalScore(questionScores);
    }

    /* ------------------------------ 转换 ------------------------------ */

    private SessionBriefResp toBrief(InterviewSessionDO s) {
        SessionBriefResp r = new SessionBriefResp();
        r.setId(s.getId());
        r.setSessionNo(s.getSessionNo());
        r.setUserId(s.getUserId());
        r.setDirections(parseDirections(s.getDirections()));
        r.setDifficulty(s.getDifficulty());
        r.setTotalQuestion(s.getTotalQuestion());
        r.setCurrentIndex(s.getCurrentIndex());
        r.setStatus(s.getStatus());
        r.setScore(s.getScore());
        r.setResumeId(s.getResumeId());
        r.setCreatedAt(s.getCreateTime());
        r.setFinishedAt(s.getFinishedAt());
        return r;
    }

    private SessionDetailResp toDetail(InterviewSessionDO s) {
        SessionDetailResp r = new SessionDetailResp();
        r.setId(s.getId());
        r.setSessionNo(s.getSessionNo());
        r.setUserId(s.getUserId());
        r.setResumeId(s.getResumeId());
        r.setDirections(parseDirections(s.getDirections()));
        r.setDifficulty(s.getDifficulty());
        r.setTotalQuestion(s.getTotalQuestion());
        r.setCurrentIndex(s.getCurrentIndex());
        r.setStatus(s.getStatus());
        r.setPrevStatus(s.getPrevStatus());
        r.setScore(s.getScore());
        r.setJdText(s.getJdText());
        r.setCreatedAt(s.getCreateTime());
        r.setStartedAt(s.getStartedAt());
        r.setFinishedAt(s.getFinishedAt());
        return r;
    }

    private QuestionRespStream toQuestionStream(SessionQuestionDO q, GeneratedQuestion g, Integer total) {
        QuestionRespStream r = new QuestionRespStream();
        r.setQuestionNo(q.getQuestionNo());
        r.setSessionQuestionId(q.getId());
        r.setTitle(g.title());
        r.setReferencePoints(g.referencePoints());
        r.setDifficulty(g.difficulty());
        r.setSource(g.source());
        r.setTotalQuestion(total);
        r.setPhase(q.getPhase());
        return r;
    }

    private SessionAnswerItemResp toAnswerItem(SessionAnswerDO a) {
        SessionAnswerItemResp r = new SessionAnswerItemResp();
        r.setAnswerId(a.getId());
        r.setSessionQuestionId(a.getSessionQuestionId());
        r.setContent(a.getContent());
        r.setScore(a.getScore());
        r.setComment(a.getComment());
        r.setHighlights(parseList(a.getHighlights()));
        r.setGaps(parseList(a.getGaps()));
        r.setImprovedAnswer(a.getImprovedAnswer());
        r.setIsFollowUp(a.getIsFollowUp() != null && a.getIsFollowUp() == 1);
        r.setParentAnswerId(a.getParentAnswerId());
        r.setFollowUpCount(a.getFollowUpCount());
        r.setSkipped(a.getSkipped() != null && a.getSkipped() == 1);
        r.setEvaluatedBy(a.getEvaluatedBy());
        r.setDegraded("RULE".equals(a.getEvaluatedBy()));
        r.setCreatedAt(a.getCreateTime());
        return r;
    }

    private List<String> parseDirections(String s) {
        if (s == null || s.isBlank()) {
            return new ArrayList<>();
        }
        return java.util.Arrays.stream(s.split(",")).filter(x -> !x.isBlank()).collect(Collectors.toList());
    }

    private List<String> parseList(String json) {
        List<String> list = JsonUtil.parse(json, new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
        return list == null ? new ArrayList<>() : list;
    }

    /** 从点评正文中解析 {@code ===JSON===} 之后的 followUpQuestion（库表无独立字段，纯兜底）。 */
    private String parseFollowUpQuestion(String comment) {
        if (comment == null || comment.isBlank()) {
            return null;
        }
        int idx = comment.indexOf("===JSON===");
        if (idx < 0) {
            return null;
        }
        String json = comment.substring(idx + "===JSON===".length()).trim();
        if (json.isEmpty()) {
            return null;
        }
        try {
            com.fasterxml.jackson.databind.JsonNode node = JsonUtil.MAPPER.readTree(json);
            com.fasterxml.jackson.databind.JsonNode fu = node.get("followUpQuestion");
            return fu != null && !fu.isNull() ? fu.asText() : null;
        } catch (Exception e) {
            return null;
        }
    }
}

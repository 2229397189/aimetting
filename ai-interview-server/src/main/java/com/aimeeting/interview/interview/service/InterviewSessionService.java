package com.aimeeting.interview.interview.service;

import com.aimeeting.interview.common.convention.result.PageInfo;
import com.aimeeting.interview.interview.api.io.req.CreateSessionReq;
import com.aimeeting.interview.interview.api.io.req.SessionPageQuery;
import com.aimeeting.interview.interview.api.io.resp.AnswerDetailResp;
import com.aimeeting.interview.interview.api.io.resp.MessageItemResp;
import com.aimeeting.interview.interview.api.io.resp.QuestionRespStream;
import com.aimeeting.interview.interview.api.io.resp.SessionBriefResp;
import com.aimeeting.interview.interview.api.io.resp.SessionDetailResp;
import com.aimeeting.interview.interview.api.io.resp.SessionStatusResp;
import java.util.List;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 面试会话服务。
 */
public interface InterviewSessionService {

    /**
     * 创建会话（status=INIT）。
     *
     * @param userId 用户 ID
     * @param req    创建请求
     * @return 会话 ID
     */
    Long create(Long userId, CreateSessionReq req);

    /**
     * 会话分页列表。
     *
     * @param userId 用户 ID
     * @param query  查询条件
     * @return 分页结果
     */
    PageInfo<SessionBriefResp> page(Long userId, SessionPageQuery query);

    /**
     * 会话详情（含题目 / 答案 / 追问）。
     *
     * @param userId    用户 ID
     * @param sessionId 会话 ID
     * @return 详情
     */
    SessionDetailResp detail(Long userId, Long sessionId);

    /**
     * 开始面试：INIT → ASKING，阻塞式产出首题。
     *
     * @param userId    用户 ID
     * @param sessionId 会话 ID
     * @return 首题
     */
    QuestionRespStream start(Long userId, Long sessionId);

    /**
     * SSE 取下一题。
     *
     * @param userId    用户 ID
     * @param sessionId 会话 ID
     * @param emitter   SSE 发射器
     */
    void streamNextQuestion(Long userId, Long sessionId, SseEmitter emitter);

    /**
     * 暂停会话（记录 prevStatus）。
     *
     * @param userId    用户 ID
     * @param sessionId 会话 ID
     */
    void pause(Long userId, Long sessionId);

    /**
     * 恢复会话：PAUSED → prevStatus。
     *
     * @param userId    用户 ID
     * @param sessionId 会话 ID
     */
    void resume(Long userId, Long sessionId);

    /**
     * 结束会话：→ COMPLETED，计算总分，返回报告 ID。
     *
     * @param userId    用户 ID
     * @param sessionId 会话 ID
     * @return 报告 ID（M6 接入前为 null）
     */
    Long finish(Long userId, Long sessionId);

    /**
     * 删除会话（逻辑删除）。
     *
     * @param userId    用户 ID
     * @param sessionId 会话 ID
     */
    void remove(Long userId, Long sessionId);

    /**
     * 轻量状态轮询（SSE 断线补偿）。
     *
     * @param userId    用户 ID
     * @param sessionId 会话 ID
     * @return 状态
     */
    SessionStatusResp status(Long userId, Long sessionId);

    /**
     * 会话对话流水。
     *
     * @param userId    用户 ID
     * @param sessionId 会话 ID
     * @return 流水列表
     */
    List<MessageItemResp> messages(Long userId, Long sessionId);

    /**
     * 跳过当前题（记 0 分 + skipped=true）。
     *
     * @param userId            用户 ID
     * @param sessionId         会话 ID
     * @param sessionQuestionId 会话题目 ID
     */
    void skip(Long userId, Long sessionId, Long sessionQuestionId);

    /**
     * 查询单条答题详情（越权抛 B0303）。
     *
     * @param userId   用户 ID
     * @param answerId 答案 ID
     * @return 答题详情
     */
    AnswerDetailResp answerDetail(Long userId, Long answerId);

    /**
     * 仅校验会话归属（越权抛 B0303），供控制器同步越权拦截。
     *
     * @param userId    用户 ID
     * @param sessionId 会话 ID
     */
    void checkOwner(Long userId, Long sessionId);
}

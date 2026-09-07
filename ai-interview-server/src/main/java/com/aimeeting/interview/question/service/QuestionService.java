package com.aimeeting.interview.question.service;

import com.aimeeting.interview.common.convention.result.PageInfo;
import com.aimeeting.interview.question.api.io.req.QuestionQueryReq;
import com.aimeeting.interview.question.api.io.req.QuestionSaveReq;
import com.aimeeting.interview.question.api.io.req.RandomQuestionReq;
import com.aimeeting.interview.question.api.io.resp.DirectionsResp;
import com.aimeeting.interview.question.api.io.resp.QuestionDetailResp;
import com.aimeeting.interview.question.api.io.resp.QuestionImportResult;
import com.aimeeting.interview.question.api.io.resp.QuestionResp;
import java.util.List;

/**
 * 题库服务。
 */
public interface QuestionService {

    PageInfo<QuestionResp> page(QuestionQueryReq req);

    QuestionDetailResp detail(Long id);

    Long save(QuestionSaveReq req, Long operatorId);

    Boolean update(Long id, QuestionSaveReq req);

    Boolean remove(Long id);

    List<QuestionResp> random(RandomQuestionReq req);

    DirectionsResp directions();

    QuestionImportResult importQuestions(List<QuestionSaveReq> list, Long operatorId);
}

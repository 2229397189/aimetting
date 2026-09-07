package com.aimeeting.interview.question.service;

import com.aimeeting.interview.common.convention.errorcode.BaseErrorCode;
import com.aimeeting.interview.common.convention.exception.ClientException;
import com.aimeeting.interview.common.convention.exception.ServiceException;
import com.aimeeting.interview.common.convention.result.PageInfo;
import com.aimeeting.interview.common.util.JsonUtil;
import com.aimeeting.interview.question.api.io.req.QuestionQueryReq;
import com.aimeeting.interview.question.api.io.req.QuestionSaveReq;
import com.aimeeting.interview.question.api.io.req.RandomQuestionReq;
import com.aimeeting.interview.question.api.io.resp.DirectionOption;
import com.aimeeting.interview.question.api.io.resp.DifficultyOption;
import com.aimeeting.interview.question.api.io.resp.DirectionsResp;
import com.aimeeting.interview.question.api.io.resp.QuestionDetailResp;
import com.aimeeting.interview.question.api.io.resp.QuestionImportResult;
import com.aimeeting.interview.question.api.io.resp.QuestionResp;
import com.aimeeting.interview.question.dao.entity.QuestionDO;
import com.aimeeting.interview.question.dao.mapper.CountRow;
import com.aimeeting.interview.question.dao.mapper.QuestionMapper;
import com.aimeeting.interview.question.domain.Direction;
import com.aimeeting.interview.question.domain.Difficulty;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 题库服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionServiceImpl implements QuestionService {

    private final QuestionMapper questionMapper;

    @Override
    public PageInfo<QuestionResp> page(QuestionQueryReq req) {
        int pageNum = req.getPageNum() == null || req.getPageNum() < 1 ? 1 : req.getPageNum();
        int pageSize = req.getPageSize() == null || req.getPageSize() < 1 ? 10 : Math.min(req.getPageSize(), 50);
        long offset = (long) (pageNum - 1) * pageSize;
        List<QuestionDO> dos = questionMapper.selectPageList(
                req.getDirection(), req.getDifficulty(), req.getKeyword(), offset, pageSize);
        long total = questionMapper.countPageList(req.getDirection(), req.getDifficulty(), req.getKeyword());
        PageInfo<QuestionResp> pageInfo = new PageInfo<>();
        pageInfo.setList(dos.stream().map(this::toResp).collect(Collectors.toList()));
        pageInfo.setTotal(total);
        pageInfo.setPageNum(pageNum);
        pageInfo.setPageSize(pageSize);
        return pageInfo;
    }

    @Override
    public QuestionDetailResp detail(Long id) {
        QuestionDO DO = questionMapper.selectById(id);
        if (DO == null) {
            throw new ClientException("题目不存在", BaseErrorCode.PARAM_ERROR);
        }
        return toDetail(DO);
    }

    @Override
    public Long save(QuestionSaveReq req, Long operatorId) {
        validate(req);
        QuestionDO DO = toDO(req);
        DO.setCreatedBy(operatorId);
        DO.setSource(DO.getSource() == null ? "ADMIN" : DO.getSource());
        questionMapper.insert(DO);
        return DO.getId();
    }

    @Override
    public Boolean update(Long id, QuestionSaveReq req) {
        QuestionDO existing = questionMapper.selectById(id);
        if (existing == null) {
            throw new ClientException("题目不存在", BaseErrorCode.PARAM_ERROR);
        }
        validate(req);
        QuestionDO DO = toDO(req);
        DO.setId(id);
        questionMapper.updateById(DO);
        return true;
    }

    @Override
    public Boolean remove(Long id) {
        if (questionMapper.selectById(id) == null) {
            throw new ClientException("题目不存在", BaseErrorCode.PARAM_ERROR);
        }
        questionMapper.deleteById(id);
        return true;
    }

    @Override
    public List<QuestionResp> random(RandomQuestionReq req) {
        int count = req.getCount() == null || req.getCount() < 1 ? 5 : Math.min(req.getCount(), 20);
        List<QuestionDO> dos = questionMapper.selectRandom(
                req.getDirection(), req.getDifficulty(), req.getExcludeIds(), count);
        return dos.stream().map(this::toResp).collect(Collectors.toList());
    }

    @Override
    public DirectionsResp directions() {
        DirectionsResp resp = new DirectionsResp();
        Map<String, Long> dirCounts = questionMapper.countByDirection().stream()
                .collect(Collectors.toMap(CountRow::getK, CountRow::getCnt, (a, b) -> b));
        List<DirectionOption> dirs = Direction.options().stream().peek(o -> {
            Long c = dirCounts.get(o.getCode());
            o.setCount(c == null ? 0L : c);
        }).collect(Collectors.toList());
        Map<String, Long> diffCounts = questionMapper.countByDifficulty().stream()
                .collect(Collectors.toMap(CountRow::getK, CountRow::getCnt, (a, b) -> b));
        List<DifficultyOption> diffs = Difficulty.options().stream().peek(o -> {
            Long c = diffCounts.get(o.getCode());
            o.setCount(c == null ? 0L : c);
        }).collect(Collectors.toList());
        resp.setDirections(dirs);
        resp.setDifficulties(diffs);
        return resp;
    }

    @Override
    public QuestionImportResult importQuestions(List<QuestionSaveReq> list, Long operatorId) {
        int success = 0;
        int fail = 0;
        if (list != null) {
            for (QuestionSaveReq req : list) {
                try {
                    save(req, operatorId);
                    success++;
                } catch (Exception e) {
                    log.warn("[Question] 导入失败 title={}, err={}", req.getTitle(), e.getMessage());
                    fail++;
                }
            }
        }
        QuestionImportResult result = new QuestionImportResult();
        result.setSuccessCount(success);
        result.setFailCount(fail);
        return result;
    }

    /* ------------------------------ 内部转换 ------------------------------ */

    private void validate(QuestionSaveReq req) {
        if (!Direction.isValid(req.getDirection())) {
            throw new ClientException("方向不合法: " + req.getDirection(), BaseErrorCode.PARAM_ERROR);
        }
        if (!Difficulty.isValid(req.getDifficulty())) {
            throw new ClientException("难度不合法: " + req.getDifficulty(), BaseErrorCode.PARAM_ERROR);
        }
    }

    private QuestionDO toDO(QuestionSaveReq req) {
        QuestionDO DO = new QuestionDO();
        DO.setDirection(req.getDirection());
        DO.setDifficulty(req.getDifficulty());
        DO.setTitle(req.getTitle());
        DO.setReferencePoints(JsonUtil.toJson(req.getReferencePoints()));
        DO.setTags(JsonUtil.toJson(req.getTags()));
        DO.setAnalysis(req.getAnalysis());
        DO.setSource("ADMIN");
        DO.setStatus(req.getStatus() == null ? 1 : req.getStatus());
        return DO;
    }

    private QuestionResp toResp(QuestionDO DO) {
        QuestionResp resp = new QuestionResp();
        resp.setId(DO.getId());
        resp.setDirection(DO.getDirection());
        resp.setDifficulty(DO.getDifficulty());
        resp.setTitle(DO.getTitle());
        resp.setReferencePoints(JsonUtil.parse(DO.getReferencePoints(), new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}));
        resp.setTags(JsonUtil.parse(DO.getTags(), new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}));
        resp.setAnalysis(DO.getAnalysis());
        resp.setSource(DO.getSource());
        resp.setStatus(DO.getStatus());
        resp.setCreatedAt(DO.getCreateTime());
        return resp;
    }

    private QuestionDetailResp toDetail(QuestionDO DO) {
        QuestionDetailResp resp = new QuestionDetailResp();
        resp.setId(DO.getId());
        resp.setDirection(DO.getDirection());
        resp.setDifficulty(DO.getDifficulty());
        resp.setTitle(DO.getTitle());
        resp.setReferencePoints(JsonUtil.parse(DO.getReferencePoints(), new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}));
        resp.setTags(JsonUtil.parse(DO.getTags(), new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}));
        resp.setAnalysis(DO.getAnalysis());
        resp.setSource(DO.getSource());
        resp.setStatus(DO.getStatus());
        resp.setCreatedAt(DO.getCreateTime());
        resp.setUpdatedAt(DO.getUpdateTime());
        resp.setCreatedBy(DO.getCreatedBy());
        return resp;
    }
}

package com.aimeeting.interview.report.service;

import com.aimeeting.interview.ai.AiProperties;
import com.aimeeting.interview.ai.fallback.RuleEvaluator;
import com.aimeeting.interview.ai.guard.AiGuardService;
import com.aimeeting.interview.ai.model.AiBizType;
import com.aimeeting.interview.ai.model.AiRequest;
import com.aimeeting.interview.ai.model.AiStage;
import com.aimeeting.interview.ai.model.AiTextResult;
import com.aimeeting.interview.common.convention.errorcode.BaseErrorCode;
import com.aimeeting.interview.common.convention.exception.ClientException;
import com.aimeeting.interview.common.convention.exception.ServiceException;
import com.aimeeting.interview.common.convention.result.PageInfo;
import com.aimeeting.interview.common.util.JsonUtil;
import com.aimeeting.interview.report.api.io.req.ReportPageQuery;
import com.aimeeting.interview.report.api.io.resp.ReportBriefResp;
import com.aimeeting.interview.report.api.io.resp.ReportDetailResp;
import com.aimeeting.interview.report.api.io.resp.ReportItem;
import com.aimeeting.interview.report.dao.entity.ReportDO;
import com.aimeeting.interview.report.dao.entity.SessionAnswerReadDO;
import com.aimeeting.interview.report.dao.entity.SessionMetaReadDO;
import com.aimeeting.interview.report.dao.entity.SessionQuestionReadDO;
import com.aimeeting.interview.report.dao.mapper.ReportMapper;
import com.aimeeting.interview.report.dao.mapper.ReportReadMapper;
import com.aimeeting.interview.report.domain.ReportDimension;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 报告服务实现（M6）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportMapper reportMapper;
    private final ReportReadMapper reportReadMapper;
    private final AiGuardService aiGuardService;
    private final AiProperties aiProperties;

    @Override
    public Long generate(Long userId, Long sessionId) {
        SessionMetaReadDO meta = requireOwnedSession(userId, sessionId);
        // 已生成则直接返回（自然幂等，配合 uk_session 唯一约束）
        ReportDO existing = reportMapper.selectOne(
                Wrappers.<ReportDO>lambdaQuery().eq(ReportDO::getSessionId, sessionId));
        if (existing != null) {
            return existing.getId();
        }

        List<SessionQuestionReadDO> questions = reportReadMapper.listQuestions(sessionId);
        List<SessionAnswerReadDO> answers = reportReadMapper.listAnswers(sessionId);
        Map<Long, List<SessionAnswerReadDO>> byQuestion = answers.stream()
                .collect(Collectors.groupingBy(SessionAnswerReadDO::getSessionQuestionId));
        List<ReportItem> items = new ArrayList<>();
        List<Integer> scores = new ArrayList<>();
        int sum = 0;
        for (SessionQuestionReadDO q : questions) {
            ReportItem item = buildItem(q, byQuestion.getOrDefault(q.getId(), new ArrayList<>()));
            items.add(item);
            Integer s = item.getScore() != null ? item.getScore() : 0;
            scores.add(s);
            sum += s;
        }
        int count = Math.max(1, questions.size());
        BigDecimal totalScore = new BigDecimal(sum).divide(BigDecimal.valueOf(count), 1, RoundingMode.HALF_UP);

        ReportDO report = new ReportDO();
        report.setSessionId(sessionId);
        report.setUserId(userId);
        report.setTotalScore(totalScore);
        report.setGeneratedBy("AI");

        String raw = null;
        try {
            raw = aiGuardService.execute(AiStage.REPORT_GEN,
                    "report:" + sessionId, userId,
                    buildReportRequest(meta, items, totalScore),
                    AiTextResult::getContent);
            applyAiResult(report, raw, totalScore.intValue(), scores);
        } catch (Exception e) {
            log.warn("[Report] AI 生成失败，降级 RULE: {}", e.getMessage());
            applyRuleFallback(report, totalScore.intValue(), scores);
        }

        reportMapper.insert(report);
        return report.getId();
    }

    @Override
    public ReportDetailResp getBySession(Long userId, Long sessionId) {
        requireOwnedSession(userId, sessionId);
        ReportDO report = reportMapper.selectOne(
                Wrappers.<ReportDO>lambdaQuery().eq(ReportDO::getSessionId, sessionId));
        if (report == null) {
            return null;
        }
        return toDetail(report);
    }

    @Override
    public ReportDetailResp getById(Long userId, Long reportId) {
        ReportDO report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new ClientException("报告不存在", BaseErrorCode.PARAM_ERROR);
        }
        requireOwned(userId, report);
        return toDetail(report);
    }

    @Override
    public PageInfo<ReportBriefResp> page(Long userId, ReportPageQuery q) {
        long pageNum = q.safePageNum();
        long pageSize = q.safePageSize();
        long offset = (pageNum - 1) * pageSize;
        long total = reportMapper.selectCount(
                Wrappers.<ReportDO>lambdaQuery().eq(ReportDO::getUserId, userId));
        List<ReportDO> list = reportMapper.selectList(Wrappers.<ReportDO>lambdaQuery()
                .eq(ReportDO::getUserId, userId)
                .orderByDesc(ReportDO::getCreateTime)
                .last("LIMIT " + pageSize + " OFFSET " + offset));
        PageInfo<ReportBriefResp> pageInfo = new PageInfo<>();
        pageInfo.setList(list.stream().map(this::toBrief).toList());
        pageInfo.setTotal(total);
        pageInfo.setPageNum(pageNum);
        pageInfo.setPageSize(pageSize);
        return pageInfo;
    }

    @Override
    public void remove(Long userId, Long reportId) {
        ReportDO report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new ClientException("报告不存在", BaseErrorCode.PARAM_ERROR);
        }
        requireOwned(userId, report);
        reportMapper.deleteById(reportId);
    }

    @Override
    public String exportMarkdown(Long userId, Long reportId) {
        ReportDetailResp detail = getById(userId, reportId);
        return toMarkdown(detail);
    }

    /* ------------------------------ 内部工具 ------------------------------ */

    private SessionMetaReadDO requireOwnedSession(Long userId, Long sessionId) {
        SessionMetaReadDO meta = reportReadMapper.selectSessionMeta(sessionId);
        if (meta == null) {
            throw new ClientException("会话不存在", BaseErrorCode.PARAM_ERROR);
        }
        if (!meta.getUserId().equals(userId)) {
            throw new ServiceException("无权访问该资源", BaseErrorCode.OWNERSHIP_DENIED);
        }
        return meta;
    }

    private void requireOwned(Long userId, ReportDO report) {
        if (!report.getUserId().equals(userId)) {
            throw new ServiceException("无权访问该资源", BaseErrorCode.OWNERSHIP_DENIED);
        }
    }

    private ReportItem buildItem(SessionQuestionReadDO q, List<SessionAnswerReadDO> answers) {
        SessionAnswerReadDO main = answers.stream()
                .filter(a -> a.getIsFollowUp() == null || a.getIsFollowUp() == 0)
                .findFirst()
                .orElse(answers.isEmpty() ? null : answers.get(0));
        ReportItem item = new ReportItem();
        item.setQuestionNo(q.getQuestionNo());
        item.setTitle(q.getTitle());
        item.setReferencePoints(parseStringArray(q.getReferencePoints()));
        item.setDifficulty(q.getDifficulty());
        item.setPhase(q.getPhase());
        boolean questionSkipped = q.getSkipped() != null && q.getSkipped() == 1;
        if (main != null) {
            item.setAnswer(main.getContent());
            item.setScore(main.getScore());
            item.setComment(main.getComment());
            item.setHighlights(parseStringArray(main.getHighlights()));
            item.setGaps(parseStringArray(main.getGaps()));
            item.setImprovedAnswer(main.getImprovedAnswer());
            boolean answerSkipped = main.getSkipped() != null && main.getSkipped() == 1;
            item.setSkipped(questionSkipped || answerSkipped);
        } else {
            item.setSkipped(questionSkipped);
        }
        return item;
    }

    private void applyAiResult(ReportDO report, String raw, int totalScore, List<Integer> scores) {
        Map<String, Object> root = JsonUtil.parse(raw, new TypeReference<Map<String, Object>>() {});
        if (root == null) {
            throw new IllegalStateException("AI 返回无法解析");
        }
        Object dimsObj = root.get("dimensions");
        Map<String, Integer> dims = dimsObj instanceof Map
                ? castDimensionMap((Map<?, ?>) dimsObj) : new LinkedHashMap<>();
        Map<String, Integer> normalized = ReportDimension.normalize(dims, totalScore);
        List<String> highlights = parseStringList(root.get("highlights"));
        List<String> improvements = parseStringList(root.get("improvements"));
        List<String> actions = parseStringList(root.get("actions"));
        String overall = root.get("overallComment") instanceof String ? (String) root.get("overallComment") : null;
        // 校验不足则降级
        if (highlights.size() < 2 || improvements.size() < 3 || actions.size() < 3 || overall == null) {
            applyRuleFallback(report, totalScore, scores);
            return;
        }
        report.setDimensionJson(JsonUtil.toJson(normalized));
        report.setHighlights(JsonUtil.toJson(highlights));
        report.setImprovements(JsonUtil.toJson(improvements));
        report.setActions(JsonUtil.toJson(actions));
        report.setOverallComment(overall);
        report.setGeneratedBy("AI");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Integer> castDimensionMap(Map<?, ?> raw) {
        Map<String, Integer> result = new LinkedHashMap<>();
        raw.forEach((k, v) -> {
            if (k != null && v instanceof Number) {
                result.put(String.valueOf(k), ((Number) v).intValue());
            }
        });
        return result;
    }

    private void applyRuleFallback(ReportDO report, int totalScore, List<Integer> scores) {
        String fb = RuleEvaluator.fallbackReport(scores);
        Map<String, Object> root = JsonUtil.parse(fb, new TypeReference<Map<String, Object>>() {});
        Map<String, Integer> dims = root != null && root.get("dimensions") instanceof Map
                ? castDimensionMap((Map<?, ?>) root.get("dimensions")) : new LinkedHashMap<>();
        Map<String, Integer> normalized = ReportDimension.normalize(dims, totalScore);
        report.setDimensionJson(JsonUtil.toJson(normalized));
        report.setHighlights(JsonUtil.toJson(
                root != null ? parseStringList(root.get("highlights")) : new ArrayList<String>()));
        report.setImprovements(JsonUtil.toJson(
                root != null ? parseStringList(root.get("improvements")) : new ArrayList<String>()));
        report.setActions(JsonUtil.toJson(
                root != null ? parseStringList(root.get("actions")) : new ArrayList<String>()));
        report.setOverallComment(root != null && root.get("overallComment") instanceof String
                ? (String) root.get("overallComment") : "（规则报告）本次面试已完成");
        report.setGeneratedBy("RULE");
    }

    private AiRequest buildReportRequest(SessionMetaReadDO meta, List<ReportItem> items, BigDecimal totalScore) {
        String directions = meta.getDirections() == null ? "" : meta.getDirections();
        String difficulty = meta.getDifficulty() == null ? "" : meta.getDifficulty();
        String itemsJson = JsonUtil.toJson(items);
        String userPrompt = String.format(
                "面试方向：%s；难度：%s；已算好的总分：%s（仅供参考，不要修改）。\n"
                        + "逐题情况（JSON，最多 30 条）：%s\n"
                        + "请基于以上给出五维评分与总结。",
                directions, difficulty, totalScore.toPlainString(), itemsJson);
        return AiRequest.builder()
                .bizType(AiBizType.REPORT)
                .systemPrompt("你是资深技术面试官，正在为候选人撰写面试总结报告。严格只输出 JSON，不要输出任何额外说明文字。"
                        + "JSON 字段：overallComment(150-300字 Markdown),"
                        + "dimensions({PROFESSIONAL:0-100,EXPRESSION:0-100,LOGIC:0-100,PROJECT_DEPTH:0-100,POTENTIAL:0-100}),"
                        + "highlights(字符串数组,≥2),improvements(字符串数组,≥3),actions(字符串数组,≥3)。")
                .userPrompt(userPrompt)
                .model(aiProperties.modelFor(AiBizType.REPORT))
                .temperature(0.5)
                .maxTokens(2000)
                .jsonMode(true)
                .build();
    }

    private ReportDetailResp toDetail(ReportDO report) {
        SessionMetaReadDO meta = reportReadMapper.selectSessionMeta(report.getSessionId());
        ReportDetailResp resp = new ReportDetailResp();
        resp.setId(report.getId());
        resp.setSessionId(report.getSessionId());
        resp.setUserId(report.getUserId());
        resp.setSessionNo(meta == null ? null : meta.getSessionNo());
        resp.setTotalScore(report.getTotalScore());
        Map<String, Integer> dims = JsonUtil.parse(report.getDimensionJson(),
                new TypeReference<Map<String, Integer>>() {});
        resp.setDimensionJson(dims == null ? new LinkedHashMap<>() : dims);
        resp.setDimensions(resp.getDimensionJson());
        resp.setHighlights(parseStringListFromJson(report.getHighlights()));
        resp.setImprovements(parseStringListFromJson(report.getImprovements()));
        resp.setActions(parseStringListFromJson(report.getActions()));
        resp.setOverallComment(report.getOverallComment());
        resp.setGeneratedBy(report.getGeneratedBy());
        resp.setCreatedAt(report.getCreateTime());
        if (meta != null) {
            resp.setDirections(Arrays.stream(meta.getDirections().split(","))
                    .filter(s -> !s.isBlank()).map(String::trim).collect(Collectors.toList()));
            resp.setDifficulty(meta.getDifficulty());
        }
        resp.setItems(buildItemsForSession(report.getSessionId()));
        return resp;
    }

    private ReportBriefResp toBrief(ReportDO report) {
        SessionMetaReadDO meta = reportReadMapper.selectSessionMeta(report.getSessionId());
        ReportBriefResp resp = new ReportBriefResp();
        resp.setId(report.getId());
        resp.setSessionId(report.getSessionId());
        resp.setSessionNo(meta == null ? null : meta.getSessionNo());
        resp.setTotalScore(report.getTotalScore());
        resp.setGeneratedBy(report.getGeneratedBy());
        resp.setCreatedAt(report.getCreateTime());
        if (meta != null) {
            resp.setDirections(Arrays.stream(meta.getDirections().split(","))
                    .filter(s -> !s.isBlank()).map(String::trim).collect(Collectors.toList()));
            resp.setDifficulty(meta.getDifficulty());
        }
        return resp;
    }

    private List<ReportItem> buildItemsForSession(Long sessionId) {
        List<SessionQuestionReadDO> questions = reportReadMapper.listQuestions(sessionId);
        List<SessionAnswerReadDO> answers = reportReadMapper.listAnswers(sessionId);
        Map<Long, List<SessionAnswerReadDO>> byQuestion = answers.stream()
                .collect(Collectors.groupingBy(SessionAnswerReadDO::getSessionQuestionId));
        List<ReportItem> items = new ArrayList<>();
        for (SessionQuestionReadDO q : questions) {
            items.add(buildItem(q, byQuestion.getOrDefault(q.getId(), new ArrayList<>())));
        }
        return items;
    }

    private String toMarkdown(ReportDetailResp d) {
        StringBuilder sb = new StringBuilder();
        sb.append("# 面试报告\n\n");
        if (d.getSessionNo() != null) {
            sb.append("**会话编号**：").append(d.getSessionNo()).append("\n\n");
        }
        sb.append("**总分**：").append(d.getTotalScore()).append(" / 100\n\n");
        sb.append("**生成方式**：").append("AI".equals(d.getGeneratedBy()) ? "AI 智能生成" : "规则引擎兜底").append("\n\n");
        sb.append("## 五维评分\n\n");
        if (d.getDimensionJson() != null) {
            for (Map.Entry<String, Integer> e : d.getDimensionJson().entrySet()) {
                sb.append("- ").append(e.getKey()).append("：").append(e.getValue()).append("\n");
            }
        }
        sb.append("\n## 总体评价\n\n").append(d.getOverallComment() == null ? "" : d.getOverallComment()).append("\n\n");
        sb.append("## 亮点\n\n");
        appendList(sb, d.getHighlights());
        sb.append("\n## 改进建议\n\n");
        appendList(sb, d.getImprovements());
        sb.append("\n## 后续行动\n\n");
        appendList(sb, d.getActions());
        sb.append("\n## 逐题点评\n\n");
        if (d.getItems() != null) {
            for (ReportItem it : d.getItems()) {
                sb.append("### 第 ").append(it.getQuestionNo()).append(" 题：")
                        .append(it.getTitle() == null ? "" : it.getTitle()).append("\n\n");
                sb.append("- 得分：").append(it.getScore() == null ? "-" : it.getScore()).append("\n");
                if (it.getComment() != null) {
                    sb.append("- 点评：").append(it.getComment()).append("\n");
                }
                appendList(sb, it.getHighlights(), "  - 亮点：");
                appendList(sb, it.getGaps(), "  - 不足：");
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    private void appendList(StringBuilder sb, List<String> list) {
        appendList(sb, list, "- ");
    }

    private void appendList(StringBuilder sb, List<String> list, String prefix) {
        if (list == null || list.isEmpty()) {
            sb.append("(无)\n");
            return;
        }
        for (String s : list) {
            sb.append(prefix).append(s).append("\n");
        }
    }

    private List<String> parseStringArray(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        List<String> list = JsonUtil.parse(json, new TypeReference<List<String>>() {});
        return list == null ? new ArrayList<>() : list;
    }

    private List<String> parseStringListFromJson(String json) {
        return parseStringArray(json);
    }

    private List<String> parseStringList(Object obj) {
        if (obj instanceof List<?> list) {
            List<String> result = new ArrayList<>();
            list.forEach(o -> {
                if (o != null) {
                    result.add(String.valueOf(o));
                }
            });
            return result;
        }
        if (obj instanceof String s && !s.isBlank()) {
            List<String> parsed = JsonUtil.parse(s, new TypeReference<List<String>>() {});
            return parsed == null ? new ArrayList<>() : parsed;
        }
        return new ArrayList<>();
    }
}

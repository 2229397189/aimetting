package com.aimeeting.interview.admin.service;

import com.aimeeting.interview.admin.api.io.resp.AiCallLogResp;
import com.aimeeting.interview.admin.api.io.resp.AiHealthResp;
import com.aimeeting.interview.admin.api.io.resp.AdminUserResp;
import com.aimeeting.interview.admin.api.io.resp.OverviewStatsResp;
import com.aimeeting.interview.admin.api.io.resp.SessionTrendResp;
import com.aimeeting.interview.admin.api.io.resp.SessionTrendResp.DirectionCount;
import com.aimeeting.interview.admin.dao.entity.TrendRow;
import com.aimeeting.interview.admin.dao.entity.UserReadDO;
import com.aimeeting.interview.admin.dao.mapper.AdminMapper;
import com.aimeeting.interview.ai.guard.AiGuardService;
import com.aimeeting.interview.ai.guard.AiHealthSnapshot;
import com.aimeeting.interview.ai.log.AiCallLogService;
import com.aimeeting.interview.common.convention.errorcode.BaseErrorCode;
import com.aimeeting.interview.common.convention.exception.ClientException;
import com.aimeeting.interview.common.convention.result.PageInfo;
import com.aimeeting.interview.common.convention.result.PageQuery;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 管理端服务实现（M8）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final Map<String, String> DIRECTION_LABELS = Map.of(
            "JAVA_BACKEND", "Java 后端", "FRONTEND", "前端", "DATABASE", "数据库", "OS", "操作系统",
            "NETWORK", "网络", "ALGORITHM", "算法", "SYSTEM_DESIGN", "系统设计", "BEHAVIORAL", "行为面试");

    private final AdminMapper adminMapper;
    private final AiCallLogService aiCallLogService;
    private final AiGuardService aiGuardService;

    @Override
    public PageInfo<AdminUserResp> users(PageQuery q, String keyword) {
        long pageNum = q.safePageNum();
        long pageSize = q.safePageSize();
        long offset = (pageNum - 1) * pageSize;
        long total = adminMapper.countUsers(keyword);
        List<UserReadDO> list = adminMapper.listUsers(keyword, offset, pageSize);
        PageInfo<AdminUserResp> pageInfo = new PageInfo<>();
        pageInfo.setList(list.stream().map(this::toUserResp).toList());
        pageInfo.setTotal(total);
        pageInfo.setPageNum(pageNum);
        pageInfo.setPageSize(pageSize);
        return pageInfo;
    }

    @Override
    public void updateUserStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new ClientException("status 仅支持 0(禁用) 或 1(启用)", BaseErrorCode.PARAM_ERROR);
        }
        int rows = adminMapper.updateStatus(id, status);
        if (rows == 0) {
            throw new ClientException("用户不存在", BaseErrorCode.PARAM_ERROR);
        }
    }

    @Override
    public OverviewStatsResp overview() {
        long userCount = adminMapper.countAllUsers();
        long sessionCount = adminMapper.countAllSessions();
        long completedCount = adminMapper.countCompletedSessions();
        BigDecimal avg = adminMapper.avgReportScore();
        long questionCount = adminMapper.countQuestions();
        double completionRate = sessionCount > 0
                ? Math.round(completedCount * 1000.0 / sessionCount) / 10.0 : 0.0;
        double averageScore = avg == null ? 0.0 : avg.doubleValue();

        OverviewStatsResp resp = new OverviewStatsResp();
        resp.setUserCount(userCount);
        resp.setSessionCount(sessionCount);
        resp.setCompletedCount(completedCount);
        resp.setCompletionRate(completionRate);
        resp.setAverageScore(averageScore);
        resp.setQuestionCount(questionCount);
        return resp;
    }

    @Override
    public SessionTrendResp sessionTrend(int days) {
        int d = days < 1 ? 7 : Math.min(days, 90);
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.minusDays(d).atStartOfDay();
        List<String> dates = new ArrayList<>();
        Map<String, TrendRow> rowMap = adminMapper.sessionTrend(start).stream()
                .collect(Collectors.toMap(TrendRow::getDay, r -> r, (a, b) -> a));
        List<Long> counts = new ArrayList<>();
        List<Double> avgScores = new ArrayList<>();
        for (int i = d - 1; i >= 0; i--) {
            String day = today.minusDays(i).format(DATE_FMT);
            dates.add(day);
            TrendRow row = rowMap.get(day);
            counts.add(row == null || row.getCnt() == null ? 0L : row.getCnt());
            avgScores.add(row != null && row.getAvgScore() != null ? row.getAvgScore().doubleValue() : 0.0);
        }

        Map<String, Long> dirCount = new LinkedHashMap<>();
        for (String dirs : adminMapper.recentDirections(start)) {
            if (dirs == null || dirs.isBlank()) {
                continue;
            }
            for (String dir : dirs.split(",")) {
                String trimmed = dir.trim();
                if (!trimmed.isBlank()) {
                    dirCount.merge(trimmed, 1L, Long::sum);
                }
            }
        }
        List<DirectionCount> distribution = dirCount.entrySet().stream().map(e -> {
            DirectionCount dc = new DirectionCount();
            dc.setDirection(e.getKey());
            dc.setLabel(DIRECTION_LABELS.getOrDefault(e.getKey(), e.getKey()));
            dc.setCount(e.getValue());
            return dc;
        }).collect(Collectors.toList());

        SessionTrendResp resp = new SessionTrendResp();
        resp.setDays(d);
        resp.setDates(dates);
        resp.setCounts(counts);
        resp.setAvgScores(avgScores);
        resp.setDirectionDistribution(distribution);
        return resp;
    }

    @Override
    public PageInfo<AiCallLogResp> aiCalls(PageQuery q, String bizType, Boolean success) {
        com.baomidou.mybatisplus.core.metadata.IPage<com.aimeeting.interview.ai.log.AiCallLogDO> ipage =
                aiCallLogService.page(q.safePageNum(), q.safePageSize(), bizType, success);
        return PageInfo.of(ipage, this::toCallLogResp);
    }

    @Override
    public AiHealthResp aiHealth() {
        AiHealthSnapshot snapshot = aiGuardService.health();
        AiHealthResp resp = new AiHealthResp();
        resp.setProvider(snapshot.getProvider());
        resp.setModel(snapshot.getModel());
        resp.setMock(snapshot.isMock());
        resp.setCircuitBreakerOpen(snapshot.isCircuitBreakerOpen());
        resp.setAvailable(!snapshot.isCircuitBreakerOpen());
        resp.setBulkheadInUse(snapshot.getBulkheadInUse());
        resp.setBulkheadTotal(snapshot.getBulkheadTotal());
        resp.setMessage(snapshot.isCircuitBreakerOpen() ? "AI 熔断已打开，请检查上游依赖" : "正常");
        // 触发一次近期质量快照（不消费返回值，保持与 Guard 互补）
        aiCallLogService.healthSnapshot(Duration.ofHours(24));
        return resp;
    }

    /* ------------------------------ 内部工具 ------------------------------ */

    private AdminUserResp toUserResp(UserReadDO u) {
        AdminUserResp resp = new AdminUserResp();
        resp.setId(u.getId());
        resp.setUsername(u.getUsername());
        resp.setNickname(u.getNickname());
        resp.setEmail(u.getEmail());
        resp.setRole(u.getRole());
        resp.setStatus(u.getStatus());
        resp.setCreatedAt(u.getCreateTime());
        resp.setLastLoginAt(u.getLastLoginAt());
        return resp;
    }

    private AiCallLogResp toCallLogResp(com.aimeeting.interview.ai.log.AiCallLogDO log) {
        AiCallLogResp resp = new AiCallLogResp();
        resp.setId(log.getId());
        resp.setUserId(log.getUserId());
        resp.setBizType(log.getBizType());
        resp.setProvider(log.getProvider());
        resp.setModel(log.getModel());
        resp.setRequestDigest(log.getRequestDigest());
        resp.setResponseDigest(log.getResponseDigest());
        resp.setPromptTokens(log.getPromptTokens());
        resp.setCompletionTokens(log.getCompletionTokens());
        resp.setCostMs(log.getCostMs());
        resp.setSuccess(log.getSuccess() != null && log.getSuccess() == 1);
        resp.setErrorType(log.getErrorType());
        resp.setErrorMsg(log.getErrorMsg());
        resp.setCreatedAt(log.getCreateTime());
        return resp;
    }
}

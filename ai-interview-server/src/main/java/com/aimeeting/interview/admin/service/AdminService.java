package com.aimeeting.interview.admin.service;

import com.aimeeting.interview.admin.api.io.resp.AiCallLogResp;
import com.aimeeting.interview.admin.api.io.resp.AiHealthResp;
import com.aimeeting.interview.admin.api.io.resp.AdminUserResp;
import com.aimeeting.interview.admin.api.io.resp.OverviewStatsResp;
import com.aimeeting.interview.admin.api.io.resp.SessionTrendResp;
import com.aimeeting.interview.common.convention.result.PageInfo;
import com.aimeeting.interview.common.convention.result.PageQuery;

/**
 * 管理端服务（M8）。
 */
public interface AdminService {

    /**
     * 用户分页列表（ADMIN）。
     */
    PageInfo<AdminUserResp> users(PageQuery q, String keyword);

    /**
     * 启用 / 禁用用户（status: 1 启用，0 禁用）。
     */
    void updateUserStatus(Long id, Integer status);

    /**
     * 平台总览统计。
     */
    OverviewStatsResp overview();

    /**
     * 指定天数内的会话趋势与方向分布。
     */
    SessionTrendResp sessionTrend(int days);

    /**
     * AI 调用日志分页（复用 ai 包 AiCallLogService，只读）。
     */
    PageInfo<AiCallLogResp> aiCalls(PageQuery q, String bizType, Boolean success);

    /**
     * AI 服务健康状态（provider / model / mock / 熔断 / 舱壁）。
     */
    AiHealthResp aiHealth();
}

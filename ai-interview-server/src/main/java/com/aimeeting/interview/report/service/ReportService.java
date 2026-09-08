package com.aimeeting.interview.report.service;

import com.aimeeting.interview.common.convention.result.PageInfo;
import com.aimeeting.interview.report.api.io.req.ReportPageQuery;
import com.aimeeting.interview.report.api.io.resp.ReportBriefResp;
import com.aimeeting.interview.report.api.io.resp.ReportDetailResp;

/**
 * 报告服务（M6）。
 */
public interface ReportService {

    /**
     * 生成报告（幂等：IdempotentStage.REPORT_GENERATE + uk_session）。
     *
     * @param userId    当前用户
     * @param sessionId 会话 ID
     * @return 报告 ID
     */
    Long generate(Long userId, Long sessionId);

    /**
     * 按会话查询报告（越权 → OWNERSHIP_DENIED；不存在返回 null）。
     */
    ReportDetailResp getBySession(Long userId, Long sessionId);

    /**
     * 按报告 ID 查询详情（越权 → OWNERSHIP_DENIED）。
     */
    ReportDetailResp getById(Long userId, Long reportId);

    /**
     * 当前用户的报告分页列表。
     */
    PageInfo<ReportBriefResp> page(Long userId, ReportPageQuery q);

    /**
     * 逻辑删除报告（越权 → OWNERSHIP_DENIED）。
     */
    void remove(Long userId, Long reportId);

    /**
     * 导出 Markdown 文本。
     */
    String exportMarkdown(Long userId, Long reportId);
}

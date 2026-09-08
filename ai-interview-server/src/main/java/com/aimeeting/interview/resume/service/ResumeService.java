package com.aimeeting.interview.resume.service;

import com.aimeeting.interview.common.convention.result.PageInfo;
import com.aimeeting.interview.common.convention.result.PageQuery;
import com.aimeeting.interview.resume.api.io.req.ResumeParseReq;
import com.aimeeting.interview.resume.api.io.resp.ResumeDetailResp;
import com.aimeeting.interview.resume.api.io.resp.ResumeResp;
import org.springframework.web.multipart.MultipartFile;

/**
 * 简历服务（M5）。
 */
public interface ResumeService {

    /**
     * 文本 → AI 解析 → 落库（幂等 clientToken）。
     *
     * @param userId 当前用户
     * @param req    解析请求
     * @return 新简历 ID
     */
    Long parse(Long userId, ResumeParseReq req);

    /**
     * 上传简历文件（TXT/MD/PDF，≤5MB）→ 抽文本 → 解析。
     *
     * @param userId 当前用户
     * @param file   上传文件
     * @param title  可选标题（为空时取文件名）
     * @return 新简历 ID
     */
    Long upload(Long userId, MultipartFile file, String title);

    /**
     * 当前用户的简历分页列表。
     */
    PageInfo<ResumeResp> page(Long userId, PageQuery q);

    /**
     * 简历详情（含解析结果），越权抛 OWNERSHIP_DENIED。
     */
    ResumeDetailResp detail(Long userId, Long id);

    /**
     * 逻辑删除简历（越权抛 OWNERSHIP_DENIED）。
     */
    void remove(Long userId, Long id);

    /**
     * 设为默认简历（同用户其余置 0）。
     */
    void markDefault(Long userId, Long id);

    /**
     * 生成供出题 prompt 注入的简历摘要（截断 800 字符）。
     */
    String digestForPrompt(Long userId, Long resumeId);
}

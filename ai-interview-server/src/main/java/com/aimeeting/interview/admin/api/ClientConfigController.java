package com.aimeeting.interview.admin.api;

import com.aimeeting.interview.admin.api.io.resp.ClientConfig;
import com.aimeeting.interview.common.convention.result.Result;
import com.aimeeting.interview.common.convention.result.Results;
import com.aimeeting.interview.config.AiProperties;
import com.aimeeting.interview.config.InterviewProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 前端公开配置接口（/api/config/client，已在 permit-paths 放行，无需登录）。
 *
 * <p>返回前端初始化所需的业务规则与 AI 模式开关。
 */
@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
@Tag(name = "公开配置", description = "前端初始化配置")
public class ClientConfigController {

    private final InterviewProperties interviewProperties;

    private final AiProperties aiProperties;

    @Value("${spring.application.name:AI Interview}")
    private String appName;

    @GetMapping("/client")
    @Operation(summary = "前端公开配置", description = "mock 模式 / 题量范围 / 答案长度限制 / 应用名")
    public Result<ClientConfig> client() {
        ClientConfig config = new ClientConfig();
        config.setMockMode(aiProperties.isMockMode());
        config.setMinQuestion(interviewProperties.getMinQuestionCount());
        config.setMaxQuestion(interviewProperties.getMaxQuestionCount());
        config.setDefaultQuestion(interviewProperties.getDefaultQuestionCount());
        config.setMaxFollowUp(interviewProperties.getMaxFollowUp());
        config.setAnswerMinLength(interviewProperties.getAnswerMinLength());
        config.setAnswerMaxLength(interviewProperties.getAnswerMaxLength());
        config.setAppName(appName);
        return Results.success(config);
    }
}

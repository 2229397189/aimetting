package com.aimeeting.interview.admin.api.io.resp;

import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * AI 调用日志项（与前端 AiCallLogResp 对应）。
 */
@Data
public class AiCallLogResp implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long userId;

    private String bizType;

    /** 业务 Agent 维度（M3，可为 null 表示未知）。 */
    private String agentId;

    private String provider;

    private String model;

    private String requestDigest;

    private String responseDigest;

    private Integer promptTokens;

    private Integer completionTokens;

    private Long costMs;

    private Boolean success;

    private String errorType;

    private String errorMsg;

    private LocalDateTime createdAt;
}

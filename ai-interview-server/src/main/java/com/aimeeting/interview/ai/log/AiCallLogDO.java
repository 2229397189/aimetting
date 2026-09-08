package com.aimeeting.interview.ai.log;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * AI 调用日志（脱敏）：请求/响应摘要 + token 统计 + 成败标记。
 */
@Data
@TableName("t_ai_call_log")
public class AiCallLogDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String bizType;
    private String provider;
    private String model;
    private String requestDigest;
    private String responseDigest;
    private Integer promptTokens;
    private Integer completionTokens;
    private Long costMs;
    private Integer success;
    private String errorType;
    private String errorMsg;
    private String requestId;
    private LocalDateTime createTime;
}

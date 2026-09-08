package com.aimeeting.interview.ai.model;

import lombok.Builder;
import lombok.Data;

/**
 * AI 健康快照（管理端 {@code /api/admin/ai/health} 的返回体，与前端 {@code AiHealthResp} 对齐）。
 */
@Data
@Builder
public class AiHealthSnapshot {

    /** 供应商名：deepseek | mock。 */
    private String provider;

    /** 当前模型。 */
    private String model;

    /** 是否 Mock 模式。 */
    private boolean mock;

    /** 供应商可用性（apiKey 是否配置）。 */
    private boolean available;

    /** 熔断器状态：CLOSED | OPEN | HALF_OPEN。 */
    private String circuitBreaker;

    /** 熔断是否已打开。 */
    private boolean circuitBreakerOpen;

    /** 舱壁已占用配额。 */
    private int bulkheadInUse;

    /** 舱壁总配额。 */
    private int bulkheadTotal;

    /** 人类可读说明。 */
    private String message;
}

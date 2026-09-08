package com.aimeeting.interview.ai.guard;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 健康快照：熔断 / 舱壁 / 调用质量（成功率 / 平均耗时 / 失败分布）。
 *
 * <p>由 {@link AiGuardService#health()} 与 {@code AiCallLogService#healthSnapshot} 共同填充，
 * 供管理端 {@code GET /api/admin/ai/health} 展示。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiHealthSnapshot implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 当前 provider 名：deepseek / mock。 */
    private String provider;

    /** 当前模型。 */
    private String model;

    /** 是否 mock 模式。 */
    private boolean mock;

    /** 是否有任意阶段的熔断器处于 OPEN。 */
    private boolean circuitBreakerOpen;

    /** 各阶段的熔断状态（stage.name -> CLOSED/OPEN/HALF_OPEN）。 */
    private Map<String, String> circuitBreakerStates = new LinkedHashMap<>();

    /** 舱壁已占用并发数。 */
    private int bulkheadInUse;

    /** 舱壁总并发数。 */
    private int bulkheadTotal;

    /** 最近窗口内成功率（0~1），无数据时为 null。 */
    private Double successRate;

    /** 平均耗时（毫秒）。 */
    private Long avgCostMs;

    /** 失败分布：errorType -> 次数。 */
    private Map<String, Long> failureDistribution = new LinkedHashMap<>();

    /** 统计窗口内的总调用次数。 */
    private long totalCalls;
}

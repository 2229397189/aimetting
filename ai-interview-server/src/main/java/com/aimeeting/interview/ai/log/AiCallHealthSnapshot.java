package com.aimeeting.interview.ai.log;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Data;

/**
 * AI 调用健康统计（按时间窗口聚合 t_ai_call_log）。
 */
@Data
public class AiCallHealthSnapshot {

    /** 统计窗口（分钟）。 */
    private long windowMinutes = 0L;

    /** 总调用次数。 */
    private long total = 0L;

    /** 成功次数。 */
    private long successCount = 0L;

    /** 失败次数。 */
    private long failureCount = 0L;

    /** 成功率（百分数，保留 2 位小数）。 */
    private double successRate = 0.0;

    /** 平均耗时（毫秒）。 */
    private long avgCostMs = 0L;

    /** 失败分布（按错误类型）。 */
    private Map<String, Long> failureByType = new LinkedHashMap<>();

    /** 失败分布（按业务类型）。 */
    private Map<String, Long> failureByBizType = new LinkedHashMap<>();
}

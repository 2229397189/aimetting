package com.aimeeting.interview.admin.api.io.resp;

import java.io.Serializable;
import lombok.Data;

/**
 * AI 服务健康状态（与前端 AiHealthResp 对应）。
 */
@Data
public class AiHealthResp implements Serializable {

    private static final long serialVersionUID = 1L;

    private String provider;

    private String model;

    private Boolean mock;

    private Boolean available;

    private Boolean circuitBreakerOpen;

    private Integer bulkheadInUse;

    private Integer bulkheadTotal;

    private String message;
}

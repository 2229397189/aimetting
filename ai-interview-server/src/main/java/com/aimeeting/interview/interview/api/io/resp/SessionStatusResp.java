package com.aimeeting.interview.interview.api.io.resp;

import java.math.BigDecimal;
import lombok.Data;

/**
 * 会话状态（轻量轮询，SSE 断线补偿，与前端 {@code SessionStatusResp} 一致）。
 */
@Data
public class SessionStatusResp {

    private Long id;

    private String status;

    private Integer currentIndex;

    private Integer totalQuestion;

    private BigDecimal score;

    /** 报告 ID（M6 接入前为 null）。 */
    private Long reportId;
}

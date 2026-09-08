package com.aimeeting.interview.interview.service.sse;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SSE 事件信封（统一外层，ARCHITECTURE.md §6.2）。
 *
 * <p>字段严格对齐前端 {@code types/index.ts#SseEnvelope}：
 * <pre>
 * { type, seq, requestId?, sessionId?, questionNo?, degraded?, timestamp?, payload }
 * </pre>
 * 注意：前端没有 {@code source} 字段，此处已对齐移除。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SseEnvelope implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 事件类型（SseEventType 枚举名）。 */
    private String type;

    /** 连接内单调递增序号。 */
    private long seq;

    /** 与 HTTP 响应头 X-Request-Id 一致。 */
    private String requestId;

    /** 会话 ID。 */
    private Long sessionId;

    /** 题号（1-based）。 */
    private Integer questionNo;

    /** true = 本次结果来自降级。 */
    private boolean degraded;

    /** 服务端时间戳（ms）。 */
    private long timestamp;

    /** 各事件不同的 payload。 */
    private Object payload;

    /**
     * 构造信封（无 source 字段）。
     *
     * @param type         事件类型
     * @param seq          自增序号
     * @param requestId    链路 ID
     * @param sessionId    会话 ID
     * @param questionNo   题号
     * @param degraded     是否降级
     * @param payload      载荷
     * @return 信封
     */
    public static SseEnvelope of(SseEventType type, long seq, String requestId, Long sessionId,
                                 Integer questionNo, boolean degraded, Object payload) {
        return SseEnvelope.builder()
                .type(type == null ? null : type.name())
                .seq(seq)
                .requestId(requestId)
                .sessionId(sessionId)
                .questionNo(questionNo)
                .degraded(degraded)
                .timestamp(System.currentTimeMillis())
                .payload(payload)
                .build();
    }
}

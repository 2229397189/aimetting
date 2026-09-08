package com.aimeeting.interview.interview.service.sse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * progress 事件 payload（长等待提示，与前端 {@code ProgressPayload} 逐字一致）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgressPayload {

    private String text;
}

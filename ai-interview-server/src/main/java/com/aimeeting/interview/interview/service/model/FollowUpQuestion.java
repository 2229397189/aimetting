package com.aimeeting.interview.interview.service.model;

import lombok.Data;

/**
 * 追问生成结果（{@code FollowUpAgent} 的输出）。
 *
 * <p>{@code probeType} 取值：{@code "DEEPEN"} 深探针 / {@@code "SHIFT"} 改问另一子方向 /
 * {@code "STOP"} 已达上限不再追问。</p>
 */
@Data
public class FollowUpQuestion {

    /** 生成的追问问题文本（STOP 时为空串）。 */
    private String question;

    /** 探针类型：DEEPEN | SHIFT | STOP。 */
    private String probeType;

    /** 本次追问后的累计追问次数。 */
    private int followUpCount;

    public FollowUpQuestion() {
    }

    public FollowUpQuestion(String question, String probeType, int followUpCount) {
        this.question = question;
        this.probeType = probeType;
        this.followUpCount = followUpCount;
    }
}

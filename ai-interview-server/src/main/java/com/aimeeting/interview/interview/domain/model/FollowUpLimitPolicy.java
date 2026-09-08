package com.aimeeting.interview.interview.domain.model;

/**
 * 追问次数上限策略（BR-01）。
 *
 * <p>默认值由配置 {@code ai-interview.interview.max-follow-up} 注入，缺省 2 次。
 */
public final class FollowUpLimitPolicy {

    /** 默认最大追问次数。 */
    public static final int DEFAULT_MAX = 2;

    private FollowUpLimitPolicy() {
    }

    /**
     * 是否还能继续追问。
     *
     * @param currentCount 已追问次数
     * @param max          上限（&lt;=0 时回退为 {@link #DEFAULT_MAX}）
     * @return 可以继续追问返回 true
     */
    public static boolean canFollowUp(int currentCount, int max) {
        int safeMax = max <= 0 ? DEFAULT_MAX : max;
        return currentCount < safeMax;
    }
}

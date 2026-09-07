package com.aimeeting.interview.auth.domain;

import java.time.Duration;

/**
 * 登录失败锁定策略（BR-20）：同一账号连续失败 {@value #MAX_FAIL} 次锁定 5 分钟。
 *
 * <p>纯领域对象，不依赖 Spring；计数存储由 {@code CacheService} 承担（Caffeine）。
 */
public final class LoginFailPolicy {

    /** 最大连续失败次数。 */
    public static final int MAX_FAIL = 5;

    /** 锁定时长。 */
    public static final Duration LOCK_DURATION = Duration.ofMinutes(5);

    /** 缓存键前缀。 */
    private static final String CACHE_PREFIX = "login:fail:";

    private LoginFailPolicy() {
    }

    /**
     * 判断是否应锁定账号。
     *
     * @param failCount 当前连续失败次数
     * @return 达到或超过阈值返回 true
     */
    public static boolean shouldLock(int failCount) {
        return failCount >= MAX_FAIL;
    }

    /**
     * 构造登录失败计数的缓存键。
     *
     * @param loginName 登录名（用户名或邮箱）
     * @return 缓存键
     */
    public static String cacheKey(String loginName) {
        return CACHE_PREFIX + (loginName == null ? "" : loginName.trim().toLowerCase());
    }
}

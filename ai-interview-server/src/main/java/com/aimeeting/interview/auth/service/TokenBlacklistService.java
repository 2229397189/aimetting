package com.aimeeting.interview.auth.service;

import com.aimeeting.interview.common.cache.CacheService;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Token 黑名单：退出登录后把 accessToken 的 {@code jti} 加入短期黑名单（U-05）。
 *
 * <p>实现：Caffeine 缓存，TTL = 令牌剩余有效期，令牌自然过期后黑名单条目自动失效，
 * 无需额外清理。禁用了 Redis，因此黑名单仅单实例生效（横向扩展时需替换为 Redis 实现）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    /** 黑名单键前缀。 */
    private static final String KEY_PREFIX = "jti:blacklist:";

    private final CacheService cacheService;

    /**
     * 加入黑名单。
     *
     * @param jti           JWT ID
     * @param remainSeconds 剩余有效秒数
     */
    public void blacklist(String jti, long remainSeconds) {
        if (jti == null || jti.isBlank()) {
            return;
        }
        long ttl = remainSeconds <= 0L ? 60L : remainSeconds;
        cacheService.put(KEY_PREFIX + jti, "1", Duration.ofSeconds(ttl));
        log.info("[TokenBlacklist] 令牌已加入黑名单, jti={}, ttl={}s", jti, ttl);
    }

    /**
     * 判断令牌是否已被拉黑。
     *
     * @param jti JWT ID
     * @return 已拉黑返回 true
     */
    public boolean isBlacklisted(String jti) {
        if (jti == null || jti.isBlank()) {
            return false;
        }
        return cacheService.get(KEY_PREFIX + jti, String.class).isPresent();
    }
}

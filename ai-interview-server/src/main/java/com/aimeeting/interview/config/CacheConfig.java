package com.aimeeting.interview.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.List;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Spring Cache 配置。
 *
 * <p>说明：业务侧的缓存能力统一走 {@code CacheService} 抽象（Redis 可替换），
 * 此处仅提供 Spring {@code @Cacheable} 体系所需的 {@link CacheManager}，
 * 用于少量声明式缓存场景。
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /** 默认缓存过期时间。 */
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(30);

    /** 缓存名称列表。 */
    private static final List<String> CACHE_NAMES = List.of("user", "question", "session", "config");

    /** 最大条目数。 */
    private static final long MAXIMUM_SIZE = 5_000L;

    /**
     * Caffeine 缓存管理器。
     *
     * @return 缓存管理器
     */
    @Bean
    @Primary
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCacheNames(CACHE_NAMES);
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(MAXIMUM_SIZE)
                .expireAfterWrite(DEFAULT_TTL)
                .recordStats());
        return cacheManager;
    }
}

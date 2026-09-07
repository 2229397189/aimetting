package com.aimeeting.interview.common.cache;

import com.aimeeting.interview.common.util.JsonUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 基于 Caffeine 的 {@link CacheService} 单机实现。
 *
 * <p>关键设计：
 * <ul>
 *   <li>使用 <b>可变过期策略</b>（{@link Expiry}）：TTL 随每次写入单独指定，
 *       从而同时支持「幂等处理中键 60s」与「回放键 24h」等不同生命周期。</li>
 *   <li>{@link #putIfAbsent} 走 {@code asMap().putIfAbsent}，Caffeine 保证原子性，
 *       这是幂等三态判定的正确性基础。</li>
 *   <li>容量上限 10_000，配合 LRU 驱逐，避免单机内存被缓存打爆。</li>
 * </ul>
 */
@Slf4j
@Service
public class CaffeineCacheService implements CacheService {

    /** 最大缓存条目数。 */
    private static final long MAXIMUM_SIZE = 10_000L;

    /** 兜底 TTL（调用方未指定时使用）。 */
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(5);

    private final ConcurrentMap<String, CacheValue> cache;

    /** 默认构造：便于单测直接使用，无需 Spring 容器。 */
    public CaffeineCacheService() {
        this.cache = Caffeine.newBuilder()
                .maximumSize(MAXIMUM_SIZE)
                .expireAfter(new CacheExpiry())
                .<String, CacheValue>build()
                .asMap();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String key, Class<T> type) {
        CacheValue holder = cache.get(key);
        if (holder == null || holder.isExpired()) {
            if (holder != null) {
                cache.remove(key, holder);
            }
            return Optional.empty();
        }
        Object value = holder.value();
        if (value == null) {
            return Optional.empty();
        }
        if (type.isInstance(value)) {
            return Optional.of((T) value);
        }
        // 类型不一致（例如经 JSON 序列化后回填）时尝试按目标类型重新解析
        T converted = JsonUtil.parse(JsonUtil.toJson(value), type);
        return Optional.ofNullable(converted);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String key, TypeReference<T> type) {
        CacheValue holder = cache.get(key);
        if (holder == null || holder.isExpired()) {
            if (holder != null) {
                cache.remove(key, holder);
            }
            return Optional.empty();
        }
        Object value = holder.value();
        if (value == null) {
            return Optional.empty();
        }
        if (type.getType() instanceof Class<?> rawType && rawType.isInstance(value)) {
            return Optional.of((T) rawType.cast(value));
        }
        T converted = JsonUtil.parse(JsonUtil.toJson(value), type);
        return Optional.ofNullable(converted);
    }

    @Override
    public void put(String key, Object value, Duration ttl) {
        long ttlMillis = ttl == null ? DEFAULT_TTL.toMillis() : ttl.toMillis();
        cache.put(key, CacheValue.of(value, ttlMillis));
    }

    @Override
    public boolean putIfAbsent(String key, String value, Duration ttl) {
        long ttlMillis = ttl == null ? DEFAULT_TTL.toMillis() : ttl.toMillis();
        return cache.putIfAbsent(key, CacheValue.of(value, ttlMillis)) == null;
    }

    @Override
    public boolean remove(String key) {
        return cache.remove(key) != null;
    }

    @Override
    public long increment(String key, Duration ttl) {
        long ttlMillis = ttl == null ? DEFAULT_TTL.toMillis() : ttl.toMillis();
        CacheValue updated = cache.compute(key, (k, existing) -> {
            long current = 0L;
            if (existing != null && !existing.isExpired() && existing.value() instanceof Number number) {
                current = number.longValue();
            }
            return CacheValue.of(current + 1L, ttlMillis);
        });
        return updated == null ? 0L : ((Number) updated.value()).longValue();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getOrLoad(String key, Duration ttl, Supplier<T> loader) {
        CacheValue holder = cache.get(key);
        if (holder != null && !holder.isExpired()) {
            return (T) holder.value();
        }
        T loaded = loader.get();
        if (loaded != null) {
            put(key, loaded, ttl);
        }
        return loaded;
    }

    /**
     * 缓存值包装：保存 TTL 与创建时间，使 Caffeine 的可变过期策略可精确生效。
     *
     * @param value      实际值
     * @param ttlMillis  存活时长（毫秒）
     * @param createAt   写入时刻（毫秒时间戳）
     */
    record CacheValue(Object value, long ttlMillis, long createAt) {

        static CacheValue of(Object value, long ttlMillis) {
            return new CacheValue(value, ttlMillis, System.currentTimeMillis());
        }

        boolean isExpired() {
            return System.currentTimeMillis() - createAt >= ttlMillis;
        }

        long remainMillis() {
            long remain = ttlMillis - (System.currentTimeMillis() - createAt);
            return remain < 0L ? 0L : remain;
        }
    }

    /**
     * 可变过期策略：按每条记录的自定义 TTL 计算剩余存活时间。
     */
    static class CacheExpiry implements Expiry<String, CacheValue> {

        @Override
        public long expireAfterCreate(String key, CacheValue value, long currentTime) {
            return toNanos(value.ttlMillis());
        }

        @Override
        public long expireAfterUpdate(String key, CacheValue value, long currentTime, long currentDuration) {
            return toNanos(value.ttlMillis());
        }

        @Override
        public long expireAfterRead(String key, CacheValue value, long currentTime, long currentDuration) {
            return toNanos(value.remainMillis());
        }

        private static long toNanos(long millis) {
            return millis <= 0L ? 1L : millis * 1_000_000L;
        }
    }
}

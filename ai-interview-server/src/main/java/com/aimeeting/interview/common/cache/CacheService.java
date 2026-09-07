package com.aimeeting.interview.common.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * 缓存服务抽象。
 *
 * <p>设计前提：Redis 不可用，因此提供 Caffeine 单机实现
 * {@link CaffeineCacheService}；接口本身与 Redis 语义对齐（TTL、原子占位、自增），
 * 后续引入 Redis 时新增一个实现类替换 Bean 即可，业务代码零改动。
 *
 * <p>承载场景：登录失败计数、Token 黑名单、幂等双键、Single-flight 之外的热点数据。
 */
public interface CacheService {

    /**
     * 读取缓存值。
     *
     * @param key  缓存键
     * @param type 目标类型
     * @param <T>  目标类型
     * @return 缓存值，不存在或已过期返回 {@link Optional#empty()}
     */
    <T> Optional<T> get(String key, Class<T> type);

    /**
     * 读取复杂泛型缓存值（如回放的 {@code Result<XxxResp>}）。
     *
     * @param key  缓存键
     * @param type 目标类型引用
     * @param <T>  目标类型
     * @return 缓存值，不存在或已过期返回 {@link Optional#empty()}
     */
    <T> Optional<T> get(String key, TypeReference<T> type);

    /**
     * 写入缓存。
     *
     * @param key   缓存键
     * @param value 缓存值
     * @param ttl   过期时间
     */
    void put(String key, Object value, Duration ttl);

    /**
     * 原子写入（仅当键不存在时成功），幂等「处理中键」的核心。
     *
     * @param key   缓存键
     * @param value 占位值
     * @param ttl   过期时间
     * @return 抢占成功返回 true，键已存在返回 false
     */
    boolean putIfAbsent(String key, String value, Duration ttl);

    /**
     * 删除缓存。
     *
     * @param key 缓存键
     * @return 删除前存在返回 true
     */
    boolean remove(String key);

    /**
     * 原子自增，用于登录失败计数与限流；首次调用返回 1。
     *
     * @param key 缓存键
     * @param ttl 过期时间（每次自增刷新）
     * @return 自增后的值
     */
    long increment(String key, Duration ttl);

    /**
     * 读穿：缓存命中直接返回，未命中执行 loader 并回写。
     *
     * @param key    缓存键
     * @param ttl    过期时间
     * @param loader 数据加载器
     * @param <T>    数据类型
     * @return 缓存值或加载值
     */
    <T> T getOrLoad(String key, Duration ttl, Supplier<T> loader);
}

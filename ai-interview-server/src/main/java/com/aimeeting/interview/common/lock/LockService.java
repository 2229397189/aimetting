package com.aimeeting.interview.common.lock;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * 分布式锁抽象。
 *
 * <p>Redis 不可用环境下由 {@link JvmLockService}（{@code ConcurrentHashMap} +
 * {@code ReentrantLock}）实现单机语义；接口与 Redisson 语义一致，后续可直接替换实现。
 */
public interface LockService {

    /**
     * 尝试加锁。
     *
     * @param key   锁键
     * @param lease 租约时长，超时自动释放，避免死锁
     * @return 加锁成功返回 true
     */
    boolean tryLock(String key, Duration lease);

    /**
     * 释放锁。
     *
     * @param key 锁键
     */
    void unlock(String key);

    /**
     * 带超时等待的执行模板：在锁内执行 supplier，执行完自动释放。
     *
     * @param key      锁键
     * @param wait     最长等待时间
     * @param lease    租约时长
     * @param supplier 锁内逻辑
     * @param <T>      返回类型
     * @return supplier 的执行结果
     */
    <T> T withLock(String key, Duration wait, Duration lease, Supplier<T> supplier);
}

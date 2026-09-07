package com.aimeeting.interview.common.concurrent.singleflight;

import com.aimeeting.interview.common.convention.errorcode.BaseErrorCode;
import com.aimeeting.interview.common.convention.exception.ServiceException;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 基于 {@code ConcurrentHashMap} + {@code CompletableFuture} 的 JVM 版 Single-flight。
 *
 * <p>核心流程：
 * <ol>
 *   <li>调用方以 {@code group:key} 为键做一次原子 {@code putIfAbsent}：
 *       成功者成为 <b>owner</b>，失败者成为 <b>follower</b>。</li>
 *   <li>owner 执行 {@code loader}：成功 {@code complete(v)}；异常
 *       {@code completeExceptionally(e)}，使所有 follower 立即感知失败而不是死等。</li>
 *   <li>无论成功或失败，owner 在 {@code finally} 中移除自己的 entry（用
 *       {@code remove(key, entry)} 避免误删后续新建的 entry）。</li>
 *   <li>follower 在 {@code future.get(waitTimeout)} 上等待：
 *       超时抛 {@link FlightWaitTimeoutException}（-&gt; C0503）；异常原样传播。</li>
 *   <li>entry 数量超过 256 时清理已过期的条目，防止内存泄漏。</li>
 * </ol>
 */
@Slf4j
@Service
public class CaffeineSingleFlight implements SingleFlight {

    /** 触发过期清理的容量阈值。 */
    private static final int CLEAN_THRESHOLD = 256;

    private final ConcurrentMap<String, FlightEntry> flights = new ConcurrentHashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public <T> T execute(String group, String key, Duration waitTimeout, Callable<T> loader) throws Exception {
        String flightKey = buildKey(group, key);
        long waitMillis = waitTimeout == null ? 120_000L : waitTimeout.toMillis();
        // entry 的兜底过期时间 = follower 最长等待 + 1s 缓冲，供清理线程识别残留
        FlightEntry candidate = FlightEntry.newEntry(waitMillis + 1_000L);

        FlightEntry existing = flights.putIfAbsent(flightKey, candidate);
        boolean owner = existing == null;
        if (owner) {
            return runAsOwner(flightKey, candidate, loader);
        }
        return (T) waitAsFollower(flightKey, existing, waitMillis);
    }

    /**
     * owner 分支：真实执行 loader，并把结果/异常广播给所有 follower。
     */
    private <T> T runAsOwner(String flightKey, FlightEntry entry, Callable<T> loader) throws Exception {
        try {
            T value = loader.call();
            entry.future().complete(value);
            return value;
        } catch (Throwable t) {
            entry.future().completeExceptionally(t);
            throw asException(t);
        } finally {
            flights.remove(flightKey, entry);
            cleanIfNeeded();
        }
    }

    /**
     * follower 分支：等待 owner 的 future，处理超时与异常传播。
     */
    private Object waitAsFollower(String flightKey, FlightEntry entry, long waitMillis) throws Exception {
        try {
            return entry.future().get(waitMillis, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            flights.remove(flightKey, entry);
            throw new FlightWaitTimeoutException("Single-flight 等待超时: key=" + flightKey);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            throw asException(cause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceException("Single-flight 等待被中断", e, BaseErrorCode.SERVICE_ERROR);
        }
    }

    /**
     * 把 loader 抛出的 Throwable 归一为可抛出的 Exception。
     */
    private static Exception asException(Throwable t) {
        if (t instanceof Exception exception) {
            return exception;
        }
        if (t instanceof Error error) {
            throw error;
        }
        return new ServiceException(String.valueOf(t.getMessage()), t, BaseErrorCode.SERVICE_ERROR);
    }

    /**
     * 构造 flight 键。
     *
     * @param group 业务分组
     * @param key   请求指纹
     * @return 复合键
     */
    private static String buildKey(String group, String key) {
        return (group == null ? "default" : group) + ":" + (key == null ? "" : key);
    }

    /** 条目数超阈值时清理过期 entry。 */
    private void cleanIfNeeded() {
        if (flights.size() > CLEAN_THRESHOLD) {
            long now = System.currentTimeMillis();
            flights.values().removeIf(entry -> entry.expireAtMillis() <= now);
        }
    }

    /**
     * 一次 flight 的共享句柄。
     *
     * @param future        owner 完成后写入结果 / 异常的 future
     * @param expireAtMillis 兜底过期时间戳，用于清理残留
     */
    record FlightEntry(CompletableFuture<Object> future, long expireAtMillis) {

        static FlightEntry newEntry(long ttlMillis) {
            return new FlightEntry(new CompletableFuture<>(), System.currentTimeMillis() + ttlMillis);
        }
    }
}

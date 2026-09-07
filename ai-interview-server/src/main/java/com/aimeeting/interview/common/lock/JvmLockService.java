package com.aimeeting.interview.common.lock;

import com.aimeeting.interview.common.convention.errorcode.BaseErrorCode;
import com.aimeeting.interview.common.convention.exception.ServiceException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * JVM 级锁实现：{@code ConcurrentHashMap<String, ReentrantLock>}。
 *
 * <p>关键点：
 * <ul>
 *   <li>锁对象带 <b>过期时间</b>（lease），超过租约强制解锁，防止业务异常导致锁永久占用。</li>
 *   <li>{@link #withLock} 在 {@code finally} 中释放锁，保证异常路径也能解锁。</li>
 *   <li>定期清理空闲锁对象，避免 map 无限膨胀。</li>
 * </ul>
 */
@Slf4j
@Service
public class JvmLockService implements LockService {

    /** 触发清理的阈值：锁数量超过该值时顺带清理已释放且空闲的锁。 */
    private static final int CLEAN_THRESHOLD = 1_000;

    /** 默认租约时长。 */
    private static final Duration DEFAULT_LEASE = Duration.ofSeconds(30);

    private final Map<String, LockHolder> locks = new ConcurrentHashMap<>();

    @Override
    public boolean tryLock(String key, Duration lease) {
        LockHolder holder = locks.computeIfAbsent(key, k -> new LockHolder());
        boolean acquired = holder.tryLock(lease == null ? DEFAULT_LEASE : lease);
        if (!acquired && holder.isLeaseExpired()) {
            // 租约过期但未被释放（持锁线程异常退出），抢占式接管
            holder.forceUnlock();
            acquired = holder.tryLock(lease == null ? DEFAULT_LEASE : lease);
        }
        if (locks.size() > CLEAN_THRESHOLD) {
            cleanIdle();
        }
        return acquired;
    }

    @Override
    public void unlock(String key) {
        LockHolder holder = locks.get(key);
        if (holder != null) {
            holder.unlock();
        }
    }

    @Override
    public <T> T withLock(String key, Duration wait, Duration lease, Supplier<T> supplier) {
        long deadline = System.currentTimeMillis() + (wait == null ? 0L : wait.toMillis());
        Duration realLease = lease == null ? DEFAULT_LEASE : lease;
        while (true) {
            if (tryLock(key, realLease)) {
                try {
                    return supplier.get();
                } finally {
                    unlock(key);
                }
            }
            if (System.currentTimeMillis() >= deadline) {
                throw new ServiceException("获取锁失败，请稍后重试", BaseErrorCode.SERVICE_ERROR);
            }
            sleepQuietly(20L);
        }
    }

    /** 清理未被持有且已过期的锁对象。 */
    private void cleanIdle() {
        locks.entrySet().removeIf(entry -> {
            LockHolder holder = entry.getValue();
            return !holder.isLocked() && holder.isLeaseExpired();
        });
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 带租约的锁持有者。
     */
    static final class LockHolder {

        private final ReentrantLock lock = new ReentrantLock();

        private volatile long expireAtMillis = 0L;

        boolean tryLock(Duration lease) {
            try {
                if (lock.tryLock(200L, TimeUnit.MILLISECONDS)) {
                    expireAtMillis = System.currentTimeMillis() + lease.toMillis();
                    return true;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return false;
        }

        void unlock() {
            if (lock.isHeldByCurrentThread()) {
                expireAtMillis = 0L;
                lock.unlock();
            }
        }

        void forceUnlock() {
            if (lock.isLocked()) {
                lock.unlock();
            }
            expireAtMillis = 0L;
        }

        boolean isLocked() {
            return lock.isLocked();
        }

        boolean isLeaseExpired() {
            return expireAtMillis > 0L && System.currentTimeMillis() > expireAtMillis;
        }
    }
}

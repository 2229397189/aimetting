package com.aimeeting.interview.ai.guard;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;

/**
 * 滑动窗口熔断器（BR-10）：窗口内失败率 >= 阈值则开断，openDuration 后进入半开试探。
 */
@Slf4j
public class CircuitBreaker {

    public enum State {
        CLOSED, OPEN, HALF_OPEN
    }

    private final int windowSize;
    private final double failureRate;
    private final long openMillis;

    private final AtomicInteger attempts = new AtomicInteger(0);
    private final AtomicInteger failures = new AtomicInteger(0);
    private final AtomicLong openedAt = new AtomicLong(0);

    public CircuitBreaker(int windowSize, double failureRate, Duration openDuration) {
        this.windowSize = windowSize;
        this.failureRate = failureRate;
        this.openMillis = openDuration.toMillis();
    }

    public synchronized boolean allowRequest() {
        if (openedAt.get() == 0) {
            return true;
        }
        if (System.currentTimeMillis() - openedAt.get() >= openMillis) {
            // 半开试探：重置窗口，允许这次请求探活
            openedAt.set(0);
            attempts.set(0);
            failures.set(0);
            log.info("[CircuitBreaker] 进入半开探测");
            return true;
        }
        return false;
    }

    public synchronized void recordSuccess() {
        attempts.incrementAndGet();
        failures.set(0);
        if (attempts.get() >= windowSize) {
            attempts.set(0);
        }
    }

    public synchronized void recordFailure() {
        attempts.incrementAndGet();
        failures.incrementAndGet();
        if (attempts.get() >= windowSize) {
            double rate = (double) failures.get() / attempts.get();
            if (rate >= failureRate) {
                openedAt.set(System.currentTimeMillis());
                log.warn("[CircuitBreaker] 失败率 {}/{} = {} 触发开断 {}ms", failures.get(), attempts.get(), rate, openMillis);
            }
            attempts.set(0);
            failures.set(0);
        }
    }

    public State state() {
        if (openedAt.get() == 0) {
            return State.CLOSED;
        }
        if (System.currentTimeMillis() - openedAt.get() >= openMillis) {
            return State.HALF_OPEN;
        }
        return State.OPEN;
    }

    public boolean isOpen() {
        return state() == State.OPEN;
    }
}

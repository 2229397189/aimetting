package com.aimeeting.interview.ai.guard;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 全局 AI 并发舱壁（BR-11）：限制同时进行的 AI 调用数量，保护下游与本地线程。
 */
@Slf4j
@Component
public class Bulkhead {

    private final Semaphore semaphore;
    private final int total;

    public Bulkhead(@Value("${ai.guard.bulkhead.total:20}") int total) {
        this.total = total;
        this.semaphore = new Semaphore(total);
    }

    /** 尝试获取一个并发配额（最多等待 200ms）。 */
    public boolean tryAcquire() {
        try {
            return semaphore.tryAcquire(200, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public void release() {
        semaphore.release();
    }

    public int available() {
        return semaphore.availablePermits();
    }

    public int total() {
        return total;
    }

    public int inUse() {
        return total - semaphore.availablePermits();
    }
}

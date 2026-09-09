package com.aimeeting.interview.ai.guard;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * AI 调用按用户限流（保护 token 成本与下游稳定性）。
 *
 * <p>采用「固定窗口计数」：每个用户在分钟窗口与日窗口内各有一个计数器，窗口到期自动归零。
 * 注意这里不能用 {@code expireAfterWrite} 直接当窗口——那样每次写入都会刷新过期时间，
 * 持续流量下计数永不清零，会导致用户被永久限流；因此由 {@link Window} 自行判断是否跨窗。</p>
 *
 * <p>单机 Caffeine 实现（与项目零中间件定位一致）；多实例部署时需在网关层再兜一层。</p>
 */
@Slf4j
@Component
public class AiRateLimiter {

    private final Cache<String, Window> minuteWindows;
    private final Cache<String, Window> dayWindows;
    private final int perMinute;
    private final int perDay;

    public AiRateLimiter(@Value("${ai-interview.ai.rate-limit-per-minute:30}") int perMinute,
                         @Value("${ai-interview.ai.rate-limit-per-day:300}") int perDay) {
        this.perMinute = perMinute;
        this.perDay = perDay;
        // 缓存仅用于自动清理，窗口语义由 Window 保证
        this.minuteWindows = Caffeine.newBuilder()
                .expireAfterAccess(Duration.ofMinutes(5))
                .maximumSize(10_000)
                .build();
        this.dayWindows = Caffeine.newBuilder()
                .expireAfterAccess(Duration.ofHours(30))
                .maximumSize(100_000)
                .build();
    }

    /**
     * 判断该用户本次 AI 调用是否被放行（放行即计数 +1）。
     *
     * @param userId 用户 ID，为空时归到匿名桶
     * @return true 放行；false 已超限
     */
    public boolean allow(Long userId) {
        if (perMinute <= 0 && perDay <= 0) {
            return true;
        }
        String key = String.valueOf(userId == null ? 0L : userId);
        if (perMinute > 0) {
            Window minute = minuteWindows.get(key, k -> new Window(60_000L));
            if (minute.incrementAndGet() > perMinute) {
                log.warn("[AiRateLimiter] 用户 {} 触发每分钟 AI 调用上限 {}", key, perMinute);
                return false;
            }
        }
        if (perDay > 0) {
            Window day = dayWindows.get(key, k -> new Window(24 * 60 * 60 * 1000L));
            if (day.incrementAndGet() > perDay) {
                log.warn("[AiRateLimiter] 用户 {} 触发每日 AI 调用上限 {}", key, perDay);
                return false;
            }
        }
        return true;
    }

    /**
     * 固定窗口计数器：跨窗自动归零。
     */
    private static final class Window {

        private final long windowMillis;
        private final AtomicInteger count = new AtomicInteger();
        private volatile long start;

        private Window(long windowMillis) {
            this.windowMillis = windowMillis;
            this.start = System.currentTimeMillis();
        }

        private synchronized int incrementAndGet() {
            long now = System.currentTimeMillis();
            if (now - start >= windowMillis) {
                start = now;
                count.set(0);
            }
            return count.incrementAndGet();
        }
    }
}

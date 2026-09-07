package com.aimeeting.interview.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aimeeting.interview.common.concurrent.singleflight.CaffeineSingleFlight;
import com.aimeeting.interview.common.concurrent.singleflight.FlightWaitTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link CaffeineSingleFlight} 单测：并发去重、异常传播、follower 等待超时。
 */
class CaffeineSingleFlightTest {

    private CaffeineSingleFlight singleFlight;

    @BeforeEach
    void setUp() {
        singleFlight = new CaffeineSingleFlight();
    }

    @Test
    @DisplayName("20 个线程并发同 key 请求，loader 只执行 1 次，全部拿到同一份结果")
    void concurrentSameKeyExecutesLoaderOnce() throws Exception {
        int threadCount = 20;
        AtomicInteger loaderCount = new AtomicInteger(0);
        CountDownLatch allArrived = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try {
            List<Future<String>> futures = new ArrayList<>(threadCount);
            Callable<String> loader = () -> {
                loaderCount.incrementAndGet();
                // 让所有 follower 都能在 owner 完成前进入等待
                Thread.sleep(500L);
                return "SHARED_RESULT";
            };
            for (int i = 0; i < threadCount; i++) {
                futures.add(executor.submit(() -> {
                    allArrived.countDown();
                    return singleFlight.execute("QUESTION_GEN", "key-A",
                            Duration.ofSeconds(10), loader);
                }));
            }
            assertTrue(allArrived.await(5, TimeUnit.SECONDS), "所有任务应在 5s 内提交完成");

            List<String> results = new ArrayList<>(threadCount);
            for (Future<String> future : futures) {
                results.add(future.get(15, TimeUnit.SECONDS));
            }

            assertEquals(1, loaderCount.get(), "同 key 并发下 loader 只能执行 1 次");
            for (String result : results) {
                assertEquals("SHARED_RESULT", result, "所有参与者应拿到同一份结果");
            }
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("不同 key 的请求互不影响，各自执行一次 loader")
    void differentKeysExecuteSeparately() throws Exception {
        AtomicInteger loaderCount = new AtomicInteger(0);
        Callable<String> loader = () -> "R" + loaderCount.incrementAndGet();

        String first = singleFlight.execute("QUESTION_GEN", "key-1", Duration.ofSeconds(5), loader);
        String second = singleFlight.execute("QUESTION_GEN", "key-2", Duration.ofSeconds(5), loader);

        assertEquals("R1", first);
        assertEquals("R2", second);
        assertEquals(2, loaderCount.get());
    }

    @Test
    @DisplayName("owner 抛异常时，异常原样传播给调用方，且不残留 flight entry")
    void exceptionPropagates() {
        IllegalStateException expected = new IllegalStateException("LLM 调用失败");
        Callable<String> failingLoader = () -> {
            throw expected;
        };

        IllegalStateException actual = assertThrows(IllegalStateException.class,
                () -> singleFlight.execute("EVALUATION", "key-B", Duration.ofSeconds(5), failingLoader));
        assertEquals("LLM 调用失败", actual.getMessage());

        // 异常后 entry 被立即清理，下一次调用应重新执行 loader
        AtomicInteger secondRun = new AtomicInteger(0);
        assertThrows(IllegalStateException.class,
                () -> singleFlight.execute("EVALUATION", "key-B", Duration.ofSeconds(5), () -> {
                    secondRun.incrementAndGet();
                    throw expected;
                }));
        assertEquals(1, secondRun.get(), "异常清理后重复调用应重新成为 owner");
    }

    @Test
    @DisplayName("follower 等待超过 waitTimeout 抛 FlightWaitTimeoutException")
    void followerWaitTimeout() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch ownerStarted = new CountDownLatch(1);
            Callable<String> slowLoader = () -> {
                ownerStarted.countDown();
                // 故意慢于 follower 的等待时间
                Thread.sleep(3_000L);
                return "TOO_LATE";
            };
            Future<String> ownerFuture = executor.submit(() ->
                    singleFlight.execute("REPORT_GEN", "key-C", Duration.ofSeconds(5), slowLoader));
            assertTrue(ownerStarted.await(3, TimeUnit.SECONDS));

            FlightWaitTimeoutException timeoutException = assertThrows(FlightWaitTimeoutException.class,
                    () -> singleFlight.execute("REPORT_GEN", "key-C", Duration.ofMillis(300), slowLoader),
                    "follower 等待超时应抛 FlightWaitTimeoutException");
            assertTrue(timeoutException.getMessage().contains("key-C"));

            assertEquals("TOO_LATE", ownerFuture.get(10, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("串行重复调用（无并发）每次都重新执行 loader")
    void sequentialCallsAlwaysExecuteLoader() throws Exception {
        AtomicInteger loaderCount = new AtomicInteger(0);
        Callable<String> loader = () -> "V" + loaderCount.incrementAndGet();

        List<String> actual = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            actual.add(singleFlight.execute("RESUME_PARSE", "key-D", Duration.ofSeconds(5), loader));
        }
        assertEquals(List.of("V1", "V2", "V3"), actual);
        assertEquals(3, loaderCount.get(), "串行调用每次都应重新执行 loader");
    }
}

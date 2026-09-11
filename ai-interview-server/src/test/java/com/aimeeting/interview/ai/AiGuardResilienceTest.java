package com.aimeeting.interview.ai;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aimeeting.interview.ai.guard.AiGuardService;
import com.aimeeting.interview.ai.guard.AiHealthSnapshot;
import com.aimeeting.interview.ai.guard.AiRateLimiter;
import com.aimeeting.interview.ai.guard.Bulkhead;
import com.aimeeting.interview.ai.log.AiCallLogService;
import com.aimeeting.interview.ai.model.AiBizType;
import com.aimeeting.interview.ai.model.AiErrorType;
import com.aimeeting.interview.ai.model.AiRequest;
import com.aimeeting.interview.ai.model.AiStage;
import com.aimeeting.interview.ai.model.AiStreamListener;
import com.aimeeting.interview.ai.model.AiTextResult;
import com.aimeeting.interview.ai.provider.AiProvider;
import com.aimeeting.interview.common.concurrent.singleflight.CaffeineSingleFlight;
import com.aimeeting.interview.common.concurrent.singleflight.SingleFlight;
import com.aimeeting.interview.common.convention.errorcode.BaseErrorCode;
import com.aimeeting.interview.common.convention.exception.RemoteException;
import com.aimeeting.interview.config.AiProperties;
import java.net.SocketTimeoutException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * {@link AiGuardService} 极端场景故障注入测试：网络中断 / 高并发 / 熔断 / 舱壁 /
 * 限流 / 瞬时故障重试 / 永久错误不重试 / 非法响应严格重试 / 退避生效 / 健康快照。
 *
 * <p>不依赖 Spring 容器：直接构造 Guard 及其依赖，用 {@link FakeProvider} 注入各类故障，
 * 用真实 {@link CaffeineSingleFlight} / {@link Bulkhead} / {@link AiRateLimiter} 验证并发语义。</p>
 */
class AiGuardResilienceTest {

    private AiProperties props;
    private FakeProvider provider;
    private SingleFlight singleFlight;
    private Bulkhead bulkhead;
    private AiRateLimiter rateLimiter;
    private AiCallLogService callLog;
    private AiGuardService guard;

    @BeforeEach
    void setUp() {
        props = new AiProperties();
        props.setMaxRetries(2);
        props.setRetryBaseDelayMillis(500L);
        props.setSingleflightWaitTimeoutSeconds(10);
        // 各阶段超时都设成 30s，故障注入由 provider 行为控制，避免误触超时
        props.setQuestionTimeoutSeconds(30);
        props.setEvaluateTimeoutSeconds(30);
        props.setFollowUpTimeoutSeconds(30);
        props.setResumeTimeoutSeconds(30);
        props.setReportTimeoutSeconds(30);
        props.setCircuitBreakerWindowSize(20);
        props.setCircuitBreakerFailureRate(0.5);
        props.setCircuitBreakerOpenSeconds(30);
        props.setBulkheadConcurrency(20);

        provider = new FakeProvider();
        singleFlight = new CaffeineSingleFlight();
        bulkhead = new Bulkhead(20);
        rateLimiter = new AiRateLimiter(0, 0); // 限流默认关闭，隔离其它维度
        callLog = Mockito.mock(AiCallLogService.class);
        guard = new AiGuardService(provider, props, singleFlight, bulkhead, rateLimiter, callLog);
    }

    /* --------------------------- 网络中断 --------------------------- */

    @Test
    @DisplayName("网络中断：provider 阻塞超阶段超时 -> 抛 AI_TIMEOUT(C0504)，且不重试")
    void networkInterruptionTimeoutThrowsAiTimeout() {
        props.setMaxRetries(0);
        props.setQuestionTimeoutSeconds(1); // 1s 超时
        provider.behavior = () -> {
            Thread.sleep(3000L); // 模拟网络卡死
            return new AiTextResult("late", 1, 1, 1L, "mock");
        };

        RemoteException ex = assertThrows(RemoteException.class,
                () -> guard.execute(AiStage.QUESTION_GEN, "k-timeout", 1L,
                        req(AiBizType.QUESTION), r -> r.getContent()));

        assertEquals("C0504", ex.getErrorCode(), "应映射为 AI 超时错误码 C0504");
        assertEquals(AiErrorType.TIMEOUT, ex.getErrorType());
        assertEquals(1, provider.chatCalls.get(), "超时不应触发重试（maxRetries=0）");
    }

    /* --------------------------- 高并发去重 --------------------------- */

    @Test
    @DisplayName("高并发：50 个相同请求只产生 1 次真实 AI 调用（single-flight 去重）")
    void highConcurrencySingleFlightDedup() throws Exception {
        int n = 50;
        provider.behavior = () -> {
            Thread.sleep(500L); // 让所有 follower 在 owner 完成前进入等待
            return new AiTextResult("OK", 1, 1, 1L, "mock");
        };
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(n);
        ExecutorService executor = Executors.newFixedThreadPool(n);
        String[] results = new String[n];
        AtomicInteger errors = new AtomicInteger(0);
        try {
            for (int i = 0; i < n; i++) {
                final int idx = i;
                executor.submit(() -> {
                    try {
                        start.await();
                        results[idx] = guard.execute(AiStage.QUESTION_GEN, "same-key", 1L,
                                req("concurrent"), r -> r.getContent());
                    } catch (Exception e) {
                        errors.incrementAndGet();
                    } finally {
                        done.countDown();
                    }
                });
            }
            start.countDown();
            assertTrue(done.await(25, TimeUnit.SECONDS), "所有并发请求应在 25s 内完成");
            assertEquals(0, errors.get(), "并发请求不应出现错误");
            assertEquals(1, provider.chatCalls.get(), "同 key 并发下 provider.chat 只应执行 1 次");
            for (String r : results) {
                assertEquals("OK", r, "所有调用者应拿到同一份结果");
            }
        } finally {
            executor.shutdown();
        }
    }

    /* --------------------------- 熔断 --------------------------- */

    @Test
    @DisplayName("熔断：连续失败率超阈值后熔断打开，后续请求快速失败 AI_UNAVAILABLE(C0501) 且不调用 provider")
    void circuitBreakerOpensAndShortCircuits() {
        props.setMaxRetries(0); // 隔离重试维度：每次失败只调用 1 次 provider
        props.setCircuitBreakerWindowSize(10);
        props.setCircuitBreakerFailureRate(0.5);
        props.setCircuitBreakerOpenSeconds(30);
        // breaker 在 AiGuardService 构造时固化为 props 当前值，改 props 后必须重建 guard
        guard = new AiGuardService(provider, props, singleFlight, bulkhead, rateLimiter, callLog);
        provider.behavior = () -> {
            throw new SocketTimeoutException("connection refused");
        };

        // 前 10 次失败，第 10 次触发熔断打开
        for (int i = 0; i < 10; i++) {
            final int idx = i;
            RemoteException ex = assertThrows(RemoteException.class,
                    () -> guard.execute(AiStage.QUESTION_GEN, "cb-" + idx, 1L,
                            req("cb" + idx), r -> r.getContent()));
            assertTrue(ex.getErrorType() == AiErrorType.TIMEOUT
                    || ex.getErrorType() == AiErrorType.UNAVAILABLE);
        }
        assertEquals(10, provider.chatCalls.get(), "前 10 次应真实调用 provider");

        // 第 11 次：熔断已打开，快速失败
        RemoteException open = assertThrows(RemoteException.class,
                () -> guard.execute(AiStage.QUESTION_GEN, "cb-11", 1L,
                        req("cb11"), r -> r.getContent()));
        assertEquals(BaseErrorCode.AI_UNAVAILABLE.code(), open.getErrorCode(),
                "熔断打开后应返回 C0501");
        assertEquals(10, provider.chatCalls.get(), "熔断打开后不应再调用 provider");
        assertTrue(guard.health().isCircuitBreakerOpen(), "健康快照应反映熔断打开");
    }

    /* --------------------------- 舱壁 --------------------------- */

    @Test
    @DisplayName("舱壁：并发打满后多余请求被拒 AI_BUSY(C0502)，且不调用 provider")
    void bulkheadRejectsWhenFull() throws Exception {
        bulkhead = new Bulkhead(2);
        guard = new AiGuardService(provider, props, singleFlight, bulkhead, rateLimiter, callLog);
        provider.behavior = () -> {
            Thread.sleep(2000L); // 占用并发配额 2s
            return new AiTextResult("slow", 1, 1, 1L, "mock");
        };
        int n = 3;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(n);
        ExecutorService executor = Executors.newFixedThreadPool(n);
        RemoteException[] errors = new RemoteException[n];
        try {
            for (int i = 0; i < n; i++) {
                final int idx = i;
                executor.submit(() -> {
                    try {
                        start.await();
                        guard.execute(AiStage.QUESTION_GEN, "bh-" + idx, 1L,
                                req("bh"), r -> r.getContent());
                    } catch (RemoteException e) {
                        errors[idx] = e;
                    } catch (Exception e) {
                        errors[idx] = new RemoteException(e.getMessage(),
                                BaseErrorCode.REMOTE_ERROR, AiErrorType.UNAVAILABLE);
                    } finally {
                        done.countDown();
                    }
                });
            }
            start.countDown();
            assertTrue(done.await(15, TimeUnit.SECONDS), "请求应在 15s 内全部结束");
            int busy = 0;
            int ok = 0;
            for (RemoteException e : errors) {
                if (e != null && BaseErrorCode.AI_BUSY.code().equals(e.getErrorCode())) {
                    busy++;
                } else if (e == null) {
                    ok++;
                }
            }
            assertEquals(1, busy, "应有 1 个请求被舱壁拒绝");
            assertEquals(2, ok, "应有 2 个请求成功占用并发配额");
            assertEquals(2, provider.chatCalls.get(), "被拒请求不应调用 provider");
        } finally {
            executor.shutdown();
        }
    }

    /* --------------------------- 限流 --------------------------- */

    @Test
    @DisplayName("限流：超过每分钟上限后被拒 RATE_LIMITED(A0501)，且不调用 provider")
    void rateLimiterRejectsWhenExceeded() {
        rateLimiter = new AiRateLimiter(2, 100000);
        guard = new AiGuardService(provider, props, singleFlight, bulkhead, rateLimiter, callLog);
        provider.behavior = () -> new AiTextResult("ok", 1, 1, 1L, "mock");

        for (int i = 0; i < 2; i++) {
            String r = guard.execute(AiStage.QUESTION_GEN, "rl-" + i, 99L,
                    req("rl" + i), x -> x.getContent());
            assertEquals("ok", r);
        }
        assertEquals(2, provider.chatCalls.get(), "前 2 次应放行");

        RemoteException ex = assertThrows(RemoteException.class,
                () -> guard.execute(AiStage.QUESTION_GEN, "rl-2", 99L,
                        req("rl2"), x -> x.getContent()));
        assertEquals(BaseErrorCode.RATE_LIMITED.code(), ex.getErrorCode());
        assertEquals(2, provider.chatCalls.get(), "超限请求不应调用 provider");
    }

    /* --------------------------- 重试 --------------------------- */

    @Test
    @DisplayName("瞬时故障重试：TIMEOUT 两次后第 3 次成功，共调用 3 次")
    void retryOnTransientTimeoutEventuallySucceeds() {
        props.setMaxRetries(2);
        provider.behavior = new ThrowingSupplier<>() {
            private int c = 0;

            @Override
            public AiTextResult get() throws Exception {
                if (c++ < 2) {
                    throw new SocketTimeoutException("read timed out");
                }
                return new AiTextResult("recovered", 1, 1, 1L, "mock");
            }
        };
        String r = guard.execute(AiStage.QUESTION_GEN, "retry-key", 1L,
                req("retry"), x -> x.getContent());
        assertEquals("recovered", r);
        assertEquals(3, provider.chatCalls.get(), "应重试直到成功：1 次原始 + 2 次重试");
    }

    @Test
    @DisplayName("永久错误不重试：QUOTA(402) 直接抛出，仅调用 1 次")
    void permanentErrorNoRetry() {
        provider.behavior = () -> {
            throw new RemoteException("额度耗尽", BaseErrorCode.REMOTE_ERROR, AiErrorType.QUOTA);
        };
        RemoteException ex = assertThrows(RemoteException.class,
                () -> guard.execute(AiStage.QUESTION_GEN, "quota", 1L,
                        req("quota"), x -> x.getContent()));
        assertEquals(AiErrorType.QUOTA, ex.getErrorType());
        assertEquals(1, provider.chatCalls.get(), "永久错误不应重试");
    }

    @Test
    @DisplayName("非法响应追加一次严格重试：首回非法内容，strict 重试后成功，共 2 次调用")
    void invalidResponseOneStrictRetry() {
        provider.behavior = new ThrowingSupplier<>() {
            private int c = 0;

            @Override
            public AiTextResult get() throws Exception {
                c++;
                return new AiTextResult(c == 1 ? "NOT_JSON_AT_ALL" : "{\"score\":88}",
                        1, 1, 1L, "mock");
            }
        };
        Function<AiTextResult, String> parser = r -> {
            if (!r.getContent().startsWith("{")) {
                throw new RemoteException("非法响应", BaseErrorCode.REMOTE_ERROR,
                        AiErrorType.INVALID_RESPONSE);
            }
            return r.getContent();
        };
        String r = guard.execute(AiStage.EVALUATION, "invalid", 1L,
                req(AiBizType.EVALUATE), parser);
        assertEquals("{\"score\":88}", r);
        assertEquals(2, provider.chatCalls.get(), "非法响应应追加一次严格重试");
    }

    @Test
    @DisplayName("退避生效：瞬时故障重试总耗时 >= 基础退避之和（验证 Equal Jitter 确实休眠）")
    void backoffDelayActuallySleeps() {
        props.setMaxRetries(2);
        provider.behavior = new ThrowingSupplier<>() {
            private int c = 0;

            @Override
            public AiTextResult get() throws Exception {
                if (c++ < 2) {
                    throw new SocketTimeoutException();
                }
                return new AiTextResult("ok", 1, 1, 1L, "mock");
            }
        };
        long t0 = System.currentTimeMillis();
        guard.execute(AiStage.QUESTION_GEN, "bo", 1L, req("bo"), x -> x.getContent());
        long elapsed = System.currentTimeMillis() - t0;
        // Equal Jitter：attempt0∈[250,500]，attempt1∈[750,1500]，合计至少 ~1s
        assertTrue(elapsed >= 900,
                "两次退避（500+1500 基数，Equal Jitter 减半）至少应休眠约 1s，实际=" + elapsed);
    }

    /* --------------------------- 健康快照 --------------------------- */

    @Test
    @DisplayName("健康快照：正常时熔断关闭、舱壁总量正确")
    void healthSnapshotReflectsState() {
        AiHealthSnapshot h = guard.health();
        assertEquals("mock", h.getProvider());
        assertEquals(false, h.isCircuitBreakerOpen());
        assertEquals(20, h.getBulkheadTotal());
        assertDoesNotThrow(() -> h.getCircuitBreakerStates());
    }

    /* --------------------------- 工具 --------------------------- */

    private AiRequest req(AiBizType biz) {
        return AiRequest.builder().bizType(biz).userPrompt("p").build();
    }

    private AiRequest req(String prompt) {
        return AiRequest.builder().bizType(AiBizType.QUESTION).userPrompt(prompt).build();
    }

    /** 可控故障的假 Provider：每次 chat 调用都计数，行为由 {@link #behavior} 注入。 */
    static class FakeProvider implements AiProvider {
        volatile boolean available = true;
        final AtomicInteger chatCalls = new AtomicInteger(0);
        volatile ThrowingSupplier<AiTextResult> behavior =
                () -> new AiTextResult("OK", 1, 1, 1L, "mock");

        @Override
        public String name() {
            return "mock";
        }

        @Override
        public boolean available() {
            return available;
        }

        @Override
        public AiTextResult chat(AiRequest request) {
            chatCalls.incrementAndGet();
            try {
                return behavior.get();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void streamChat(AiRequest request, AiStreamListener out) {
            out.onDelta("hi");
            out.onComplete(new AiTextResult("hi", 0, 0, 1L, "mock"));
        }
    }

    @FunctionalInterface
    interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
}

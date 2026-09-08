package com.aimeeting.interview.interview.service;

import com.aimeeting.interview.interview.service.sse.ErrorPayload;
import com.aimeeting.interview.interview.service.sse.SseEnvelope;
import com.aimeeting.interview.interview.service.sse.SseEventType;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE 发射器管理：统一创建 / 发送 / 完成 / 错误。
 *
 * <p>约定（ARCHITECTURE.md §6.1）：超时 5min；每 15s 发 {@code : ping} 心跳防代理超时；
 * onCompletion / onError / onTimeout 统一清理心跳任务。
 */
@Slf4j
@Component
public class SseEmitterManager {

    /** SSE 连接超时：5 分钟。 */
    private static final long TIMEOUT_MILLIS = 5L * 60 * 1000;

    /** 心跳间隔：15 秒。 */
    private static final long HEARTBEAT_SECONDS = 15L;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2, new ThreadFactory() {
        private int seq = 0;
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "sse-heartbeat-" + (++seq));
            t.setDaemon(true);
            return t;
        }
    });

    /**
     * 创建发射器并设置生命周期回调。
     *
     * @param requestId 请求 ID（超时错误事件携带）
     * @param sessionId 会话 ID
     * @return SseEmitter
     */
    public SseEmitter create(String requestId, Long sessionId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MILLIS);
        ScheduledFuture<?> heartbeat = scheduler.scheduleAtFixedRate(() -> {
            try {
                emitter.send(SseEmitter.event().comment("ping"));
            } catch (Exception ignore) {
                // 连接已断开，心跳失败时心跳任务会在 onCompletion/onError 中被取消
            }
        }, HEARTBEAT_SECONDS, HEARTBEAT_SECONDS, TimeUnit.SECONDS);

        emitter.onCompletion(() -> heartbeat.cancel(true));
        emitter.onError((Throwable t) -> heartbeat.cancel(true));
        emitter.onTimeout(() -> {
            heartbeat.cancel(true);
            try {
                emitter.send(SseEmitter.event().name(SseEventType.error.name()).data(
                        SseEnvelope.of(SseEventType.error, 0, requestId, sessionId, null, false,
                                new ErrorPayload("C0000", "连接超时，请重试", true))));
            } catch (Exception ignore) {
                // 超时后发送可能失败，忽略
            }
        });
        return emitter;
    }

    /**
     * 发送一个事件。
     *
     * @param emitter 发射器
     * @param env      事件信封
     */
    public void send(SseEmitter emitter, SseEnvelope env) {
        try {
            emitter.send(SseEmitter.event().name(env.getType()).data(env));
        } catch (IOException | IllegalStateException e) {
            throw new IllegalStateException("SSE 发送失败: " + e.getMessage(), e);
        }
    }

    /** 正常完成流。 */
    public void complete(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (Exception ignore) {
            // 已结束
        }
    }

    /**
     * 发送错误事件并完成流。
     *
     * @param emitter    发射器
     * @param code       错误码
     * @param message    错误文案
     * @param requestId  请求 ID
     * @param sessionId  会话 ID
     */
    public void error(SseEmitter emitter, String code, String message, String requestId, Long sessionId) {
        try {
            emitter.send(SseEmitter.event().name(SseEventType.error.name()).data(
                    SseEnvelope.of(SseEventType.error, 0, requestId, sessionId, null, false,
                            new ErrorPayload(code, message, true))));
        } catch (Exception ignore) {
            // 连接可能已断
        }
        complete(emitter);
    }
}

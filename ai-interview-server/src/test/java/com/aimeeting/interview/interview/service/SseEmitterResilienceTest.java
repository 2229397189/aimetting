package com.aimeeting.interview.interview.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.aimeeting.interview.interview.service.sse.ErrorPayload;
import com.aimeeting.interview.interview.service.sse.SseEnvelope;
import com.aimeeting.interview.interview.service.sse.SseEventType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * {@link SseEmitterManager} 客户端断开 / 异常场景下的健壮性测试。
 *
 * <p>核心关注点：客户端中途断开（连接丢失）时，发送失败必须被显式转换为异常交给上层降级，
 * 而不是静默吞掉导致内存/线程泄漏；重复 complete 与 error 必须幂等且安全。</p>
 */
class SseEmitterResilienceTest {

    private final SseEmitterManager manager = new SseEmitterManager();

    @Test
    @DisplayName("客户端断开后发送失败被转换为异常（不静默吞掉，由上层降级）")
    void sendAfterDisconnectThrows() {
        SseEmitter emitter = manager.create("r1", 1L);
        manager.complete(emitter); // 模拟客户端断开（连接已结束）
        // 已断开的 emitter.send 会抛 IOException/IllegalStateException，
        // manager.send 应将其归一为 IllegalStateException 交由调用方处理
        SseEnvelope env = SseEnvelope.of(SseEventType.question, 0, "r1", 1L, null, false, "x");
        assertThrows(IllegalStateException.class, () -> manager.send(emitter, env),
                "断开后发送应抛异常而非静默失败");
    }

    @Test
    @DisplayName("重复 complete 幂等且不抛异常（连接关闭安全）")
    void completeIsIdempotent() {
        SseEmitter emitter = manager.create("r2", 2L);
        assertDoesNotThrow(() -> {
            manager.complete(emitter);
            manager.complete(emitter);
        });
    }

    @Test
    @DisplayName("error 发送错误事件并安全完成，即使连接已断也不抛异常")
    void errorIsSafeOnDisconnect() {
        SseEmitter emitter = manager.create("r3", 3L);
        assertDoesNotThrow(() -> manager.error(emitter, "C0000", "连接超时，请重试", "r3", 3L));
    }

    @Test
    @DisplayName("断开可被发现而非泄漏：completed 后再 send 抛异常，error 事件安全完成")
    void disconnectSurfacedNotSwallowed() {
        SseEmitter emitter = manager.create("r4", 4L);
        manager.complete(emitter); // 模拟客户端断开
        SseEnvelope env = SseEnvelope.of(SseEventType.error, 0, "r4", 4L, null, true,
                new ErrorPayload("C0501", "AI 熔断已打开", false));
        // error 事件对已完成的 emitter 应安全（内部吞掉），不抛
        assertDoesNotThrow(() -> manager.error(emitter, "C0501", "AI 熔断已打开", "r4", 4L));
        // 但再次 send 必须暴露断开，交由上层终止流
        assertThrows(IllegalStateException.class, () -> manager.send(emitter, env));
    }
}

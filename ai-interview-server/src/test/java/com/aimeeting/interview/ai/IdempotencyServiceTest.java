package com.aimeeting.interview.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.aimeeting.interview.common.cache.CaffeineCacheService;
import com.aimeeting.interview.common.idempotent.IdempotencyService;
import com.aimeeting.interview.common.idempotent.IdempotentStage;
import com.aimeeting.interview.common.idempotent.TryStartResult;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link IdempotencyService} 单测：三态判定（NEW -&gt; PROCESSING -&gt; SUCCEEDED 回放）。
 */
class IdempotencyServiceTest {

    private static final TypeReference<Map<String, Object>> REPLAY_TYPE =
            new TypeReference<Map<String, Object>>() {
            };

    private IdempotencyService idempotencyService;

    @BeforeEach
    void setUp() {
        idempotencyService = new IdempotencyService(new CaffeineCacheService());
    }

    @Test
    @DisplayName("首次请求为 NEW，未标记成功前再次请求为 PROCESSING")
    void newThenProcessing() {
        TryStartResult<Map<String, Object>> first = idempotencyService.tryStart(
                IdempotentStage.ANSWER_SUBMIT, 1L, "1", "token-1", REPLAY_TYPE);
        assertEquals(TryStartResult.Status.NEW, first.getStatus());
        assertNull(first.getReplay());

        TryStartResult<Map<String, Object>> second = idempotencyService.tryStart(
                IdempotentStage.ANSWER_SUBMIT, 1L, "1", "token-1", REPLAY_TYPE);
        assertEquals(TryStartResult.Status.PROCESSING, second.getStatus());
        assertNull(second.getReplay());
    }

    @Test
    @DisplayName("标记成功后再次请求为 SUCCEEDED 并回放上次结果")
    void newThenSucceededReplay() {
        Long userId = 2L;
        String bizId = "77";
        String clientToken = "token-replay";

        TryStartResult<Map<String, Object>> first = idempotencyService.tryStart(
                IdempotentStage.ANSWER_SUBMIT, userId, bizId, clientToken, REPLAY_TYPE);
        assertEquals(TryStartResult.Status.NEW, first.getStatus());

        idempotencyService.markSuccess(IdempotentStage.ANSWER_SUBMIT, userId, bizId,
                clientToken, Map.of("score", 88, "comment", "回答不错"));

        TryStartResult<Map<String, Object>> replay = idempotencyService.tryStart(
                IdempotentStage.ANSWER_SUBMIT, userId, bizId, clientToken, REPLAY_TYPE);
        assertEquals(TryStartResult.Status.SUCCEEDED, replay.getStatus());
        assertNotNull(replay.getReplay());
        assertEquals(88, replay.getReplay().get("score"));
        assertEquals("回答不错", replay.getReplay().get("comment"));
    }

    @Test
    @DisplayName("clear 释放处理中键后，可重新开始（模拟失败重试）")
    void clearAllowsRetry() {
        TryStartResult<Map<String, Object>> first = idempotencyService.tryStart(
                IdempotentStage.REPORT_GENERATE, 3L, "9", "token-clear", REPLAY_TYPE);
        assertEquals(TryStartResult.Status.NEW, first.getStatus());

        idempotencyService.clear(IdempotentStage.REPORT_GENERATE, 3L, "9", "token-clear");

        TryStartResult<Map<String, Object>> retry = idempotencyService.tryStart(
                IdempotentStage.REPORT_GENERATE, 3L, "9", "token-clear", REPLAY_TYPE);
        assertEquals(TryStartResult.Status.NEW, retry.getStatus(), "clear 后应可重新抢占");
    }

    @Test
    @DisplayName("不同 stage / clientToken 之间互不干扰")
    void differentKeysAreIsolated() {
        TryStartResult<Map<String, Object>> answerSubmit = idempotencyService.tryStart(
                IdempotentStage.ANSWER_SUBMIT, 4L, "5", "same-token", REPLAY_TYPE);
        TryStartResult<Map<String, Object>> reportGenerate = idempotencyService.tryStart(
                IdempotentStage.REPORT_GENERATE, 4L, "5", "same-token", REPLAY_TYPE);

        assertEquals(TryStartResult.Status.NEW, answerSubmit.getStatus());
        assertEquals(TryStartResult.Status.NEW, reportGenerate.getStatus(),
                "不同 stage 的幂等键应互相隔离");
    }

    @Test
    @DisplayName("clientToken 为空时跳过幂等，恒为 NEW")
    void blankClientTokenAlwaysNew() {
        TryStartResult<Map<String, Object>> first = idempotencyService.tryStart(
                IdempotentStage.RESUME_PARSE, 5L, "3", null, REPLAY_TYPE);
        TryStartResult<Map<String, Object>> second = idempotencyService.tryStart(
                IdempotentStage.RESUME_PARSE, 5L, "3", "  ", REPLAY_TYPE);

        assertEquals(TryStartResult.Status.NEW, first.getStatus());
        assertEquals(TryStartResult.Status.NEW, second.getStatus());
    }
}

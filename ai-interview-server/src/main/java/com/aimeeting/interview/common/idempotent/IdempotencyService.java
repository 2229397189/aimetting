package com.aimeeting.interview.common.idempotent;

import com.aimeeting.interview.common.cache.CacheService;
import com.fasterxml.jackson.core.type.TypeReference;
import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 幂等服务：双键模型（BR-07）。
 *
 * <ul>
 *   <li><b>处理中键</b> {@code idem:proc:{stage}:{userId}:{bizId}:{clientToken}}，TTL 60s，
 *       用 {@code putIfAbsent} 原子抢占，用于识别「重复提交 / 并发提交」。</li>
 *   <li><b>回放键</b> {@code idem:replay:{...}}，TTL 24h，保存上次成功结果，
 *       重复提交时直接回放，保证 LLM 调用次数不增加。</li>
 * </ul>
 *
 * <p>三态判定顺序：SUCCEEDED -&gt; NEW/PROCESSING（先查回放键，再抢处理中键）。
 * 单机实现基于 Caffeine；后续接 Redis 只需替换 {@link CacheService} 实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    /** 处理中键 TTL：60s（BR-07）。 */
    private static final Duration PROCESSING_TTL = Duration.ofSeconds(60);

    /** 回放键 TTL：24h（BR-07）。 */
    private static final Duration REPLAY_TTL = Duration.ofHours(24);

    /** 处理中键前缀。 */
    private static final String PROC_PREFIX = "idem:proc:";

    /** 回放键前缀。 */
    private static final String REPLAY_PREFIX = "idem:replay:";

    private final CacheService cacheService;

    /**
     * 尝试开始一次幂等操作。
     *
     * @param stage       幂等阶段
     * @param userId      用户 ID
     * @param bizId       业务 ID（如 sessionId）
     * @param clientToken 前端生成的幂等令牌（{@code X-Client-Token}），为空则跳过幂等
     * @param replayType  回放结果类型引用
     * @param <T>         回放结果类型
     * @return 三态结果：NEW / PROCESSING / SUCCEEDED
     */
    public <T> TryStartResult<T> tryStart(IdempotentStage stage, Long userId, String bizId,
                                          String clientToken, TypeReference<T> replayType) {
        if (clientToken == null || clientToken.isBlank()) {
            return TryStartResult.newRequest();
        }
        String replayKey = replayKey(stage, userId, bizId, clientToken);
        Optional<T> replay = cacheService.get(replayKey, replayType);
        if (replay.isPresent()) {
            log.info("[Idempotent] 命中回放键, stage={}, userId={}, bizId={}", stage, userId, bizId);
            return TryStartResult.succeeded(replay.get());
        }
        String procKey = procKey(stage, userId, bizId, clientToken);
        boolean acquired = cacheService.putIfAbsent(procKey, "1", PROCESSING_TTL);
        if (acquired) {
            return TryStartResult.newRequest();
        }
        log.info("[Idempotent] 处理中, stage={}, userId={}, bizId={}", stage, userId, bizId);
        return TryStartResult.processing();
    }

    /**
     * 业务成功后写入回放结果并释放处理中键。
     *
     * @param stage       幂等阶段
     * @param userId      用户 ID
     * @param bizId       业务 ID
     * @param clientToken 幂等令牌
     * @param result      成功结果（会被缓存用于回访）
     */
    public void markSuccess(IdempotentStage stage, Long userId, String bizId,
                            String clientToken, Object result) {
        if (clientToken == null || clientToken.isBlank()) {
            return;
        }
        cacheService.put(replayKey(stage, userId, bizId, clientToken), result, REPLAY_TTL);
        cacheService.remove(procKey(stage, userId, bizId, clientToken));
    }

    /**
     * 业务失败后释放处理中键，允许客户端重试。
     *
     * @param stage       幂等阶段
     * @param userId      用户 ID
     * @param bizId       业务 ID
     * @param clientToken 幂等令牌
     */
    public void clear(IdempotentStage stage, Long userId, String bizId, String clientToken) {
        if (clientToken == null || clientToken.isBlank()) {
            return;
        }
        cacheService.remove(procKey(stage, userId, bizId, clientToken));
    }

    /**
     * 构造处理中键。
     *
     * @param stage       幂等阶段
     * @param userId      用户 ID
     * @param bizId       业务 ID
     * @param clientToken 幂等令牌
     * @return 处理中缓存键
     */
    private String procKey(IdempotentStage stage, Long userId, String bizId, String clientToken) {
        return PROC_PREFIX + stage + ":" + userId + ":" + bizId + ":" + clientToken;
    }

    /**
     * 构造回放键。
     *
     * @param stage       幂等阶段
     * @param userId      用户 ID
     * @param bizId       业务 ID
     * @param clientToken 幂等令牌
     * @return 回放缓存键
     */
    private String replayKey(IdempotentStage stage, Long userId, String bizId, String clientToken) {
        return REPLAY_PREFIX + stage + ":" + userId + ":" + bizId + ":" + clientToken;
    }
}

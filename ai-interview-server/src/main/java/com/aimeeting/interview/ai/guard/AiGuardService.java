package com.aimeeting.interview.ai.guard;

import com.aimeeting.interview.config.AiProperties;
import com.aimeeting.interview.ai.log.AiCallLogDO;
import com.aimeeting.interview.ai.log.AiCallLogService;
import com.aimeeting.interview.ai.model.AiErrorType;
import com.aimeeting.interview.ai.model.AiRequest;
import com.aimeeting.interview.ai.model.AiStage;
import com.aimeeting.interview.ai.model.AiStreamListener;
import com.aimeeting.interview.ai.model.AiTextResult;
import com.aimeeting.interview.ai.parser.SectionedStreamParser;
import com.aimeeting.interview.ai.provider.AiProvider;
import com.aimeeting.interview.common.concurrent.singleflight.SingleFlight;
import com.aimeeting.interview.common.convention.errorcode.BaseErrorCode;
import com.aimeeting.interview.common.convention.exception.RemoteException;
import com.aimeeting.interview.common.util.Md5Util;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * AI 调用的唯一入口（BR-08 ~ BR-13）。
 *
 * <p>统一的管控顺序：
 * <ol>
 *   <li><b>单飞</b>（Single-flight）：同 {@code group + key} 的并发请求只放行一个 owner，
 *       follower 复用 owner 结果，避免 LLM 调用放大。</li>
 *   <li><b>熔断</b>：滑动窗口内失败率超阈值直接快速失败（C0501）。</li>
 *   <li><b>舱壁</b>：并发配额打满直接拒绝（C0502）。</li>
 *   <li><b>超时</b>：按阶段超时时间中断调用（C0504）。</li>
 *   <li><b>重试</b>：最多 {@code maxRetries} 次，指数退避并叠加 Equal Jitter 抖动
 *       （避免重试风暴），仅 TIMEOUT / UNAVAILABLE 可重试；
 *       另对 INVALID_RESPONSE 追加一次「严格按格式输出」的重试。</li>
 *   <li><b>日志</b>：异步落 t_ai_call_log（摘要截断 512）。</li>
 *   <li><b>失败</b>：统一抛 {@link RemoteException}，由上层决定降级。</li>
 * </ol>
 */
@Slf4j
@Service
public class AiGuardService {

    /** 流式回放分片大小（字符）。 */
    private static final int REPLAY_CHUNK = 20;

    /** 流式回放分片间隔（毫秒）。 */
    private static final long REPLAY_INTERVAL_MS = 30L;

    /** 退避基数默认值：配置缺失或非法时兜底 500ms。 */
    private static final long DEFAULT_BACKOFF_BASE_MILLIS = 500L;

    /** 退避上限，防止 attempt 较大时指数增长到不可接受的等待（30s）。 */
    private static final long MAX_BACKOFF_MILLIS = 30_000L;

    /** 位移上限：超过此 attempt 不再做 {@code 1 << attempt} 计算，直接取上限，避免整型溢出。 */
    private static final int MAX_BACKOFF_SHIFT = 6;

    private final AiProvider provider;
    private final AiProperties props;
    private final SingleFlight singleFlight;
    private final Bulkhead bulkhead;
    private final AiRateLimiter rateLimiter;
    private final AiCallLogService callLogService;
    private final CircuitBreaker circuitBreaker;

    /** 超时控制用线程池：AI 调用本身阻塞在 OkHttp，这里只做超时兜底。 */
    private final ExecutorService timeoutExecutor = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "ai-guard-timeout-");
        thread.setDaemon(true);
        return thread;
    });

    public AiGuardService(AiProvider provider, AiProperties props, SingleFlight singleFlight,
                          Bulkhead bulkhead, AiRateLimiter rateLimiter, AiCallLogService callLogService) {
        this.provider = provider;
        this.props = props;
        this.singleFlight = singleFlight;
        this.bulkhead = bulkhead;
        this.rateLimiter = rateLimiter;
        this.callLogService = callLogService;
        this.circuitBreaker = new CircuitBreaker(props.getCircuitBreakerWindowSize(),
                props.getCircuitBreakerFailureRate(), Duration.ofSeconds(props.getCircuitBreakerOpenSeconds()));
    }

    /* ------------------------------ 阻塞式调用 ------------------------------ */

    /**
     * 执行 AI 调用并解析结果。
     *
     * @param stage           调用阶段（决定超时 / 单飞分组 / 业务类型）
     * @param singleFlightKey 单飞键，为空时用 userPrompt 的 MD5 兜底
     * @param userId          操作用户（用于日志）
     * @param req             AI 请求
     * @param parser          结果解析器（解析失败请抛 {@link RemoteException}(INVALID_RESPONSE)）
     * @param <T>             解析结果类型
     * @return 解析后的业务结果
     * @throws RemoteException AI 调用或解析失败
     */
    public <T> T execute(AiStage stage, String singleFlightKey, Long userId,
                         AiRequest req, Function<AiTextResult, T> parser) {
        String group = "AI_" + stage.name();
        String key = singleFlightKey == null || singleFlightKey.isBlank()
                ? Md5Util.md5Safe(req.getUserPrompt()) : singleFlightKey;
        try {
            return singleFlight.execute(group, key, Duration.ofSeconds(props.getSingleflightWaitTimeoutSeconds()),
                    () -> callWithGuards(stage, userId, req, parser));
        } catch (Exception e) {
            throw translate(e, stage);
        }
    }

    /**
     * 熔断 + 舱壁 + 超时 + 重试 + 日志的执行体（仅 single-flight 的 owner 会走到这里）。
     */
    private <T> T callWithGuards(AiStage stage, Long userId, AiRequest req, Function<AiTextResult, T> parser) {
        long start = System.currentTimeMillis();
        if (!rateLimiter.allow(userId)) {
            RemoteException limited = new RemoteException("AI 调用过于频繁，请稍后再试",
                    BaseErrorCode.RATE_LIMITED, AiErrorType.UNAVAILABLE);
            callLogService.record(buildLog(stage, userId, req, null, false, AiErrorType.UNAVAILABLE,
                    limited.getMessage(), System.currentTimeMillis() - start));
            throw limited;
        }
        if (!circuitBreaker.allowRequest()) {
            RemoteException blocked = new RemoteException("AI 熔断已打开，暂时不可用", BaseErrorCode.AI_UNAVAILABLE, AiErrorType.UNAVAILABLE);
            callLogService.record(buildLog(stage, userId, req, null, false, AiErrorType.UNAVAILABLE,
                    blocked.getMessage(), System.currentTimeMillis() - start));
            throw blocked;
        }
        if (!bulkhead.tryAcquire()) {
            RemoteException busy = new RemoteException("AI 服务繁忙，请稍后再试", BaseErrorCode.AI_BUSY, AiErrorType.UNAVAILABLE);
            callLogService.record(buildLog(stage, userId, req, null, false, AiErrorType.UNAVAILABLE,
                    busy.getMessage(), System.currentTimeMillis() - start));
            throw busy;
        }
        try {
            int maxRetries = Math.max(0, props.getMaxRetries());
            boolean invalidRetried = false;
            AiRequest current = req;
            RemoteException last = null;
            for (int attempt = 0; attempt <= maxRetries; attempt++) {
                try {
                    AiTextResult result = callWithTimeout(stage, current);
                    T parsed = parser.apply(result);
                    circuitBreaker.recordSuccess();
                    callLogService.record(buildLog(stage, userId, req, result, true, null, null,
                            System.currentTimeMillis() - start));
                    return parsed;
                } catch (Exception e) {
                    AiErrorType type = AiErrorClassifier.classify(e);
                    circuitBreaker.recordFailure();
                    last = asRemote(e, type);
                    boolean invalidRetry = type == AiErrorType.INVALID_RESPONSE && !invalidRetried;
                    if ((AiErrorClassifier.retryable(type) || invalidRetry) && attempt < maxRetries) {
                        if (invalidRetry) {
                            invalidRetried = true;
                            current = withStrictHint(req);
                        }
                        long delay = backoffMillis(attempt);
                        log.warn("[AiGuard] 调用失败准备重试, stage={}, attempt={}, type={}, delay={}ms, msg={}",
                                stage, attempt, type, delay, last.getMessage());
                        sleepQuietly(delay);
                        continue;
                    }
                    callLogService.record(buildLog(stage, userId, req, null, false, type, last.getMessage(),
                            System.currentTimeMillis() - start));
                    throw last;
                }
            }
            callLogService.record(buildLog(stage, userId, req, null, false,
                    last == null ? AiErrorType.UNAVAILABLE : AiErrorClassifier.classify(last),
                    last == null ? "AI 调用失败" : last.getMessage(), System.currentTimeMillis() - start));
            throw last == null
                    ? new RemoteException("AI 调用失败", BaseErrorCode.REMOTE_ERROR, AiErrorType.UNAVAILABLE)
                    : last;
        } finally {
            bulkhead.release();
        }
    }

    /**
     * 带超时的真实调用。
     */
    private AiTextResult callWithTimeout(AiStage stage, AiRequest req) {
        long timeoutMillis = props.stageTimeoutMillis(stage.bizType);
        try {
            CompletableFuture<AiTextResult> future =
                    CompletableFuture.supplyAsync(() -> provider.chat(req), timeoutExecutor);
            return future.get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            throw new RemoteException("AI 调用超时（" + timeoutMillis + "ms）", e, BaseErrorCode.AI_TIMEOUT, AiErrorType.TIMEOUT);
        } catch (java.util.concurrent.ExecutionException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            throw asRemote(cause, AiErrorClassifier.classify(cause));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RemoteException("AI 调用被中断", e, BaseErrorCode.REMOTE_ERROR, AiErrorType.UNAVAILABLE);
        } catch (Exception e) {
            throw asRemote(e, AiErrorClassifier.classify(e));
        }
    }

    /* ------------------------------ 流式调用 ------------------------------ */

    /**
     * 流式调用：把正文增量转发给 {@code out}。
     *
     * <p>用 {@link SectionedStreamParser} 分离「正文」与 {@code ===JSON===} 结构化段：
     * 增量只下发正文；{@code onComplete} 收到的 {@code content} 为 JSON 段（无分隔符时为全文）。
     *
     * @param stage           调用阶段
     * @param singleFlightKey 单飞键
     * @param userId          操作用户
     * @param req             AI 请求
     * @param out             流式回调
     */
    public void executeStream(AiStage stage, String singleFlightKey, Long userId,
                              AiRequest req, AiStreamListener out) {
        String group = "AI_STREAM_" + stage.name();
        String key = singleFlightKey == null || singleFlightKey.isBlank()
                ? Md5Util.md5Safe(req.getUserPrompt()) : singleFlightKey;
        AtomicBoolean owner = new AtomicBoolean(false);
        long start = System.currentTimeMillis();
        try {
            StreamResult result = singleFlight.execute(group, key,
                    Duration.ofSeconds(props.getSingleflightWaitTimeoutSeconds()),
                    () -> {
                        owner.set(true);
                        return streamWithGuards(stage, userId, req, out);
                    });
            if (!owner.get() && result != null) {
                // follower：按 20 字符 / 30ms 回放 owner 的结果
                replay(result, out);
            }
        } catch (Exception e) {
            RemoteException remote = translate(e, stage);
            callLogService.record(buildLog(stage, userId, req, null, false,
                    AiErrorClassifier.classify(remote), remote.getMessage(),
                    System.currentTimeMillis() - start));
            out.onError(remote);
        }
    }

    /**
     * 流式执行体（熔断 / 舱壁 / 重试 / 日志）。
     */
    private StreamResult streamWithGuards(AiStage stage, Long userId, AiRequest req, AiStreamListener out) {
        long start = System.currentTimeMillis();
        if (!rateLimiter.allow(userId)) {
            RemoteException limited = new RemoteException("AI 调用过于频繁，请稍后再试",
                    BaseErrorCode.RATE_LIMITED, AiErrorType.UNAVAILABLE);
            callLogService.record(buildLog(stage, userId, req, null, false, AiErrorType.UNAVAILABLE,
                    limited.getMessage(), System.currentTimeMillis() - start));
            throw limited;
        }
        if (!circuitBreaker.allowRequest()) {
            RemoteException blocked = new RemoteException("AI 熔断已打开，暂时不可用", BaseErrorCode.AI_UNAVAILABLE, AiErrorType.UNAVAILABLE);
            callLogService.record(buildLog(stage, userId, req, null, false, AiErrorType.UNAVAILABLE,
                    blocked.getMessage(), System.currentTimeMillis() - start));
            throw blocked;
        }
        if (!bulkhead.tryAcquire()) {
            RemoteException busy = new RemoteException("AI 服务繁忙，请稍后再试", BaseErrorCode.AI_BUSY, AiErrorType.UNAVAILABLE);
            callLogService.record(buildLog(stage, userId, req, null, false, AiErrorType.UNAVAILABLE,
                    busy.getMessage(), System.currentTimeMillis() - start));
            throw busy;
        }
        try {
            int maxRetries = Math.max(0, props.getMaxRetries());
            RemoteException last = null;
            AiRequest current = req;
            for (int attempt = 0; attempt <= maxRetries; attempt++) {
                StreamResult result = StreamResult.collect(current, provider, out);
                long timeoutMillis = props.stageTimeoutMillis(stage.bizType);
                boolean finished = result.await(timeoutMillis);
                if (finished && result.getError() == null) {
                    circuitBreaker.recordSuccess();
                    AiTextResult textResult = new AiTextResult(result.getContent(), result.getPromptTokens(),
                            result.getCompletionTokens(), System.currentTimeMillis() - start, result.getModel());
                    callLogService.record(buildLog(stage, userId, req, textResult, true, null, null,
                            System.currentTimeMillis() - start));
                    out.onComplete(textResult);
                    return result;
                }
                Throwable err = result.getError() == null ? new java.util.concurrent.TimeoutException("timeout")
                        : result.getError();
                AiErrorType type = finished ? AiErrorClassifier.classify(err) : AiErrorType.TIMEOUT;
                circuitBreaker.recordFailure();
                last = finished
                        ? asRemote(err, type)
                        : new RemoteException("AI 流式调用超时（" + timeoutMillis + "ms）", BaseErrorCode.AI_TIMEOUT, AiErrorType.TIMEOUT);
                // 只有「还没吐出任何增量」时才重试，避免重复下发正文
                if (AiErrorClassifier.retryable(type) && attempt < maxRetries && result.hasNoDelta()) {
                    long delay = backoffMillis(attempt);
                    log.warn("[AiGuard] 流式调用失败准备重试, stage={}, attempt={}, type={}, delay={}ms",
                            stage, attempt, type, delay);
                    sleepQuietly(delay);
                    continue;
                }
                callLogService.record(buildLog(stage, userId, req, null, false, type, last.getMessage(),
                        System.currentTimeMillis() - start));
                throw last;
            }
            callLogService.record(buildLog(stage, userId, req, null, false,
                    AiErrorClassifier.classify(last), last == null ? "AI 流式调用失败" : last.getMessage(),
                    System.currentTimeMillis() - start));
            throw last == null
                    ? new RemoteException("AI 流式调用失败", BaseErrorCode.REMOTE_ERROR, AiErrorType.UNAVAILABLE)
                    : last;
        } finally {
            bulkhead.release();
        }
    }

    /**
     * follower 回放：把 owner 的全文按片推送给自己的 listener。
     */
    private void replay(StreamResult result, AiStreamListener out) {
        try {
            String text = result.getContent() == null ? "" : result.getContent();
            for (int i = 0; i < text.length(); i += REPLAY_CHUNK) {
                out.onDelta(text.substring(i, Math.min(text.length(), i + REPLAY_CHUNK)));
                sleepQuietly(REPLAY_INTERVAL_MS);
            }
            out.onComplete(new AiTextResult(text, result.getPromptTokens(), result.getCompletionTokens(),
                    0L, result.getModel()));
        } catch (Exception e) {
            out.onError(asRemote(e, AiErrorClassifier.classify(e)));
        }
    }

    /* ------------------------------ 健康快照 ------------------------------ */

    /**
     * AI 健康快照：供应商 + 熔断状态 + 舱壁占用。
     *
     * @return 健康快照
     */
    public AiHealthSnapshot health() {
        CircuitBreaker.State state = circuitBreaker.state();
        Map<String, String> states = new LinkedHashMap<>();
        for (AiStage stage : AiStage.values()) {
            states.put(stage.name(), state.name());
        }
        return AiHealthSnapshot.builder()
                .provider(provider.name())
                .model(props.getModel())
                .mock("mock".equalsIgnoreCase(provider.name()))
                .circuitBreakerOpen(state == CircuitBreaker.State.OPEN)
                .circuitBreakerStates(states)
                .bulkheadInUse(bulkhead.inUse())
                .bulkheadTotal(bulkhead.total())
                .build();
    }

    /* ------------------------------ 工具方法 ------------------------------ */

    /**
     * 追加「严格 JSON」提示后重新构造请求。
     */
    private static AiRequest withStrictHint(AiRequest req) {
        String hint = "\n\n【重要】上一次输出不合法，请严格按要求的格式输出，不要输出任何多余文字。";
        return AiRequest.builder()
                .bizType(req.getBizType())
                .systemPrompt(req.getSystemPrompt())
                .userPrompt(req.getUserPrompt() + hint)
                .temperature(req.getTemperature())
                .maxTokens(req.getMaxTokens())
                .model(req.getModel())
                .jsonMode(req.isJsonMode())
                .requestId(req.getRequestId())
                .build();
    }

    /**
     * 指数退避：500ms -&gt; 1500ms。
     */
    /**
     * 计算第 {@code attempt} 次重试的退避休眠时长：指数退避 + 抖动（Equal Jitter）。
     *
     * <p><b>为什么要加抖动</b>：纯指数退避 {@code base * (2^(attempt+1) - 1)} 是确定性的，
     * 同一时刻失败的一批请求会在完全相同的未来时刻集体重试，形成「重试风暴」（thundering herd），
     * 反而把正在恢复的下游服务再次打垮——这是重试治理里最经典的坑。
     *
     * <p>这里采用 Equal Jitter：把退避值劈成两半，一半作为固定基线保住「越往后等越久」的增长趋势，
     * 另一半随机化以打散重试时刻。同时对上限封顶，避免 attempt 偏大时位移溢出导致负数或超长等待。
     *
     * @param attempt 当前重试次数（从 0 开始）
     * @return 实际休眠毫秒数
     */
    private long backoffMillis(int attempt) {
        long base = props.getRetryBaseDelayMillis() <= 0
                ? DEFAULT_BACKOFF_BASE_MILLIS : props.getRetryBaseDelayMillis();
        long expBackoff;
        if (attempt >= MAX_BACKOFF_SHIFT) {
            expBackoff = MAX_BACKOFF_MILLIS;
        } else {
            expBackoff = base * ((1L << (attempt + 1)) - 1L);
            expBackoff = Math.min(expBackoff, MAX_BACKOFF_MILLIS);
        }
        long half = Math.max(1L, expBackoff / 2);
        return half + ThreadLocalRandom.current().nextLong(half + 1);
    }

    /**
     * 归一为 {@link RemoteException}。
     */
    private static RemoteException asRemote(Throwable t, AiErrorType type) {
        if (t instanceof RemoteException remote) {
            return remote;
        }
        return new RemoteException(t == null ? "AI 调用失败" : String.valueOf(t.getMessage()),
                t, BaseErrorCode.REMOTE_ERROR, type);
    }

    /**
     * single-flight 异常翻译：等待超时映射为 C0503。
     */
    private static RemoteException translate(Exception e, AiStage stage) {
        RemoteException remote = asRemote(e, AiErrorClassifier.classify(e));
        String message = remote.getMessage() == null ? "" : remote.getMessage();
        if (message.contains("Single-flight") || message.contains("等待超时")) {
            return new RemoteException("AI 服务响应较慢，请稍后重试", e, BaseErrorCode.AI_WAIT_TIMEOUT, AiErrorType.TIMEOUT);
        }
        log.debug("[AiGuard] 调用失败, stage={}, code={}, msg={}", stage, remote.getErrorCode(), message);
        return remote;
    }

    /**
     * 关闭超时线程池（容器停机时）。
     */
    @jakarta.annotation.PreDestroy
    public void shutdown() {
        timeoutExecutor.shutdownNow();
    }

    /**
     * 构造调用日志。
     */
    private AiCallLogDO buildLog(AiStage stage, Long userId, AiRequest req, AiTextResult result,
                                 boolean success, AiErrorType errorType, String errorMsg, long costMs) {
        AiCallLogDO logDo = new AiCallLogDO();
        logDo.setUserId(userId);
        logDo.setBizType(stage.bizType.name());
        logDo.setProvider(provider.name());
        logDo.setModel(req.getModel());
        logDo.setRequestDigest(req.getUserPrompt());
        logDo.setResponseDigest(result == null ? null : result.getContent());
        logDo.setPromptTokens(result == null ? 0 : nullSafe(result.getPromptTokens()));
        logDo.setCompletionTokens(result == null ? 0 : nullSafe(result.getCompletionTokens()));
        logDo.setCostMs(costMs);
        logDo.setSuccess(success ? 1 : 0);
        logDo.setErrorType(errorType == null ? null : errorType.name());
        logDo.setErrorMsg(errorMsg);
        logDo.setRequestId(req.getRequestId());
        logDo.setCreateTime(java.time.LocalDateTime.now());
        return logDo;
    }

    private static int nullSafe(Integer value) {
        return value == null ? 0 : value;
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 流式调用的一次结果收集器：聚合增量正文 / JSON 段 / token / 异常。
     */
    private static final class StreamResult {

        private final StringBuilder body = new StringBuilder();
        private final StringBuilder json = new StringBuilder();
        private final SectionedStreamParser parser = new SectionedStreamParser();
        private final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        private final AtomicBoolean hasDelta = new AtomicBoolean(false);

        private volatile Throwable error;
        private volatile Integer promptTokens = 0;
        private volatile Integer completionTokens = 0;
        private volatile String model = "";

        /**
         * 发起一次真实流式调用并把增量转发给外部 listener。
         */
        static StreamResult collect(AiRequest req, AiProvider provider, AiStreamListener out) {
            StreamResult result = new StreamResult();
            provider.streamChat(req, result.listener(out));
            return result;
        }

        private StreamResult() {
        }

        private AiStreamListener listener(AiStreamListener out) {
            return new AiStreamListener() {
                @Override
                public void onDelta(String delta) {
                    if (delta == null || delta.isEmpty()) {
                        return;
                    }
                    hasDelta.set(true);
                    String visible = parser.onDelta(delta);
                    body.append(visible);
                    if (visible != null && !visible.isEmpty() && out != null) {
                        out.onDelta(visible);
                    }
                }

                @Override
                public void onComplete(AiTextResult result) {
                    json.append(parser.hasDelimiter() ? parser.jsonPart() : body);
                    promptTokens = result.getPromptTokens();
                    completionTokens = result.getCompletionTokens();
                    model = result.getModel();
                    latch.countDown();
                }

                @Override
                public void onError(Throwable t) {
                    error = t;
                    latch.countDown();
                }
            };
        }

        boolean await(long timeoutMillis) {
            try {
                return latch.await(timeoutMillis, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        boolean hasNoDelta() {
            return !hasDelta.get();
        }

        String getContent() {
            return json.length() > 0 ? json.toString() : body.toString();
        }

        Throwable getError() {
            return error;
        }

        Integer getPromptTokens() {
            return promptTokens;
        }

        Integer getCompletionTokens() {
            return completionTokens;
        }

        String getModel() {
            return model;
        }
    }
}

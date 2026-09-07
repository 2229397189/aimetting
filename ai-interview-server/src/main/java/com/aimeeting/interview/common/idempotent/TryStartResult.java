package com.aimeeting.interview.common.idempotent;

import lombok.Data;

/**
 * 幂等「开始执行」的三态判定结果。
 *
 * <p>对应 BR-07 双键模型：
 * <ul>
 *   <li>{@link Status#NEW} —— 首次请求，调用方可以执行真实业务逻辑。</li>
 *   <li>{@link Status#PROCESSING} —— 上一次请求仍在处理中，前端应提示「处理中」。</li>
 *   <li>{@link Status#SUCCEEDED} —— 上一次请求已完成，直接回放 {@link #replay}，
 *       不重复调用 LLM。</li>
 * </ul>
 *
 * @param <T> 回放结果类型
 */
@Data
public class TryStartResult<T> {

    /** 当前幂等状态。 */
    private Status status;

    /** 回放数据，仅在 {@link Status#SUCCEEDED} 时非空。 */
    private T replay;

    /**
     * 构造首次请求结果。
     *
     * @param <T> 回放数据类型
     * @return NEW 状态结果
     */
    public static <T> TryStartResult<T> newRequest() {
        TryStartResult<T> result = new TryStartResult<>();
        result.setStatus(Status.NEW);
        return result;
    }

    /**
     * 构造处理中结果。
     *
     * @param <T> 回放数据类型
     * @return PROCESSING 状态结果
     */
    public static <T> TryStartResult<T> processing() {
        TryStartResult<T> result = new TryStartResult<>();
        result.setStatus(Status.PROCESSING);
        return result;
    }

    /**
     * 构造已完成（可回放）结果。
     *
     * @param replay 上次成功的结果
     * @param <T>    回放数据类型
     * @return SUCCEEDED 状态结果
     */
    public static <T> TryStartResult<T> succeeded(T replay) {
        TryStartResult<T> result = new TryStartResult<>();
        result.setStatus(Status.SUCCEEDED);
        result.setReplay(replay);
        return result;
    }

    /**
     * 幂等三态。
     */
    public enum Status {

        /** 新请求，可执行。 */
        NEW,

        /** 正在处理中。 */
        PROCESSING,

        /** 已成功，可回放。 */
        SUCCEEDED
    }
}

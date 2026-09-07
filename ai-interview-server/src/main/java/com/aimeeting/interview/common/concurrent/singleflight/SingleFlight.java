package com.aimeeting.interview.common.concurrent.singleflight;

import java.time.Duration;
import java.util.concurrent.Callable;

/**
 * Single-flight 抽象：同一 {@code group + key} 的并发请求中，只有一个 owner 真正执行
 * {@code loader}，其余 follower 阻塞等待并复用 owner 的结果。
 *
 * <p>用途：AI 出题 / 评分 / 报告生成场景下，避免同一请求被重复触发导致 LLM 调用放大
 * （验收标准：并发 10 个相同请求只产生 1 次真实调用）。
 */
public interface SingleFlight {

    /**
     * 执行 single-flight 调用。
     *
     * @param group      业务分组（如 AI 阶段名），用于隔离不同业务
     * @param key        请求指纹（如 prompt MD5）
     * @param waitTimeout follower 最长等待时间，超时抛 {@link FlightWaitTimeoutException}
     * @param loader      真实业务逻辑（仅 owner 执行）
     * @param <T>         返回类型
     * @return owner 或 follower 得到的同一份结果
     * @throws FlightWaitTimeoutException follower 等待超时
     * @throws Exception                  loader 抛出的异常（原样向所有参与者传播）
     */
    <T> T execute(String group, String key, Duration waitTimeout, Callable<T> loader) throws Exception;
}

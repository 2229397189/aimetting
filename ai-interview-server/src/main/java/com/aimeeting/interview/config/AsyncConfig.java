package com.aimeeting.interview.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 异步线程池配置。
 *
 * <p>约定：
 * <ul>
 *   <li>{@code aiInterviewExecutor} —— 业务侧异步任务（幂等审计落库、AI 调用日志等）。</li>
 *   <li>拒绝策略为 {@code CallerRunsPolicy}：线程池打满时回退到调用线程执行，
 *       保证旁路逻辑（日志/审计）不静默丢失。</li>
 * </ul>
 */
@Slf4j
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    /** 核心线程数。 */
    private static final int CORE_POOL_SIZE = 4;

    /** 最大线程数。 */
    private static final int MAX_POOL_SIZE = 16;

    /** 队列容量。 */
    private static final int QUEUE_CAPACITY = 500;

    /** 业务异步执行器 Bean 名称。 */
    public static final String EXECUTOR_NAME = "aiInterviewExecutor";

    /**
     * 业务异步执行器。
     *
     * @return 线程池执行器
     */
    @Bean(EXECUTOR_NAME)
    public Executor aiInterviewExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(CORE_POOL_SIZE);
        executor.setMaxPoolSize(MAX_POOL_SIZE);
        executor.setQueueCapacity(QUEUE_CAPACITY);
        executor.setThreadNamePrefix("ai-interview-async-");
        executor.setKeepAliveSeconds(60);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return aiInterviewExecutor();
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, obj) ->
                log.error("[Async] 异步任务异常, method={}, msg={}", method.getName(), throwable.getMessage(), throwable);
    }
}

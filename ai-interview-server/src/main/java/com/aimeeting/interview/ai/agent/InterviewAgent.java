package com.aimeeting.interview.ai.agent;

import com.aimeeting.interview.ai.model.AiBizType;
import com.aimeeting.interview.ai.model.AiStage;

/**
 * 多智能体统一契约：每个 Agent 通过 {@link #run(AgentContext, Object)} 消费输入、产出结果，
 * 自身只描述「身份（id）/ 业务类型（bizType）/ 调用阶段（stage）」，与具体编排解耦。
 *
 * @param <TIn>  输入类型
 * @param <TOut> 输出类型
 */
public interface InterviewAgent<TIn, TOut> {

    /** Agent 角色标识。 */
    AgentId id();

    /** 业务类型（决定模型 / 超时 / 日志分组）。 */
    AiBizType bizType();

    /** 调用阶段（决定超时 / 单飞分组 / 熔断维度）。 */
    AiStage stage();

    /** 执行一次 Agent 推理。 */
    TOut run(AgentContext ctx, TIn input);
}

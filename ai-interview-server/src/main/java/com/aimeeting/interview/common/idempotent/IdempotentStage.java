package com.aimeeting.interview.common.idempotent;

/**
 * 幂等阶段枚举。
 *
 * <p>作为幂等键的命名空间，保证「答题提交」「报告生成」「简历解析」「题目生成」
 * 之间的 clientToken 互不干扰。
 */
public enum IdempotentStage {

    /** 提交答案（含评分 + 追问）。 */
    ANSWER_SUBMIT,

    /** 生成面试报告。 */
    REPORT_GENERATE,

    /** 简历解析。 */
    RESUME_PARSE,

    /** AI 出题。 */
    QUESTION_GENERATE
}

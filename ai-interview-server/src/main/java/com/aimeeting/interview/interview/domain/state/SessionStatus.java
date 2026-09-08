package com.aimeeting.interview.interview.domain.state;

/**
 * 面试会话状态（与前端 {@code SessionStatus} 一一对应）。
 *
 * <p>合法流转见 {@link InterviewSessionStateMachine}。
 */
public enum SessionStatus {

    /** 已创建，未开始。 */
    INIT,

    /** 答题中（等待用户作答）。 */
    ASKING,

    /** AI 评分中。 */
    EVALUATING,

    /** 追问中（等待用户回答追问）。 */
    FOLLOW_UP,

    /** 已暂停。 */
    PAUSED,

    /** 已完成（终态）。 */
    COMPLETED,

    /** 已放弃（终态）。 */
    ABORTED;

    /**
     * 是否终态（终态不可再流转）。
     *
     * @return COMPLETED / ABORTED 返回 true
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == ABORTED;
    }

    /**
     * 是否活跃（非终态）。
     *
     * @return 非终态返回 true
     */
    public boolean isActive() {
        return !isTerminal();
    }
}

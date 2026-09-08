package com.aimeeting.interview.interview.domain.state;

import com.aimeeting.interview.common.convention.errorcode.BaseErrorCode;
import com.aimeeting.interview.common.convention.exception.ServiceException;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 面试会话状态机（纯静态，无 Spring 依赖，可 100% 单测）。
 *
 * <p>合法跳转：
 * <ul>
 *   <li>INIT → ASKING / ABORTED</li>
 *   <li>ASKING → EVALUATING / PAUSED / ABORTED / COMPLETED</li>
 *   <li>EVALUATING → FOLLOW_UP / ASKING / COMPLETED / PAUSED</li>
 *   <li>FOLLOW_UP → EVALUATING / ASKING / COMPLETED / PAUSED</li>
 *   <li>PAUSED → INIT / ASKING / EVALUATING / FOLLOW_UP / ABORTED（恢复到 prevStatus，动态校验）</li>
 *   <li>COMPLETED / ABORTED：终态，无任何出边</li>
 * </ul>
 *
 * <p>非法跳转抛 {@link ServiceException}（{@code B0301} → HTTP 409）。
 */
public final class InterviewSessionStateMachine {

    private static final Map<SessionStatus, Set<SessionStatus>> ALLOWED = new EnumMap<>(SessionStatus.class);

    static {
        ALLOWED.put(SessionStatus.INIT, EnumSet.of(SessionStatus.ASKING, SessionStatus.ABORTED));
        ALLOWED.put(SessionStatus.ASKING,
                EnumSet.of(SessionStatus.EVALUATING, SessionStatus.PAUSED,
                        SessionStatus.ABORTED, SessionStatus.COMPLETED));
        ALLOWED.put(SessionStatus.EVALUATING,
                EnumSet.of(SessionStatus.FOLLOW_UP, SessionStatus.ASKING,
                        SessionStatus.COMPLETED, SessionStatus.PAUSED));
        ALLOWED.put(SessionStatus.FOLLOW_UP,
                EnumSet.of(SessionStatus.EVALUATING, SessionStatus.ASKING,
                        SessionStatus.COMPLETED, SessionStatus.PAUSED));
        ALLOWED.put(SessionStatus.PAUSED,
                EnumSet.of(SessionStatus.INIT, SessionStatus.ASKING, SessionStatus.EVALUATING,
                        SessionStatus.FOLLOW_UP, SessionStatus.ABORTED));
        ALLOWED.put(SessionStatus.COMPLETED, EnumSet.noneOf(SessionStatus.class));
        ALLOWED.put(SessionStatus.ABORTED, EnumSet.noneOf(SessionStatus.class));
    }

    private InterviewSessionStateMachine() {
    }

    /**
     * 判断是否可以流转。
     *
     * @param from 源状态
     * @param to   目标状态
     * @return 合法返回 true
     */
    public static boolean canTransit(SessionStatus from, SessionStatus to) {
        if (from == null || to == null) {
            return false;
        }
        Set<SessionStatus> targets = ALLOWED.get(from);
        return targets != null && targets.contains(to);
    }

    /**
     * 校验流转，非法时抛 {@code B0301}。
     *
     * @param from 源状态
     * @param to   目标状态
     */
    public static void check(SessionStatus from, SessionStatus to) {
        if (!canTransit(from, to)) {
            throw new ServiceException(
                    "当前会话状态不允许该操作：" + from + " -> " + to,
                    BaseErrorCode.ILLEGAL_STATUS_TRANSITION);
        }
    }

    /**
     * 取某状态的全部合法后继。
     *
     * @param from 源状态
     * @return 后继状态集合（不可变）
     */
    public static Set<SessionStatus> nextStates(SessionStatus from) {
        if (from == null) {
            return Collections.emptySet();
        }
        Set<SessionStatus> targets = ALLOWED.get(from);
        return targets == null ? Collections.emptySet() : Collections.unmodifiableSet(targets);
    }
}

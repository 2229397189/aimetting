package com.aimeeting.interview.interview.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

/**
 * 纯逻辑单测：FollowUpAgent 的 LimitGuard（decideProbeType）判定（不启动 Spring、不发真实请求）。
 */
class FollowUpAgentTest {

    @Test
    void decideProbeType_stopWhenCountExceedsMax() {
        assertEquals("STOP", FollowUpAgentServiceImpl.decideProbeType(3, 3), "达到上限应 STOP");
        assertEquals("STOP", FollowUpAgentServiceImpl.decideProbeType(4, 3), "超过上限应 STOP");
    }

    @Test
    void decideProbeType_deepenWhenUnderLimit() {
        assertEquals("DEEPEN", FollowUpAgentServiceImpl.decideProbeType(0, 3));
        assertEquals("DEEPEN", FollowUpAgentServiceImpl.decideProbeType(1, 3));
        assertEquals("DEEPEN", FollowUpAgentServiceImpl.decideProbeType(2, 3));
    }
}

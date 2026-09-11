package com.aimeeting.interview.interview.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 领域策略纯逻辑单测：{@link FollowUpLimitPolicy} 与 {@link ScorePolicy} 的边界验证。
 */
class DomainPolicyTest {

    /* ------------------------------ FollowUpLimitPolicy ------------------------------ */

    @Test
    @DisplayName("canFollowUp：在 max 上限内允许追问，达到上限即停止")
    void followUpLimitBoundary() {
        // 已追问 0 / 1 次，max=2 -> 仍可追问
        assertThat(FollowUpLimitPolicy.canFollowUp(0, 2)).isTrue();
        assertThat(FollowUpLimitPolicy.canFollowUp(1, 2)).isTrue();
        // 已追问达到上限 -> 停止（followUpCount >= max）
        assertThat(FollowUpLimitPolicy.canFollowUp(2, 2)).isFalse();
        // 超出上限 -> 停止
        assertThat(FollowUpLimitPolicy.canFollowUp(3, 2)).isFalse();
    }

    @Test
    @DisplayName("canFollowUp：max<=0 时回退默认上限 DEFAULT_MAX=2")
    void followUpLimitFallbackDefault() {
        assertThat(FollowUpLimitPolicy.canFollowUp(1, 0)).isTrue();
        assertThat(FollowUpLimitPolicy.canFollowUp(2, 0)).isFalse();
        assertThat(FollowUpLimitPolicy.canFollowUp(1, -5)).isTrue();
    }

    /* ------------------------------ ScorePolicy ------------------------------ */

    @Test
    @DisplayName("questionScore：无追问时直接采用原答案分")
    void questionScoreNoFollowUp() {
        assertThat(ScorePolicy.questionScore(80, null)).isEqualTo(80);
        assertThat(ScorePolicy.questionScore(80, List.of())).isEqualTo(80);
    }

    @Test
    @DisplayName("questionScore：有追问时按 0.7/0.3 加权并四舍五入")
    void questionScoreWithFollowUp() {
        // 80*0.7 + 90*0.3 = 56 + 27 = 83
        assertThat(ScorePolicy.questionScore(80, List.of(90))).isEqualTo(83);
        // 80*0.7 + 95*0.3 = 56 + 28.5 = 84.5 -> 四舍五入 85
        assertThat(ScorePolicy.questionScore(80, List.of(90, 100))).isEqualTo(85);
    }

    @Test
    @DisplayName("questionScore：分数越界被 clamp 到 [0,100]")
    void questionScoreClampsOutOfRange() {
        assertThat(ScorePolicy.questionScore(150, null)).isEqualTo(100);
        assertThat(ScorePolicy.questionScore(-5, null)).isEqualTo(0);
        // 越界追问分也会被 clamp：200->100, -10->0, 均值 50, 80*0.7+50*0.3=71
        assertThat(ScorePolicy.questionScore(80, List.of(200, -10))).isEqualTo(71);
    }

    @Test
    @DisplayName("totalScore：加权平均保留 1 位小数，空列表返回 0.0")
    void totalScore() {
        assertThat(ScorePolicy.totalScore(List.of(80, 90))).isEqualTo(85.0);
        assertThat(ScorePolicy.totalScore(List.of(80, 83))).isEqualTo(81.5);
        assertThat(ScorePolicy.totalScore(List.of())).isEqualTo(0.0);
        assertThat(ScorePolicy.totalScore(null)).isEqualTo(0.0);
    }
}

package com.aimeeting.interview.interview.domain.model;

import java.util.List;

/**
 * 计分策略（BR-02，纯函数）。
 *
 * <ul>
 *   <li>单题：有追问时 {@code 原题 * 0.7 + 追问均分 * 0.3}，四舍五入取整；无追问用原题分。</li>
 *   <li>总分：各题得分的加权平均，保留 1 位小数。</li>
 * </ul>
 */
public final class ScorePolicy {

    /** 原题权重。 */
    public static final double ORIGINAL_WEIGHT = 0.70;

    /** 追问权重。 */
    public static final double FOLLOW_UP_WEIGHT = 0.30;

    private ScorePolicy() {
    }

    /**
     * 计算单题最终得分。
     *
     * @param originalScore  原答案得分（0-100）
     * @param followUpScores 追问得分列表，为空表示无追问
     * @return 单题得分（0-100 整数）
     */
    public static int questionScore(int originalScore, List<Integer> followUpScores) {
        int safeOriginal = clamp(originalScore);
        if (followUpScores == null || followUpScores.isEmpty()) {
            return safeOriginal;
        }
        double avg = followUpScores.stream()
                .filter(java.util.Objects::nonNull)
                .mapToInt(ScorePolicy::clamp)
                .average()
                .orElse(safeOriginal);
        double weighted = safeOriginal * ORIGINAL_WEIGHT + avg * FOLLOW_UP_WEIGHT;
        return (int) Math.round(weighted);
    }

    /**
     * 计算会话总分。
     *
     * @param questionScores 各题得分
     * @return 总分（保留 1 位小数）
     */
    public static double totalScore(List<Integer> questionScores) {
        if (questionScores == null || questionScores.isEmpty()) {
            return 0.0;
        }
        double avg = questionScores.stream()
                .filter(java.util.Objects::nonNull)
                .mapToInt(ScorePolicy::clamp)
                .average()
                .orElse(0.0);
        return Math.round(avg * 10.0) / 10.0;
    }

    private static int clamp(int score) {
        return Math.max(0, Math.min(100, score));
    }
}

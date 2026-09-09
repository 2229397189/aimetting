package com.aimeeting.interview.auth.api.io.resp;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 个人数据概览（U-08）：累计面试场次、完成场次、平均分、累计答题数、简历数以及近期趋势。
 *
 * <p>字段与前端 {@code UserStats} 严格一致：{@code totalQuestions} 对应累计答题数，
 * {@code resumeCount} 来自简历表，{@code trend} 为每日场次与平均分趋势。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatsResp implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 累计面试场次。 */
    private Long totalSessions;

    /** 已完成场次（COMPLETED）。 */
    private Long completedSessions;

    /** 平均分，保留 1 位小数。 */
    private BigDecimal averageScore;

    /** 累计答题数。 */
    private Long totalQuestions;

    /** 简历数。 */
    private Long resumeCount;

    /** 近期趋势（默认 7 日）。 */
    @Builder.Default
    private List<TrendPoint> trend = new ArrayList<>();

    /**
     * 单日趋势项，与前端 {@code TrendPoint} 一致。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendPoint implements Serializable {

        private static final long serialVersionUID = 1L;

        /** 日期，格式 yyyy-MM-dd。 */
        private String date;

        /** 当日场次。 */
        private Long count;

        /** 当日平均分，保留 1 位小数。 */
        private BigDecimal score;
    }
}

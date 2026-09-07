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
 * 个人数据概览（U-08）：累计面试场次、完成场次、平均分、最近 7 日趋势。
 *
 * <p>M1 阶段面试相关表尚未落盘，统一返回 0 值；M4/M7 完成后由真实聚合填充。
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
    private Long totalAnswers;

    /** 最近 7 日趋势。 */
    @Builder.Default
    private List<DailyStat> recent7Days = new ArrayList<>();

    /**
     * 单日统计项。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyStat implements Serializable {

        private static final long serialVersionUID = 1L;

        /** 日期，格式 yyyy-MM-dd。 */
        private String date;

        /** 当日场次。 */
        private Long sessionCount;

        /** 当日平均分。 */
        private BigDecimal averageScore;
    }
}

package com.aimeeting.interview.admin.api.io.resp;

import java.io.Serializable;
import java.util.List;
import lombok.Data;

/**
 * 会话趋势与方向分布（与前端 SessionTrend 对应）。
 */
@Data
public class SessionTrendResp implements Serializable {

    private static final long serialVersionUID = 1L;

    private int days;

    private List<String> dates;

    private List<Long> counts;

    private List<Double> avgScores;

    private List<DirectionCount> directionDistribution;

    /**
     * 单一方向的会话计数。
     */
    @Data
    public static class DirectionCount implements Serializable {

        private static final long serialVersionUID = 1L;

        private String direction;

        private String label;

        private Long count;
    }
}

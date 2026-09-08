package com.aimeeting.interview.admin.api.io.resp;

import java.io.Serializable;
import lombok.Data;

/**
 * 管理端总览统计（与前端 OverviewStats 对应）。
 */
@Data
public class OverviewStatsResp implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userCount;

    private Long sessionCount;

    private Long completedCount;

    /** 完成率（0~100，保留 1 位小数）。 */
    private Double completionRate;

    /** 平均分（报告总分均值）。 */
    private Double averageScore;

    private Long questionCount;
}

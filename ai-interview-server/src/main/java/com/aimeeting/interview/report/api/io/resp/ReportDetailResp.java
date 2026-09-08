package com.aimeeting.interview.report.api.io.resp;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.Data;

/**
 * 报告详情（与前端 ReportDetail 对应）。
 */
@Data
public class ReportDetailResp implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long sessionId;

    private Long userId;

    private String sessionNo;

    private BigDecimal totalScore;

    /** 五维分数（dimensionJson）。 */
    private Map<String, Integer> dimensionJson;

    /** 五维分数（dimensions 别名，前端优先取 dimensionJson）。 */
    private Map<String, Integer> dimensions;

    private List<String> highlights;

    private List<String> improvements;

    private List<String> actions;

    private String overallComment;

    private String generatedBy;

    private List<ReportItem> items;

    private List<String> directions;

    private String difficulty;

    private LocalDateTime createdAt;
}

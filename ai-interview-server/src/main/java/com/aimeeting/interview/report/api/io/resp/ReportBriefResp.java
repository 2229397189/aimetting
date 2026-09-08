package com.aimeeting.interview.report.api.io.resp;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/**
 * 报告分页摘要项（与前端 ReportBrief 对应）。
 */
@Data
public class ReportBriefResp implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long sessionId;

    private String sessionNo;

    private BigDecimal totalScore;

    private String generatedBy;

    private String status;

    private List<String> directions;

    private String difficulty;

    private LocalDateTime createdAt;
}

package com.aimeeting.interview.admin.dao.entity;

import java.math.BigDecimal;
import lombok.Data;

/**
 * 会话趋势按天聚合行。
 */
@Data
public class TrendRow {

    private String day;

    private Long cnt;

    private BigDecimal avgScore;
}

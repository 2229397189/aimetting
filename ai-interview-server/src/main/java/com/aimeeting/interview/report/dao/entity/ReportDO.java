package com.aimeeting.interview.report.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 面试报告实体（t_interview_report）。
 */
@Data
@TableName("t_interview_report")
public class ReportDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sessionId;

    private Long userId;

    private BigDecimal totalScore;

    private String dimensionJson;

    private String highlights;

    private String improvements;

    private String actions;

    private String overallComment;

    private String generatedBy;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}

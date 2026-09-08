package com.aimeeting.interview.interview.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 面试会话主表实体（t_interview_session）。
 *
 * <p>列名与 {@code db/schema-mysql.sql} 保持一致，靠
 * {@code map-underscore-to-camel-case=true} 完成下划线 -&gt; 驼峰映射。
 */
@Data
@TableName("t_interview_session")
public class InterviewSessionDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 会话编号：IM + yyyyMMdd + 8 位随机数字。 */
    private String sessionNo;

    /** 所属用户。 */
    private Long userId;

    /** 绑定简历 ID（可空）。 */
    private Long resumeId;

    /** 方向，逗号分隔（JAVA_BACKEND,DATABASE）。 */
    private String directions;

    /** 难度：EASY | MEDIUM | HARD。 */
    private String difficulty;

    /** 总题量。 */
    private Integer totalQuestion;

    /** 当前题号（1-based），0 表示未开始。 */
    private Integer currentIndex;

    /** 会话状态。 */
    private String status;

    /** PAUSED 之前的状态，用于恢复。 */
    private String prevStatus;

    /** 总分。 */
    private BigDecimal score;

    /** 目标岗位 JD 文本。 */
    private String jdText;

    /** 三阶段题量 JSON：{"TECHNICAL":4,"PROJECT":2,"BEHAVIORAL":2}。 */
    private String phasePlan;

    /** 开始时间。 */
    private LocalDateTime startedAt;

    /** 结束时间。 */
    private LocalDateTime finishedAt;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}

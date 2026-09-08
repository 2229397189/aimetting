package com.aimeeting.interview.interview.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 会话题目实体（t_session_question，冗余题干，会话内不可变）。
 */
@Data
@TableName("t_session_question")
public class SessionQuestionDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 会话 ID。 */
    private Long sessionId;

    /** 题号（1-based）。 */
    private Integer questionNo;

    /** 来自题库时的题目 ID，AI 生成为 null。 */
    private Long questionId;

    /** 题面。 */
    private String title;

    /** 考察要点（JSON 数组字符串）。 */
    private String referencePoints;

    /** 来源：AI | BANK。 */
    private String source;

    /** 难度。 */
    private String difficulty;

    /** 所属阶段：TECHNICAL | PROJECT | BEHAVIORAL。 */
    private String phase;

    /** 是否跳过。 */
    private Integer skipped;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}

package com.aimeeting.interview.question.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 题库实体（t_question）。
 *
 * <p>referencePoints / tags 以 JSON 数组字符串存储，读写经 {@code JsonUtil} 转换。
 */
@Data
@TableName("t_question")
public class QuestionDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 方向枚举名，如 JAVA_BACKEND。 */
    private String direction;

    /** 难度枚举名，如 MEDIUM。 */
    private String difficulty;

    /** 题面。 */
    private String title;

    /** 考察要点（JSON 数组字符串）。 */
    private String referencePoints;

    /** 标签（JSON 数组字符串）。 */
    private String tags;

    /** 参考答案 / 解析。 */
    private String analysis;

    /** 来源枚举名。 */
    private String source;

    /** 1 启用 0 停用。 */
    private Integer status;

    /** 录入人。 */
    private Long createdBy;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}

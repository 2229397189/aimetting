package com.aimeeting.interview.interview.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 答题记录实体（t_session_answer，追问以 parent_answer_id 串链）。
 */
@Data
@TableName("t_session_answer")
public class SessionAnswerDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 会话 ID。 */
    private Long sessionId;

    /** 会话题目 ID。 */
    private Long sessionQuestionId;

    /** 作答用户。 */
    private Long userId;

    /** 答案正文。 */
    private String content;

    /** 是否追问答案。 */
    private Integer isFollowUp;

    /** 追问对应的原答案 ID。 */
    private Long parentAnswerId;

    /** 得分 0-100。 */
    private Integer score;

    /** 点评（Markdown）。 */
    private String comment;

    /** 亮点（JSON 数组字符串）。 */
    private String highlights;

    /** 不足（JSON 数组字符串）。 */
    private String gaps;

    /** Δ1：改进后的参考答案。 */
    private String improvedAnswer;

    /** 简历经历真实性/参与度判断（0-100，可空，仅当回答涉及简历项目/实习时由 AI 给出）。 */
    private Integer authenticity;

    /** AI 追问问题（纯 JSON 评分结果中的 followUpQuestion，原答案行有意义）。 */
    private String followUpQuestion;

    /** 评分来源：AI | RULE。 */
    private String evaluatedBy;

    /** 已追问次数（仅原答案行有意义）。 */
    private Integer followUpCount;

    /** 是否跳过（跳过记 0 分）。 */
    private Integer skipped;

    /** 幂等令牌（前端 X-Client-Token）。 */
    private String clientToken;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}

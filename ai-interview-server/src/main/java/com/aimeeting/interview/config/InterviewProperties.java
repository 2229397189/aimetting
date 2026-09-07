package com.aimeeting.interview.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 面试业务规则配置（{@code ai-interview.interview}），对应 PRD 第 4 章业务规则。
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai-interview.interview")
public class InterviewProperties {

    /** 每题最多追问次数（BR-01）。 */
    private int maxFollowUp = 2;

    /** 默认题量（BR-04）。 */
    private int defaultQuestionCount = 8;

    /** 最小题量。 */
    private int minQuestionCount = 3;

    /** 最大题量。 */
    private int maxQuestionCount = 15;

    /** 答案最短字符数（BR-03）。 */
    private int answerMinLength = 10;

    /** 答案最长字符数。 */
    private int answerMaxLength = 5000;

    /** 会话空闲超时分钟数，超时自动 PAUSED（BR-05）。 */
    private int idleTimeoutMinutes = 30;

    /** PAUSED 超过该天数自动 ABORTED（BR-05）。 */
    private int pausedAbortDays = 7;

    /** 简历文件最大体积（MB）。 */
    private int resumeMaxSizeMb = 5;
}

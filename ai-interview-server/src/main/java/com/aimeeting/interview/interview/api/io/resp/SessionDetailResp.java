package com.aimeeting.interview.interview.api.io.resp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * 会话详情（与前端 {@code SessionDetail} 一致）。
 */
@Data
public class SessionDetailResp {

    private Long id;

    private String sessionNo;

    private Long userId;

    private Long resumeId;

    /** 方向列表。 */
    private List<String> directions = new ArrayList<>();

    private String difficulty;

    private Integer totalQuestion;

    /** 当前题号（1-based，0 表示未开始）。 */
    private Integer currentIndex;

    private String status;

    private String prevStatus;

    private BigDecimal score;

    /** Δ1：目标岗位 JD。 */
    private String jdText;

    /** 全部题目（含作答与追问）。 */
    private List<SessionQuestionItemResp> questions = new ArrayList<>();

    private LocalDateTime createdAt;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;
}

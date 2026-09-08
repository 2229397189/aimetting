package com.aimeeting.interview.interview.api.io.resp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * 会话列表项（与前端 {@code SessionBrief} 一致）。
 */
@Data
public class SessionBriefResp {

    private Long id;

    private String sessionNo;

    private Long userId;

    /** 方向列表。 */
    private List<String> directions = new ArrayList<>();

    private String difficulty;

    private Integer totalQuestion;

    /** 当前题号（1-based，0 表示未开始）。 */
    private Integer currentIndex;

    private String status;

    private BigDecimal score;

    private Long resumeId;

    private LocalDateTime createdAt;

    private LocalDateTime finishedAt;
}

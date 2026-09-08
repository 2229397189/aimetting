package com.aimeeting.interview.resume.api.io.resp;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 简历列表 / 基础返回（与前端 ResumeResp 对应）。
 */
@Data
public class ResumeResp implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long userId;

    private String title;

    /** 原文（列表可不返回，detail 返回）。 */
    private String rawText;

    private String fileUrl;

    private Integer score;

    private String advantage;

    /** 建议列表（DB 以 JSON 数组存储，此处还原为列表）。 */
    private List<String> suggestions;

    /** AI / RULE。 */
    private String parsedBy;

    private Boolean isDefault;

    private LocalDateTime createdAt;
}

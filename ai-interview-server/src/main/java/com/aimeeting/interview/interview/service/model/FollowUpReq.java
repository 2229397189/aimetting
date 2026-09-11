package com.aimeeting.interview.interview.service.model;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * 追问生成请求（{@code FollowUpAgent} 的输入）。
 */
@Data
@Builder
public class FollowUpReq {

    private Long userId;

    private Long sessionId;

    private Long sessionQuestionId;

    private Long parentAnswerId;

    private String questionTitle;

    private List<String> referencePoints;

    private String originalAnswer;

    private int followUpCount;

    private int maxFollowUp;
}

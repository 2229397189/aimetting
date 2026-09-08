package com.aimeeting.interview.admin.api.io.resp;

import java.io.Serializable;
import lombok.Data;

/**
 * 前端公开配置（GET /api/config/client，与前端 ClientConfig 对应）。
 */
@Data
public class ClientConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    private Boolean mockMode;

    private Integer minQuestion;

    private Integer maxQuestion;

    private Integer defaultQuestion;

    private Integer maxFollowUp;

    private Integer answerMinLength;

    private Integer answerMaxLength;

    private String appName;
}

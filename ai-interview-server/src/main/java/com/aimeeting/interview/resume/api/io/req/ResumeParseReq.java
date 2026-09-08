package com.aimeeting.interview.resume.api.io.req;

import lombok.Data;

import java.io.Serializable;

/**
 * 简历解析请求（与前端 ResumeParseReq 对应）。
 */
@Data
public class ResumeParseReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 简历标题（可空，空时后端生成默认标题）。 */
    private String title;

    /** 简历原文。 */
    private String content;

    /** 幂等令牌，由前端生成（X-Client-Token）。 */
    private String clientToken;
}

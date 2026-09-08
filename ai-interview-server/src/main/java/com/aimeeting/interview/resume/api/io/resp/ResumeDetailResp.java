package com.aimeeting.interview.resume.api.io.resp;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 简历详情返回（含解析结果），与前端 ResumeDetailResp 对应。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ResumeDetailResp extends ResumeResp implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 结构化解析结果。 */
    private ResumeParsed parsedJson;
}

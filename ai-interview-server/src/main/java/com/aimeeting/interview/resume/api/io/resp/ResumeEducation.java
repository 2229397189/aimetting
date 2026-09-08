package com.aimeeting.interview.resume.api.io.resp;

import lombok.Data;

import java.io.Serializable;

/**
 * 简历中的教育经历（与前端 ResumeEducation 对应）。
 */
@Data
public class ResumeEducation implements Serializable {

    private static final long serialVersionUID = 1L;

    private String school;

    private String major;

    private String degree;

    private String period;
}

package com.aimeeting.interview.resume.api.io.resp;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 简历中的项目经历（与前端 ResumeProject 对应）。
 */
@Data
public class ResumeProject implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;

    private String role;

    private String description;

    private List<String> techStack;
}

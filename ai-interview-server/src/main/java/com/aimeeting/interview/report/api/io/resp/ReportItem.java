package com.aimeeting.interview.report.api.io.resp;

import java.io.Serializable;
import java.util.List;
import lombok.Data;

/**
 * 报告逐题点评项（与前端 ReportItem 对应）。
 */
@Data
public class ReportItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer questionNo;

    private String title;

    private String answer;

    private Integer score;

    private String comment;

    private List<String> highlights;

    private List<String> gaps;

    private String improvedAnswer;

    private List<String> referencePoints;

    private String difficulty;

    private Boolean skipped;

    private String phase;

    /** 简历经历真实性/参与度判断（0-100，可空）。 */
    private Integer authenticity;
}

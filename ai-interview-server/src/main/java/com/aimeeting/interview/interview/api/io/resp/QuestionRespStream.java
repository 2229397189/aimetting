package com.aimeeting.interview.interview.api.io.resp;

import java.util.List;
import lombok.Data;

/**
 * 首题 / 下一题返回（阻塞式，与前端 QuestionRespStream 一致）。
 */
@Data
public class QuestionRespStream {
    private Integer questionNo;
    private Long sessionQuestionId;
    private String title;
    private List<String> referencePoints;
    private String difficulty;
    private String source;
    private Integer totalQuestion;
    private String phase;
}

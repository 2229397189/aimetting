package com.aimeeting.interview.question.api.io.req;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.Data;

/**
 * 题目新增 / 修改请求。
 */
@Data
public class QuestionSaveReq {
    private Long id;

    @NotBlank(message = "方向不能为空")
    private String direction;

    @NotBlank(message = "难度不能为空")
    private String difficulty;

    @NotBlank(message = "题面不能为空")
    private String title;

    private List<String> referencePoints;

    private List<String> tags;

    private String analysis;

    private Integer status = 1;
}

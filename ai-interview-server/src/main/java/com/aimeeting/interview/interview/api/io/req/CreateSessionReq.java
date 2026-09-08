package com.aimeeting.interview.interview.api.io.req;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import lombok.Data;

/**
 * 创建会话请求（与前端 {@code CreateSessionReq} 一致）。
 */
@Data
public class CreateSessionReq {

    /** 面试方向（至少 1 个）。 */
    @NotEmpty(message = "至少选择一个面试方向")
    private List<String> directions;

    /** 难度：EASY | MEDIUM | HARD。 */
    @NotNull(message = "难度不能为空")
    private String difficulty;

    /** 题量 3~15（BR-04）。 */
    @NotNull(message = "题量不能为空")
    private Integer totalQuestion;

    /** 绑定简历（可空）。 */
    private Long resumeId;

    /** Δ1：目标岗位 JD 文本。 */
    @Size(max = 3000, message = "JD 文本不能超过 3000 字")
    private String jdText;

    /** Δ1：三阶段题量，为空时后端按 50/30/20 计算。 */
    private Map<String, Integer> phasePlan;
}

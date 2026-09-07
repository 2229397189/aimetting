package com.aimeeting.interview.question.api.io.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 难度选项（前端 DirectionsResp.difficulties 元素）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DifficultyOption {
    private String code;
    private String label;
    private Long count;
}

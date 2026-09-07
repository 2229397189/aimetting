package com.aimeeting.interview.question.api.io.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 方向选项（前端 DirectionsResp.directions 元素）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DirectionOption {
    private String code;
    private String label;
    private Long count;
}

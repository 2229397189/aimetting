package com.aimeeting.interview.question.api.io.resp;

import java.util.List;
import lombok.Data;

/**
 * 方向 / 难度枚举列表（公开接口返回，前端字典）。
 */
@Data
public class DirectionsResp {
    private List<DirectionOption> directions;
    private List<DifficultyOption> difficulties;
}

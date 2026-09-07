package com.aimeeting.interview.question.api.io.req;

import java.util.List;
import lombok.Data;

/**
 * 随机抽题请求。
 */
@Data
public class RandomQuestionReq {
    private String direction;
    private String difficulty;
    private Integer count = 5;
    private List<Long> excludeIds;
}

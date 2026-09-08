package com.aimeeting.interview.ai.parser;

import com.aimeeting.interview.ai.model.AiErrorType;
import com.aimeeting.interview.common.convention.exception.RemoteException;
import com.aimeeting.interview.common.convention.errorcode.BaseErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;

/**
 * AI 输出字段校验：分数范围、非空、字符串数组。
 */
public final class AiOutputValidator {

    private AiOutputValidator() {
    }

    /** 0-100 整数，越界/非数字抛 RemoteException(INVALID_RESPONSE)。 */
    public static int scoreInRange(JsonNode node, String field) {
        JsonNode v = node.path(field);
        if (v.isNull() || v.isMissingNode() || !v.isNumber()) {
            throw new RemoteException("AI 输出缺少有效分数字段: " + field,
                    BaseErrorCode.REMOTE_ERROR, AiErrorType.INVALID_RESPONSE);
        }
        int score = v.asInt();
        if (score < 0 || score > 100) {
            throw new RemoteException("AI 输出分数越界: " + field + "=" + score,
                    BaseErrorCode.REMOTE_ERROR, AiErrorType.INVALID_RESPONSE);
        }
        return score;
    }

    public static String nonBlank(JsonNode node, String field) {
        JsonNode v = node.path(field);
        if (v.isNull() || v.isMissingNode() || v.asText("").isBlank()) {
            throw new RemoteException("AI 输出缺少非空字段: " + field,
                    BaseErrorCode.REMOTE_ERROR, AiErrorType.INVALID_RESPONSE);
        }
        return v.asText();
    }

    public static List<String> stringArray(JsonNode node, String field, int minSize) {
        JsonNode v = node.path(field);
        List<String> out = new ArrayList<>();
        if (v.isArray()) {
            for (JsonNode item : v) {
                out.add(item.asText());
            }
        }
        if (out.size() < minSize) {
            throw new RemoteException("AI 输出数组字段不足: " + field + " (need>=" + minSize + ")",
                    BaseErrorCode.REMOTE_ERROR, AiErrorType.INVALID_RESPONSE);
        }
        return out;
    }
}

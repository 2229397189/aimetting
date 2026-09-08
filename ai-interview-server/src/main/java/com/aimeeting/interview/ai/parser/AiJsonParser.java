package com.aimeeting.interview.ai.parser;

import com.aimeeting.interview.ai.model.AiErrorType;
import com.aimeeting.interview.common.convention.exception.RemoteException;
import com.aimeeting.interview.common.convention.errorcode.BaseErrorCode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * AI 输出 JSON 解析：剥离 ```json 围栏 / 前后缀文字，定位首个 '{' 与最后一个 '}'。
 */
public final class AiJsonParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private AiJsonParser() {
    }

    public static String stripFence(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim();
        // 去除 ```json ... ``` 或 ``` ... ```
        int fenceStart = s.indexOf("```");
        if (fenceStart >= 0) {
            int afterFence = s.indexOf("\n", fenceStart) + 1;
            if (afterFence <= 0) {
                afterFence = fenceStart + 3;
            }
            int fenceEnd = s.indexOf("```", afterFence);
            if (fenceEnd > afterFence) {
                s = s.substring(afterFence, fenceEnd);
            }
        }
        int first = s.indexOf('{');
        int last = s.lastIndexOf('}');
        if (first >= 0 && last > first) {
            return s.substring(first, last + 1);
        }
        return s;
    }

    public static JsonNode parse(String raw) {
        try {
            return MAPPER.readTree(stripFence(raw));
        } catch (Exception e) {
            throw new RemoteException("AI 返回内容无法解析为 JSON", e, BaseErrorCode.REMOTE_ERROR, AiErrorType.INVALID_RESPONSE);
        }
    }

    public static <T> T parse(String raw, Class<T> type) {
        try {
            return MAPPER.readValue(stripFence(raw), type);
        } catch (Exception e) {
            throw new RemoteException("AI 返回内容无法解析为 " + type.getSimpleName(),
                    e, BaseErrorCode.REMOTE_ERROR, AiErrorType.INVALID_RESPONSE);
        }
    }

    public static <T> T parse(String raw, TypeReference<T> type) {
        try {
            return MAPPER.readValue(stripFence(raw), type);
        } catch (Exception e) {
            throw new RemoteException("AI 返回内容无法解析", e, BaseErrorCode.REMOTE_ERROR, AiErrorType.INVALID_RESPONSE);
        }
    }
}

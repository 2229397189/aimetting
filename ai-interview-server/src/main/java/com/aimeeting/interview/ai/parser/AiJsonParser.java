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

    /**
     * 尝试把 AI 原始输出修复为合法 JSON 字符串。
     *
     * <p>先剥离 Markdown 围栏与前后缀文字；若剩余内容能被解析为 JSON 对象则返回，否则返回 {@code null}。
     * 用于 {@code response_format=json_object} 偶发失效（模型在 JSON 外附加说明文字甚至纯叙述）时的兜底。</p>
     *
     * @param raw AI 原始输出
     * @return 修复后的 JSON 字符串；无法修复时为 {@code null}
     */
    public static String tryRepairJson(String raw) {
        if (raw == null) {
            return null;
        }
        String cleaned = stripFence(raw).trim();
        if (cleaned.isEmpty()) {
            return null;
        }
        try {
            JsonNode node = MAPPER.readTree(cleaned);
            return (node != null && node.isObject()) ? cleaned : null;
        } catch (Exception e) {
            return null;
        }
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

    /**
     * 同 {@link #parse(String)} 的容错版本：解析失败（含修复后仍非 JSON 对象）时返回 {@code null} 而非抛异常。
     *
     * @param raw AI 原始输出
     * @return JSON 对象节点；无法解析时为 {@code null}
     */
    public static JsonNode tryParseObject(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            JsonNode node = MAPPER.readTree(stripFence(raw));
            return (node != null && node.isObject()) ? node : null;
        } catch (Exception e) {
            return null;
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

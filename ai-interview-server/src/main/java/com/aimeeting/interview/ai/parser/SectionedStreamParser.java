package com.aimeeting.interview.ai.parser;

/**
 * 评分专用分段解析器：正文 + {@code ===JSON===} 分隔符。
 *
 * <p>增量喂入 delta；返回本次可下发给前端的正文片段（未遇到分隔符时全量下发）。
 */
public final class SectionedStreamParser {

    public static final String DELIM = "===JSON===";

    private final StringBuilder buffer = new StringBuilder();
    private boolean delimited = false;

    /** @return 本次应下发给前端的增量正文（遇到分隔符后返回空串）。 */
    public String onDelta(String delta) {
        if (delta == null || delta.isEmpty()) {
            return "";
        }
        if (delimited) {
            return "";
        }
        int idx = delta.indexOf(DELIM);
        if (idx >= 0) {
            delimited = true;
            String head = delta.substring(0, idx);
            buffer.append(head);
            return head;
        }
        // 分隔符可能跨 delta 边界，做前缀保护：若缓冲尾部与分隔符有重叠则暂不下发
        String pending = delta;
        String tail = buffer.length() > 0 ? buffer.substring(Math.max(0, buffer.length() - (DELIM.length() - 1))) : "";
        String combined = tail + delta;
        int cross = combined.indexOf(DELIM);
        if (cross >= 0 && cross >= tail.length()) {
            // 分隔符完整落在本次 delta（已在上面 idx>=0 处理），此处仅保护跨包
            delimited = true;
            String head = delta.substring(0, cross - tail.length());
            buffer.append(head);
            return head;
        }
        buffer.append(pending);
        return pending;
    }

    /** 流结束后调用，返回分隔符之后的严格 JSON 部分。 */
    public String jsonPart() {
        String full = buffer.toString();
        int idx = full.indexOf(DELIM);
        if (idx < 0) {
            return "";
        }
        return full.substring(idx + DELIM.length());
    }

    public boolean hasDelimiter() {
        return delimited;
    }
}

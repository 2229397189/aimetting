package com.aimeeting.interview.report.domain;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 报告五维雷达维度。
 *
 * <p>对应前端 {@code DimensionKey}：PROFESSIONAL / EXPRESSION / LOGIC / PROJECT_DEPTH / POTENTIAL。
 */
public enum ReportDimension {

    PROFESSIONAL("专业技能"),
    EXPRESSION("表达沟通"),
    LOGIC("逻辑思维"),
    PROJECT_DEPTH("项目深度"),
    POTENTIAL("潜力");

    /** 维度中文标签。 */
    public final String label;

    ReportDimension(String label) {
        this.label = label;
    }

    /**
     * 归一化五维评分。
     *
     * <p>规则：缺失维度用传入的总分兜底；全部维度 clamp 到 0-100。
     *
     * @param raw        原始维度分数（键为维度名，可能缺失部分维度）
     * @param totalScore 总分（缺失维度兜底值）
     * @return 五个维度齐全、范围 0-100 的 Map
     */
    public static Map<String, Integer> normalize(Map<String, Integer> raw, int totalScore) {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (ReportDimension dimension : values()) {
            Integer value = raw == null ? null : raw.get(dimension.name());
            int v = (value == null) ? totalScore : clamp(value);
            result.put(dimension.name(), v);
        }
        return result;
    }

    /**
     * 归一化（无总分兜底时缺失维度记为 0，全部 clamp 0-100）。
     *
     * @param raw 原始维度分数
     * @return 五个维度齐全、范围 0-100 的 Map
     */
    public static Map<String, Integer> normalize(Map<String, Integer> raw) {
        return normalize(raw, 0);
    }

    /**
     * 将分数限制在 0-100。
     */
    public static int clamp(int score) {
        return Math.max(0, Math.min(100, score));
    }
}

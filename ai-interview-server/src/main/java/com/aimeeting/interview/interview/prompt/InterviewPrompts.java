package com.aimeeting.interview.interview.prompt;

import java.util.List;

/**
 * 面试域 Prompt 构造（架构文档第 7 章）。
 *
 * <p>评分 prompt 是唯一使用「正文 + {@code ===JSON===}」分段的场景：
 * 分隔符之前是面向用户的 Markdown 点评（流式打字机），之后是严格 JSON 结构化结果。
 */
public final class InterviewPrompts {

    /** 严格 JSON 约束（系统级）。 */
    private static final String STRICT_JSON_RULE =
            "你必须且只能输出一个合法 JSON 对象：不要输出解释、寒暄或前后缀文字；"
                    + "不要使用 Markdown 代码块围栏；所有字符串值使用双引号，不要尾随逗号；中文使用 UTF-8 不要转义。";

    private InterviewPrompts() {
    }

    /**
     * 出题系统提示。
     *
     * @param directionLabel  方向中文名
     * @param phaseLabel      阶段中文名
     * @param difficultyLabel 难度中文名
     * @return 系统提示
     */
    public static String questionSystem(String directionLabel, String phaseLabel, String difficultyLabel) {
        return "你是一位有 10 年经验的" + nullToEmpty(directionLabel) + "技术面试官，当前进行"
                + nullToEmpty(phaseLabel) + "，目标难度为" + nullToEmpty(difficultyLabel) + "。" + STRICT_JSON_RULE;
    }

    /**
     * 出题用户提示。
     *
     * @param directionLabel 方向中文名
     * @param difficulty     难度枚举名
     * @param questionNo     题号
     * @param totalQuestion  总题量
     * @param resumeDigest   简历摘要（可空）
     * @param jdText         目标岗位 JD（可空）
     * @param usedTitles     会话内已用题干（用于去重，可空）
     * @return 用户提示
     */
    public static String questionUser(String directionLabel, String difficulty, int questionNo, int totalQuestion,
                                      String resumeDigest, String jdText, List<String> usedTitles) {
        StringBuilder sb = new StringBuilder();
        sb.append("请为").append(nullToEmpty(directionLabel)).append("方向的候选人出第 ").append(questionNo)
                .append("/").append(totalQuestion).append(" 道面试题（难度 ").append(difficulty).append("）。\n");
        if (jdText != null && !jdText.isBlank()) {
            sb.append("目标岗位 JD：").append(clip(jdText, 1500)).append("\n");
        }
        if (resumeDigest != null && !resumeDigest.isBlank()) {
            sb.append("候选人简历摘要：").append(clip(resumeDigest, 800)).append("\n");
        }
        if (usedTitles != null && !usedTitles.isEmpty()) {
            sb.append("以下问题已经问过，请避免重复：").append(clip(String.join(" / ", usedTitles), 800)).append("\n");
        }
        sb.append("输出 JSON 结构：{\"title\":\"题面\",\"referencePoints\":[\"要点1\",\"要点2\",\"要点3\"],"
                + "\"analysis\":\"参考答案要点\"}；title 控制在 200 字以内。");
        return sb.toString();
    }

    /**
     * 评分系统提示。
     *
     * @return 系统提示
     */
    public static String evaluateSystem() {
        return "你是一位严谨的技术面试官，负责给候选人的回答打分。"
                + "先输出面向候选人的中文点评正文（Markdown，120-240 字，包含亮点与改进建议），"
                + "然后换行输出分隔符 ===JSON===，再输出严格 JSON 结构化结果。" + STRICT_JSON_RULE;
    }

    /**
     * 评分用户提示（追问场景 isFollowUp=true 时提示关注补充深度）。
     *
     * @param title           题面
     * @param referencePoints 考察要点
     * @param answer          用户答案
     * @param isFollowUp      是否追问答案
     * @return 用户提示
     */
    public static String evaluateUser(String title, List<String> referencePoints, String answer, boolean isFollowUp) {
        StringBuilder sb = new StringBuilder();
        sb.append("题目：").append(nullToEmpty(title)).append("\n");
        if (referencePoints != null && !referencePoints.isEmpty()) {
            sb.append("考察要点：").append(String.join("、", referencePoints)).append("\n");
        }
        if (isFollowUp) {
            sb.append("以下是候选人对追问的补充回答，请重点评估补充的深度与准确性。\n");
        }
        sb.append("候选人回答：").append(nullToEmpty(answer)).append("\n");
        sb.append("请按格式输出：点评正文 + 换行 + ===JSON=== + "
                + "{\"score\":78,\"highlights\":[\"...\"],\"gaps\":[\"...\"],\"needFollowUp\":true,"
                + "\"followUpQuestion\":\"追问问题\",\"improvedAnswer\":\"改进后的参考答案\"}。"
                + "score 为 0-100 整数；needFollowUp 表示是否需要继续追问。"
                + (isFollowUp ? "追问场景下 needFollowUp 一般为 false。" : ""));
        return sb.toString();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String clip(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}

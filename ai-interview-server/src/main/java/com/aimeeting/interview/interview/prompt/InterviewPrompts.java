package com.aimeeting.interview.interview.prompt;

import com.aimeeting.interview.ai.guard.PromptSanitizer;
import java.util.List;

/**
 * 面试域 Prompt 构造（架构文档第 7 章）。
 *
 * <p>评分与出题统一走「纯 JSON 模式」：请求带 {@code response_format=json_object}，
 * 模型只返回严格 JSON 对象；评分结果的面向用户点评正文放在 JSON 的 {@code comment} 字段里，
 * 由上层在评分完成后统一下发给前端，避免把原始 JSON 透传为点评。
 *
 * <p>所有来自用户的文本（候选人答案、简历摘要、岗位 JD）在拼装前统一经
 * {@link PromptSanitizer} 清洗，防止提示词注入劫持面试官角色或伪造评分指令。</p>
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
            sb.append("目标岗位 JD：").append(PromptSanitizer.sanitize(clip(jdText, 1500))).append("\n");
        }
        if (resumeDigest != null && !resumeDigest.isBlank()) {
            sb.append("候选人简历摘要：").append(PromptSanitizer.sanitize(clip(resumeDigest, 800))).append("\n");
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
        return "你是一位严谨的技术面试官，负责给候选人的回答打分并给出点评。"
                + "你必须且只能输出一个合法 JSON 对象，不要输出任何解释、寒暄或前后缀文字，不要使用 Markdown 代码块围栏。"
                + "JSON 字段定义："
                + "comment(字符串, 面向候选人的中文点评正文, 120-240 字, 含亮点与改进建议), "
                + "score(整数 0-100), highlights(字符串数组, 回答亮点), gaps(字符串数组, 不足与改进点), "
                + "needFollowUp(布尔, 是否需要继续追问), followUpQuestion(字符串, 需要追问时的具体问题, 不需要则空字符串), "
                + "improvedAnswer(字符串, 改进后的参考答案, 可空)。"
                + "所有字符串使用双引号，不要尾随逗号，中文使用 UTF-8 不要转义。";
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
        sb.append("候选人回答：").append(PromptSanitizer.sanitize(answer, 5000)).append("\n");
        sb.append("请严格按系统提示的 JSON 结构输出：必须包含 comment / score / highlights / gaps / "
                + "needFollowUp / followUpQuestion / improvedAnswer 七个字段。"
                + "score 为 0-100 整数；needFollowUp 表示是否需要继续追问"
                + (isFollowUp ? "（追问场景下一般为 false）。" : "；若 needFollowUp 为 true，followUpQuestion 必须给出具体追问问题。"));
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

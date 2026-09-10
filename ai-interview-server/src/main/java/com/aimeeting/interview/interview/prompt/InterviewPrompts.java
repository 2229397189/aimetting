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
    /**
     * 出题系统提示：默认采用「拷打型」面试官人设（对标字节跳动技术面风格）。
     *
     * <p>拷打型的内核不是刻薄，而是**不放过没有证据的表述**：
     * 沿「结论 → 原理 → 边界 → 取舍 → 验证」逐层施压，对"我负责了""做了优化"
     * "支持高并发""性能稳定"这类空话一律追问具体动作与数字证据。
     * 但严格禁止攻击人格、学历、经历，只针对技术结论、证据和岗位差距施压。
     *
     * @param directionLabel  方向中文名
     * @param phaseLabel      阶段中文名
     * @param difficultyLabel 难度中文名
     * @return 系统提示
     */
    public static String questionSystem(String directionLabel, String phaseLabel, String difficultyLabel) {
        return "你是一位有 10 年经验的" + nullToEmpty(directionLabel) + "技术面试官，当前进行"
                + nullToEmpty(phaseLabel) + "，目标难度为" + nullToEmpty(difficultyLabel) + "。" + STRICT_JSON_RULE
                + "你的面试风格是【拷打型】：节奏快、措辞直接、不粉饰；"
                + "出题要能区分「背过概念」和「真的做过」——优先出需要讲清原理、边界条件、"
                + "失败场景与取舍理由的问题，避免只考名词解释的记忆题。"
                + RESUME_GRILL_STRATEGY;
    }

    /**
     * 简历实习/项目经历真实性拷打策略：当且仅当候选人提供了简历时启用。
     *
     * <p>核心目标不是否定经历，而是用深度追问判断候选人是否真正参与过自己写下的项目/实习：
     * 真实参与者能讲清机制、边界、踩坑与取舍；只背过话术的人会在「失去特异性」时露怯。
     * 方法学综合 STAR/CAR 连环追问、深度测试（meracareer）与「失去特异性即红旗」（thecognitive）。
     */
    private static final String RESUME_GRILL_STRATEGY =
            "【简历拷打】若候选人提供了简历摘要，必须优先从其项目/实习经历里挑 1-2 个最「亮眼」的"
            + "（如「主导/优化/高并发/性能提升」类表述）作为出题与深挖对象，而不是只考通用八股。"
            + "对每个经历按下列层次逐层施压，一次只问一个动作："
            + "1) 做了什么具体动作（S/T/A）：你在其中到底是主导、执行还是配合？具体写了哪部分代码/配置？"
            + "2) 遇到什么坑（Obstacle）：线上或自测时出过什么具体故障？报错/现象是什么？"
            + "3) 怎么解决（Action/Result）：你推理出的根因是什么？改了什么？验证方式是什么？"
            + "4) 取舍（Trade-off）：为什么用 A 不用 B？引入了什么代价或限制？"
            + "5) 验证（Verification）：有没有可量化的结果（QPS/耗时/命中率/数据）？怎么测出来的？"
            + "识别空话的红旗：只重复简历话术、说不出具体人名/数据/时间/流程、被自己写进简历的技术栈问住、"
            + "前后表述矛盾（如先说个人完成、后说团队成果）、对任何反向追问都退回泛泛而谈。"
            + "一旦出现红旗，继续追问并把它写进后续评分的 gaps；若始终无法给出特异性细节，"
            + "在 authenticity（真实性）维度大幅扣分并明确给出「该经历真实性存疑」的判定。";

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
        return "你是一位严谨的技术面试官，采用【拷打型】风格给候选人的回答打分并给出点评。"
                + "你必须且只能输出一个合法 JSON 对象，不要输出任何解释、寒暄或前后缀文字，不要使用 Markdown 代码块围栏。"
                + "JSON 字段定义："
                + "comment(字符串, 面向候选人的中文点评正文, 120-240 字, 含亮点与改进建议), "
                + "score(整数 0-100), highlights(字符串数组, 回答亮点), gaps(字符串数组, 不足与改进点), "
                + "needFollowUp(布尔, 是否需要继续追问), followUpQuestion(字符串, 需要追问时的具体问题, 不需要则空字符串), "
                + "improvedAnswer(字符串, 改进后的参考答案, 可空), "
                + "authenticity(整数 0-100, 仅当回答涉及候选人简历中的项目/实习经历时给出「真实性/参与度」判断；"
                + "与 score 独立：衡量候选人是否真正做过自己写下的经历，而非答得好不好)。"
                + "所有字符串使用双引号，不要尾随逗号，中文使用 UTF-8 不要转义。"
                + SCORING_RUBRIC
                + FOLLOW_UP_STRATEGY
                + AUTHENTICITY_RULE
                + STYLE_BOUNDARY;
    }

    /** 真实性/参与度判定规则：基于「失去特异性即红旗」与 STAR/CAR 深度测试。 */
    private static final String AUTHENTICITY_RULE =
            "【真实性判定】仅当回答涉及简历中的项目/实习经历时才评估 authenticity（否则该字段置 0 或省略）。"
            + "判断依据是候选人对自己写下经历的掌握深度，而非回答是否流畅："
            + "≥80 真实可信：能讲清自己具体做的动作、关键技术决策、踩过的坑、取舍理由与可量化结果，"
            + "并能接住反向追问（如「假设流量翻倍预算不变先改什么」「哪个指标证明你那次修复成功了」）；"
            + "60-79 基本可信但有缺口：大方向对，但细节/数据/失败场景不全，需补证据；"
            + "≤59 真实性存疑：只重复简历话术、说不出人名/数据/时间/流程、被自己写的技术栈问住、"
            + "前后矛盾、回避具体动作——这类回答即使术语正确也应判为「疑似未真正参与」，并在 gaps 写明缺的证据。"
            + "authenticity 与 score 解耦：一个经历可能「讲得漂亮但经不起追问」(score 高 authenticity 低)，"
            + "也可能「确实做过但表达一般」(score 中 authenticity 高)。";

    /** 拷打型评分铁律：按「回答落在哪一层」给分，而不是按「听起来像不像懂」。 */
    private static final String SCORING_RUBRIC =
            "【评分铁律】按回答抵达的层次给分，不要被术语堆砌迷惑："
                    + "只给结论、讲不出原理 → 不超过 40 分；"
                    + "讲清原理但没有边界条件与失败场景 → 不超过 65 分；"
                    + "原理 + 边界 + 取舍理由 + 可验证数据（如 QPS、耗时、命中率）→ 75 分以上；"
                    + "出现「高并发」「做了优化」「性能稳定」「解决了难题」等表述却没有具体动作、"
                    + "数字或验证方式 → 必须扣分，并把这个点写进 gaps，同时置 needFollowUp=true 追问证据；"
                    + "答非所问或完全跑题 → 0-10 分，并在 comment 中直接指出跑题、给出本题应有的回答方向。";

    /** 拷打型追问策略：默认继续追问，只有真正答透才收手。 */
    private static final String FOLLOW_UP_STRATEGY =
            "【追问判定】默认倾向继续追问，只有回答已经「原理 + 边界 + 取舍 + 数据」四层齐全时才置 needFollowUp=false。"
                    + "出现下列任一情况必须置 needFollowUp=true 并在 followUpQuestion 给出具体问题："
                    + "1) 只停留在概念层，说了「是什么」没说「为什么/怎么做」；"
                    + "2) 出现「负责」「优化」「高并发」「稳定」「性能好」等模糊表述而无证据；"
                    + "3) 只讲了优点，没提代价、限制或失败场景；"
                    + "4) 声称用过某项技术却说不清关键参数或踩过的坑。"
                    + "追问方向按「结论 → 原理 → 边界 → 取舍 → 验证」逐层深入，一次只问一个点，"
                    + "例如：讲清实现机制、追问并发下的表现、追问失败如何排查、追问为什么不用另一种方案。";

    /** 风格边界：严厉只针对技术，绝不做人身评价。 */
    private static final String STYLE_BOUNDARY =
            "【边界】措辞可以直接、简短、指出「不够」「没覆盖核心机制」，"
                    + "但严禁嘲讽、贬低、羞辱，严禁评价候选人的学历、经历、智力或求职资格；"
                    + "所有批评必须落在具体技术点上，并给出可执行的改进方向。";

    /**
     * 评分用户提示（追问场景 isFollowUp=true 时提示关注补充深度）。
     *
     * @param title           题面
     * @param referencePoints 考察要点
     * @param answer          用户答案
     * @param isFollowUp      是否追问答案
     * @return 用户提示
     */
    public static String evaluateUser(String title, List<String> referencePoints, String answer, boolean isFollowUp,
                                      String resumeDigest) {
        StringBuilder sb = new StringBuilder();
        sb.append("题目：").append(nullToEmpty(title)).append("\n");
        if (referencePoints != null && !referencePoints.isEmpty()) {
            sb.append("考察要点：").append(String.join("、", referencePoints)).append("\n");
        }
        if (resumeDigest != null && !resumeDigest.isBlank()) {
            sb.append("候选人简历摘要（用于判断回答是否涉及其真实经历、评估 authenticity）：")
                    .append(PromptSanitizer.sanitize(clip(resumeDigest, 800))).append("\n");
        }
        if (isFollowUp) {
            sb.append("以下是候选人对追问的补充回答，请重点评估补充的深度与准确性。\n");
        }
        sb.append("候选人回答：").append(PromptSanitizer.sanitize(answer, 5000)).append("\n");
        sb.append("请严格按系统提示的 JSON 结构输出：必须包含 comment / score / highlights / gaps / "
                + "needFollowUp / followUpQuestion / improvedAnswer 七个字段，"
                + "若回答涉及简历中的项目/实习经历则额外包含 authenticity 字段。"
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

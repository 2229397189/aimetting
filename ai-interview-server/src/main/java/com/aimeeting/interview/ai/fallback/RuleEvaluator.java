package com.aimeeting.interview.ai.fallback;

import com.aimeeting.interview.ai.model.AiBizType;
import com.aimeeting.interview.common.util.JsonUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 规则引擎降级：当 AI 不可用 / 返回非法时，给出可用但不依赖 LLM 的结果。
 *
 * <p>覆盖四类：出题、评分、简历解析、报告生成。评分采用
 * 关键词命中(60%) + 答案长度(20%) + 结构分(20%) 的启发式，输出 0-100 整数与模板点评。
 */
public final class RuleEvaluator {

    private static final Pattern CN_PUNCT = Pattern.compile("[\\p{P}\\s]+");

    private RuleEvaluator() {
    }

    /* ------------------------------ 出题降级 ------------------------------ */

    public static String fallbackQuestionTitle(String direction, String difficulty) {
        return "请结合「" + direction + "」方向（" + difficulty + "）谈谈你的理解，并举一个实际例子说明。";
    }

    public static List<String> fallbackReferencePoints() {
        return Arrays.asList("核心概念", "适用场景", "与其它方案的对比", "常见误区");
    }

    /* ------------------------------ 评分降级 ------------------------------ */

    public static final class RuleScore {
        public int score;
        public String comment;
        public List<String> highlights = new ArrayList<>();
        public List<String> gaps = new ArrayList<>();
        public boolean needFollowUp;
        public String followUpQuestion;
    }

    /**
     * 评分降级。
     *
     * @param referencePoints 题目考察要点（用于关键词命中）
     * @param answer          用户答案
     */
    public static RuleScore fallbackEvaluate(List<String> referencePoints, String answer) {
        RuleScore r = new RuleScore();
        String text = answer == null ? "" : answer.trim();
        if (text.isEmpty()) {
            r.score = 0;
            r.comment = "未作答或答案过短，建议补充完整回答后再提交。";
            r.gaps.add("答案缺失");
            return r;
        }
        // 1) 关键词命中 60%
        double hitRatio = keywordHitRatio(referencePoints, text);
        int keywordScore = (int) Math.round(hitRatio * 60);
        // 2) 长度 20%（>=200 字满分）
        int lenScore = (int) Math.round(Math.min(1.0, text.length() / 200.0) * 20);
        // 3) 结构 20%（分段 / 含列举）
        int structScore = (text.contains("\n") || text.contains("、") || text.contains("，") || text.length() > 80) ? 20 : 10;
        r.score = Math.min(100, keywordScore + lenScore + structScore);

        if (hitRatio >= 0.5) {
            r.highlights.add("覆盖了主要考察要点");
        } else {
            r.gaps.add("未充分覆盖题目考察要点：" + String.join("、", safeSub(referencePoints, 3)));
        }
        if (text.length() < 80) {
            r.gaps.add("回答偏简短，可补充具体例子");
        } else {
            r.highlights.add("回答有具体展开");
        }
        r.needFollowUp = r.score < 75 && r.score >= 40;
        r.followUpQuestion = r.needFollowUp ? "能否就其中一个要点再深入展开说明？" : "";
        r.comment = String.format("（规则引擎评分，未接入大模型）综合得分 %d。%s%s",
                r.score,
                r.highlights.isEmpty() ? "" : "亮点：" + String.join("；", r.highlights) + "。",
                r.gaps.isEmpty() ? "" : "待改进：" + String.join("；", r.gaps) + "。");
        return r;
    }

    /* ------------------------------ 简历解析降级 ------------------------------ */

    public static String fallbackResumeParsed(String rawText) {
        // 简单抽取「技能/项目/学校」关键词
        Set<String> skills = new HashSet<>();
        for (String kw : new String[]{"Java", "Spring", "MySQL", "Redis", "Vue", "React", "Python",
                "Go", "Kafka", "Docker", "Kubernetes", "Linux", "MQ", "微服务", "分布式"}) {
            if (rawText != null && rawText.contains(kw)) {
                skills.add(kw);
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("{\"skills\":").append(JsonUtil.toJson(new ArrayList<>(skills)))
                .append(",\"summary\":\"（规则解析）")
                .append(rawText == null ? "" : rawText.length() > 100 ? rawText.substring(0, 100) : rawText)
                .append("\"}");
        return sb.toString();
    }

    /* ------------------------------ 报告降级 ------------------------------ */

    public static String fallbackReport(List<Integer> scores) {
        int avg = scores.isEmpty() ? 0 : (int) Math.round(scores.stream().mapToInt(Integer::intValue).average().orElse(0));
        return String.format("{\"totalScore\":%d,"
                + "\"dimensions\":{\"PROFESSIONAL\":%d,\"EXPRESSION\":%d,\"LOGIC\":%d,\"PROJECT_DEPTH\":%d,\"POTENTIAL\":%d},"
                + "\"highlights\":[\"完成了若干题目的作答\"],"
                + "\"improvements\":[\"建议加强项目深度的表达\"],"
                + "\"actions\":[\"针对薄弱方向做专项复习\"],"
                + "\"overallComment\":\"（规则报告）本次面试平均得分 %d 分，整体表现%s。\"}",
                avg, avg, Math.max(0, avg - 2), avg, Math.max(0, avg - 3), Math.max(0, avg - 1), avg,
                avg >= 75 ? "良好" : "有待提升");
    }

    /* ------------------------------ 工具 ------------------------------ */

    private static double keywordHitRatio(List<String> referencePoints, String text) {
        if (referencePoints == null || referencePoints.isEmpty() || text == null || text.isBlank()) {
            return 0.3;
        }
        int hit = 0;
        for (String rp : referencePoints) {
            if (rp == null) {
                continue;
            }
            String key = rp.length() > 4 ? rp.substring(0, 4) : rp;
            if (text.contains(key)) {
                hit++;
            }
        }
        return Math.min(1.0, (double) hit / referencePoints.size());
    }

    private static List<String> safeSub(List<String> list, int n) {
        if (list == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(list.subList(0, Math.min(n, list.size())));
    }
}

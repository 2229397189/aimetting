package com.aimeeting.interview.ai.agent.tool.impl;

import com.aimeeting.interview.ai.agent.AgentContext;
import com.aimeeting.interview.ai.agent.skill.AgentTool;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * 简历 × JD 匹配工具（{@code MULTI_AGENT_DESIGN.md} §3.1、§9 M3「AgentTool 实作」）。
 *
 * <p>职责：把「会话目标岗位 JD 关键词」与「简历摘要」做**确定性**命中/缺口分析，
 * 输出文本片段供 ReporterAgent 注入 user prompt，让报告的改进建议与行动建议对齐岗位要求。
 *
 * <p>设计约束：
 * <ul>
 *   <li>零模型调用（不是另一个 Agent，不消耗额度）；</li>
 *   <li>**不新增任何数据库表**——JD 复用 {@code t_interview_session.jd_text}，简历复用会话已解析的摘要；</li>
 *   <li>信息不足（无 JD 或无简历摘要）时返回 {@code null}，由调用方跳过该片段，不阻断报告生成。</li>
 * </ul>
 */
@Component
public class ResumeMatchTool implements AgentTool {

    /** 关键词抽取上限（控制注入 prompt 的 token 成本）。 */
    private static final int MAX_KEYWORDS = 20;

    /** 中文关键词上下限：2-4 字才视为可比较的技术/能力短语。 */
    private static final int HAN_MIN_LENGTH = 2;

    private static final int HAN_MAX_LENGTH = 4;

    /** JD 中的非技术性通用词，命中率无区分度，直接剔除。 */
    private static final Set<String> STOP_WORDS = Set.of(
            "熟悉", "精通", "掌握", "了解", "具备", "具有", "负责", "参与", "要求", "优先",
            "经验", "能力", "相关", "岗位", "职责", "以上", "以及", "良好", "能够", "独立",
            "工作", "团队", "沟通", "完成", "进行", "根据", "通过", "使用");

    @Override
    public String toolId() {
        return "resume_match";
    }

    @Override
    public String description() {
        return "按目标岗位 JD 关键词与简历摘要做确定性匹配，输出命中/缺口关键词，供报告建议对齐岗位";
    }

    @Override
    public String execute(AgentContext ctx) {
        if (ctx == null) {
            return null;
        }
        String jdText = ctx.jdTextOrNull();
        String resumeDigest = ctx.getResumeDigest();
        if (jdText == null || resumeDigest == null || resumeDigest.isBlank()) {
            return null;
        }
        List<String> keywords = extractKeywords(jdText);
        if (keywords.isEmpty()) {
            return null;
        }
        String haystack = resumeDigest.toLowerCase(Locale.ROOT);
        List<String> hit = new ArrayList<>();
        List<String> miss = new ArrayList<>();
        for (String keyword : keywords) {
            if (haystack.contains(keyword.toLowerCase(Locale.ROOT))) {
                hit.add(keyword);
            } else {
                miss.add(keyword);
            }
        }
        if (hit.isEmpty() && miss.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("【岗位匹配片段（ResumeMatchTool）】\n");
        sb.append("目标岗位关键词(").append(keywords.size()).append(")：").append(String.join("、", keywords)).append('\n');
        sb.append("简历已命中(").append(hit.size()).append(")：").append(hit.isEmpty() ? "无" : String.join("、", hit)).append('\n');
        sb.append("简历缺口(").append(miss.size()).append(")：").append(miss.isEmpty() ? "无" : String.join("、", miss)).append('\n');
        sb.append("撰写 improvements / actions 时请优先对齐上述缺口关键词，并给出可验证的补强路径。");
        return sb.toString();
    }

    /**
     * 从 JD 文本抽取可比较关键词：ASCII 技术词（Java / Spring Boot / MySQL / C++…）与 2-4 字中文短语。
     *
     * <p>纯 Han 长串（如「负责后端服务的设计与开发」）视为句子而非关键词，直接丢弃；
     * 该策略偏保守，只影响提示词的丰富度，不影响报告主流程。
     *
     * @param jdText 岗位 JD 文本
     * @return 去重后的关键词（保持出现顺序，最多 {@value #MAX_KEYWORDS} 个）
     */
    List<String> extractKeywords(String jdText) {
        Set<String> keywords = new LinkedHashSet<>();
        String[] tokens = jdText.split("[^\\p{IsHan}\\p{Alnum}+#.]+");
        for (String token : tokens) {
            String candidate = token.trim();
            if (candidate.isEmpty() || candidate.length() < HAN_MIN_LENGTH) {
                continue;
            }
            boolean hasAscii = candidate.chars().anyMatch(c -> c < 128);
            if (hasAscii) {
                if (candidate.length() > 24) {
                    continue;
                }
            } else {
                if (candidate.length() > HAN_MAX_LENGTH || STOP_WORDS.contains(candidate)) {
                    continue;
                }
            }
            keywords.add(candidate);
            if (keywords.size() >= MAX_KEYWORDS) {
                break;
            }
        }
        return new ArrayList<>(keywords);
    }
}

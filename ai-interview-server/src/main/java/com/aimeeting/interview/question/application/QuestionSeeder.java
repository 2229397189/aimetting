package com.aimeeting.interview.question.application;

import com.aimeeting.interview.common.util.JsonUtil;
import com.aimeeting.interview.question.dao.entity.QuestionDO;
import com.aimeeting.interview.question.dao.mapper.QuestionMapper;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 题库种子数据（首次启动或后续升级时，按 title 幂等补齐）。
 *
 * <p>题目用于：内置题库兜底出题、题库浏览、随机抽题与管理员录入参考。
 * 真实面试题目优先由 AI 生成，题库作为降级与展示来源。
 *
 * <p>设计要点：
 * <ul>
 *   <li>题目数据由 {@link QuestionSeedData} 提供（由 tools/gen_question_seed.py 生成，约 2000 条）。</li>
 *   <li>启动时不判断“表是否为空”，而是按 title 去重：仅当库中不存在同 title 题目时才写入，
 *       因此即使已有内置库（含首批手写精选题）也能安全补足，且重复启动不会插入重复数据。</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QuestionSeeder {

    private final QuestionMapper questionMapper;

    @PostConstruct
    public void seed() {
        try {
            // 已存在 title 集合（逻辑删除由 @TableLogic 自动过滤）
            Set<String> existingTitles = new HashSet<>(questionMapper.selectAllTitles());
            int inserted = 0;
            for (Object[] row : QuestionSeedData.SEED) {
                String title = (String) row[2];
                if (existingTitles.contains(title)) {
                    continue;
                }
                existingTitles.add(title); // 同一批内也防重
                QuestionDO q = new QuestionDO();
                q.setDirection((String) row[0]);
                q.setDifficulty((String) row[1]);
                q.setTitle(title);
                q.setReferencePoints(JsonUtil.toJson(split((String) row[3])));
                q.setAnalysis((String) row[4]);
                q.setSource("SEED");
                q.setStatus(1);
                questionMapper.insert(q);
                inserted++;
            }
            log.info("[QuestionSeeder] 本次补齐内置题目 {} 条（去重后不重复写入）", inserted);
        } catch (Exception e) {
            log.warn("[QuestionSeeder] 种子初始化跳过: {}", e.getMessage());
        }
    }

    private List<String> split(String s) {
        List<String> out = new ArrayList<>();
        if (s == null || s.isEmpty()) {
            return out;
        }
        for (String part : s.split("\\|")) {
            out.add(part.trim());
        }
        return out;
    }
}

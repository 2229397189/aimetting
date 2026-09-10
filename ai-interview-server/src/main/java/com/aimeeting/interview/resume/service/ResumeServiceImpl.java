package com.aimeeting.interview.resume.service;

import com.aimeeting.interview.config.AiProperties;
import com.aimeeting.interview.ai.fallback.RuleEvaluator;
import com.aimeeting.interview.ai.guard.AiGuardService;
import com.aimeeting.interview.ai.model.AiBizType;
import com.aimeeting.interview.ai.model.AiRequest;
import com.aimeeting.interview.ai.model.AiStage;
import com.aimeeting.interview.ai.model.AiTextResult;
import com.aimeeting.interview.common.convention.errorcode.BaseErrorCode;
import com.aimeeting.interview.common.convention.exception.ClientException;
import com.aimeeting.interview.common.convention.exception.ServiceException;
import com.aimeeting.interview.common.convention.result.PageInfo;
import com.aimeeting.interview.common.convention.result.PageQuery;
import com.aimeeting.interview.common.idempotent.IdempotencyService;
import com.aimeeting.interview.common.idempotent.IdempotentStage;
import com.aimeeting.interview.common.idempotent.TryStartResult;
import com.aimeeting.interview.ai.guard.PromptSanitizer;
import com.aimeeting.interview.ai.parser.AiJsonParser;
import com.aimeeting.interview.common.util.JsonUtil;
import com.aimeeting.interview.common.util.Md5Util;
import com.aimeeting.interview.resume.api.io.req.ResumeParseReq;
import com.aimeeting.interview.resume.api.io.resp.ResumeDetailResp;
import com.aimeeting.interview.resume.api.io.resp.ResumeParsed;
import com.aimeeting.interview.resume.api.io.resp.ResumeResp;
import com.aimeeting.interview.resume.dao.entity.ResumeDO;
import com.aimeeting.interview.resume.dao.mapper.ResumeMapper;
import com.aimeeting.interview.resume.infrastructure.ResumeTextExtractor;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 简历服务实现（M5）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeServiceImpl implements ResumeService {

    /** 简历文件大小上限（5MB）。 */
    private static final long MAX_FILE_BYTES = 5L * 1024 * 1024;

    /** 出题 prompt 注入的最大摘要长度。 */
    private static final int DIGEST_MAX = 800;

    /** 简历常见关键词（命中任意一个即视为疑似简历，宁可误放不放过真简历）。 */
    private static final Pattern RESUME_KEYWORD = Pattern.compile(
            "学历|教育|项目|实习|工作|技能|经验|邮箱|电话|大学|本科|硕士|博士|工程师|简历|姓名|求职|意向|岗位|"
            + "公司|职责|负责|开发|毕业|学校|专业|年限|年龄|期望|薪资|经历|获奖|证书|"
            + "university|project|skill|resume|java|spring|python|数据库|框架|系统|架构|后端|前端");

    /** 邮箱结构。 */
    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");

    /** 手机号结构（中国大陆 11 位）。 */
    private static final Pattern PHONE = Pattern.compile("(?<![0-9])1[3-9][0-9]{9}(?![0-9])");

    private final ResumeMapper resumeMapper;
    private final AiGuardService aiGuardService;
    private final AiProperties aiProperties;
    private final IdempotencyService idempotencyService;

    @Override
    public Long parse(Long userId, ResumeParseReq req) {
        String content = req == null ? null : req.getContent();
        if (content == null || content.isBlank()) {
            throw new ClientException("简历内容不能为空", BaseErrorCode.PARAM_ERROR);
        }
        return doParse(userId, req.getTitle(), content, req.getClientToken(), null);
    }

    @Override
    public Long upload(Long userId, MultipartFile file, String title) {
        if (file == null || file.isEmpty()) {
            throw new ClientException("文件不能为空", BaseErrorCode.PARAM_ERROR);
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new ClientException("文件大小不能超过 5MB", BaseErrorCode.FILE_TOO_LARGE);
        }
        String text = ResumeTextExtractor.extract(file);
        String name = (title != null && !title.isBlank()) ? title
                : (file.getOriginalFilename() == null ? "简历" : file.getOriginalFilename());
        if (name.contains(".")) {
            name = name.substring(0, name.lastIndexOf('.'));
        }
        return doParse(userId, name, text, null, null);
    }

    private Long doParse(Long userId, String title, String content, String clientToken, String fileUrl) {
        // 规则预检：mock 模式下直接放行（交给规则兜底解析）；真实模式仅在明显不像简历时拦截，避免误杀真实简历
        if (!aiProperties.isMockMode() && !looksLikeResume(content)) {
            throw new ClientException("内容看起来不是简历，请检查后重试", BaseErrorCode.NOT_RESUME_TEXT);
        }
        // 内容级去重：同一用户上传过完全一致的简历且已解析完成，直接复用，避免重复打 DeepSeek（简历解析慢的主要来源之一）
        ResumeDO existed = resumeMapper.selectOne(Wrappers.<ResumeDO>lambdaQuery()
                .eq(ResumeDO::getUserId, userId)
                .eq(ResumeDO::getRawText, content)
                .last("LIMIT 1"));
        if (existed != null && existed.getParsedBy() != null) {
            log.info("[Resume] 命中相同简历内容，复用已解析结果 resumeId={}", existed.getId());
            return existed.getId();
        }
        // 幂等：命中回放键直接返回上次结果
        var idem = idempotencyService.tryStart(IdempotentStage.RESUME_PARSE, userId, "resume:parse",
                clientToken, new TypeReference<Long>() {});
        if (idem.getStatus() == TryStartResult.Status.SUCCEEDED) {
            return idem.getReplay();
        }

        String finalTitle = (title == null || title.isBlank()) ? "简历-" + LocalDateTime.now().toLocalDate() : title;
        ResumeDO resumeDO = new ResumeDO();
        resumeDO.setUserId(userId);
        resumeDO.setTitle(finalTitle);
        resumeDO.setRawText(content);
        resumeDO.setFileUrl(fileUrl);

        // AI 解析：先做 JSON 修复（剥离前后缀文字 / 围栏后取最外层 JSON 对象），
        // 仅当「AI 确实返回了内容、但解析不出合法 JSON」时才外层重试一次（换独立单飞键强制重调）。
        // 传输层失败（网络 / 402 / 超时 / 熔断）不再外层重试：AiGuardService 内部已按退避策略重试过，
        // 外层再重试只会把单次解析的成本放大到数倍，且对 402 这类永久性错误毫无意义。
        String baseKey = "resume:" + userId + ":" + Md5Util.md5Safe(content);
        String repairedJson = null;
        for (int attempt = 1; attempt <= 2 && repairedJson == null; attempt++) {
            String sfKey = attempt == 1 ? baseKey : baseKey + ":retry" + attempt;
            String raw;
            try {
                raw = aiGuardService.execute(AiStage.RESUME_PARSE, sfKey, userId,
                        buildResumeRequest(content), AiTextResult::getContent);
            } catch (ClientException ce) {
                throw ce;
            } catch (Exception e) {
                log.warn("[Resume] AI 调用失败（传输层，内部已重试，不再外层重试）: {}", e.getMessage());
                break;
            }
            repairedJson = AiJsonParser.tryRepairJson(raw);
            if (repairedJson == null) {
                log.warn("[Resume] AI 第 {} 次返回非合法 JSON，准备重试", attempt);
            }
        }

        if (repairedJson == null) {
            log.warn("[Resume] AI 解析失败（已重试），降级 RULE");
            applyRuleFallback(resumeDO, content);
        } else {
            JsonNode root = AiJsonParser.parse(repairedJson);
            if (root.has("isResume") && !root.get("isResume").asBoolean(true)) {
                throw new ClientException("内容看起来不是简历，请检查后重试", BaseErrorCode.NOT_RESUME_TEXT);
            }
            ResumeParsed parsed = ResumeParsed.fromJson(repairedJson);
            resumeDO.setParsedJson(JsonUtil.toJson(parsed));
            resumeDO.setParsedBy("AI");
            JsonNode scoreNode = root.get("score");
            if (scoreNode != null && scoreNode.isNumber()) {
                resumeDO.setScore(clampScore(scoreNode.asInt()));
            }
            resumeDO.setAdvantage(joinTextArray(root.get("advantages")));
            List<String> suggestions = readStringArray(root.get("suggestions"));
            if (suggestions.isEmpty()) {
                suggestions = defaultSuggestions();
            }
            resumeDO.setSuggestions(JsonUtil.toJson(suggestions));
        }

        resumeMapper.insert(resumeDO);
        if (clientToken != null && !clientToken.isBlank()) {
            idempotencyService.markSuccess(IdempotentStage.RESUME_PARSE, userId, "resume:parse",
                    clientToken, resumeDO.getId());
        }
        return resumeDO.getId();
    }

    @Override
    public PageInfo<ResumeResp> page(Long userId, PageQuery q) {
        long pageNum = q.safePageNum();
        long pageSize = q.safePageSize();
        long offset = (pageNum - 1) * pageSize;
        LambdaQueryWrapper<ResumeDO> countWrapper =
                Wrappers.<ResumeDO>lambdaQuery().eq(ResumeDO::getUserId, userId);
        long total = resumeMapper.selectCount(countWrapper);
        List<ResumeDO> list = resumeMapper.selectList(Wrappers.<ResumeDO>lambdaQuery()
                .eq(ResumeDO::getUserId, userId)
                .orderByDesc(ResumeDO::getCreateTime)
                .last("LIMIT " + pageSize + " OFFSET " + offset));
        PageInfo<ResumeResp> pageInfo = new PageInfo<>();
        pageInfo.setList(list.stream().map(this::toResp).toList());
        pageInfo.setTotal(total);
        pageInfo.setPageNum(pageNum);
        pageInfo.setPageSize(pageSize);
        return pageInfo;
    }

    @Override
    public ResumeDetailResp detail(Long userId, Long id) {
        ResumeDO data = requireOwned(userId, id);
        ResumeDetailResp resp = new ResumeDetailResp();
        copyBase(data, resp);
        resp.setRawText(data.getRawText());
        resp.setParsedJson(ResumeParsed.fromJson(data.getParsedJson()));
        return resp;
    }

    @Override
    public void remove(Long userId, Long id) {
        requireOwned(userId, id);
        resumeMapper.deleteById(id);
    }

    @Override
    public void markDefault(Long userId, Long id) {
        requireOwned(userId, id);
        resumeMapper.resetDefault(userId, id);
        ResumeDO upd = new ResumeDO();
        upd.setId(id);
        upd.setIsDefault(1);
        resumeMapper.updateById(upd);
    }

    @Override
    public String digestForPrompt(Long userId, Long resumeId) {
        ResumeDO data = requireOwned(userId, resumeId);
        String base = data.getRawText();
        if (base == null || base.isBlank()) {
            ResumeParsed parsed = ResumeParsed.fromJson(data.getParsedJson());
            if (parsed != null) {
                StringBuilder sb = new StringBuilder();
                if (parsed.getSummary() != null) {
                    sb.append(parsed.getSummary());
                }
                if (parsed.getSkills() != null) {
                    sb.append(" 技能：").append(String.join("、", parsed.getSkills()));
                }
                base = sb.toString();
            }
        }
        if (base == null) {
            base = "";
        }
        return base.length() > DIGEST_MAX ? base.substring(0, DIGEST_MAX) : base;
    }

    /* ------------------------------ 内部工具 ------------------------------ */

    private ResumeDO requireOwned(Long userId, Long id) {
        ResumeDO data = resumeMapper.selectById(id);
        if (data == null) {
            throw new ClientException("简历不存在", BaseErrorCode.PARAM_ERROR);
        }
        if (!data.getUserId().equals(userId)) {
            throw new ServiceException("无权访问该资源", BaseErrorCode.OWNERSHIP_DENIED);
        }
        return data;
    }

    private ResumeResp toResp(ResumeDO data) {
        ResumeResp resp = new ResumeResp();
        copyBase(data, resp);
        resp.setRawText(null);
        return resp;
    }

    private void copyBase(ResumeDO data, ResumeResp resp) {
        resp.setId(data.getId());
        resp.setUserId(data.getUserId());
        resp.setTitle(data.getTitle());
        resp.setFileUrl(data.getFileUrl());
        resp.setScore(data.getScore());
        resp.setAdvantage(data.getAdvantage());
        resp.setParsedBy(data.getParsedBy());
        resp.setIsDefault(data.getIsDefault() != null && data.getIsDefault() == 1);
        resp.setCreatedAt(data.getCreateTime());
        List<String> suggestions = JsonUtil.parse(data.getSuggestions(),
                new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
        resp.setSuggestions(suggestions == null ? new ArrayList<>() : suggestions);
    }

    private AiRequest buildResumeRequest(String content) {
        // 简历原文属于用户输入：拼装前做提示词注入清洗（剥离控制令牌 / 伪造角色行 / 脚本片段），
        // 并按长度截断以控制 token 成本；数据库仍保存未清洗的原文，不影响存档与展示。
        String clipped = content.length() > 8000 ? content.substring(0, 8000) : content;
        String userPrompt = "以下是候选人简历原文：\n" + PromptSanitizer.sanitize(clipped);
        return AiRequest.builder()
                .bizType(AiBizType.RESUME)
                .systemPrompt("你是资深技术招聘官兼简历优化专家。严格只输出 JSON，不要输出任何额外说明文字。"
                        + "JSON 字段：isResume(布尔),skills(字符串数组),projects(数组,name/role/desc/tech),"
                        + "education(数组,school/major/degree/period),workYears(数字),score(0-100整数),"
                        + "advantages(字符串数组),suggestions(字符串数组,至少3条)。")
                .userPrompt(userPrompt)
                .model(aiProperties.getModel())
                .temperature(0.3)
                .maxTokens(900)
                .jsonMode(true)
                .build();
    }

    private void applyRuleFallback(ResumeDO resumeDO, String content) {
        String fb = RuleEvaluator.fallbackResumeParsed(content);
        resumeDO.setParsedJson(fb);
        resumeDO.setParsedBy("RULE");
        resumeDO.setScore(null);
        resumeDO.setAdvantage(null);
        resumeDO.setSuggestions(JsonUtil.toJson(defaultSuggestions()));
    }

    private boolean looksLikeResume(String text) {
        if (text == null || text.length() < 30) {
            return false;
        }
        // 命中任意简历关键词即视为疑似简历
        if (RESUME_KEYWORD.matcher(text).find()) {
            return true;
        }
        // 或包含邮箱 / 手机号结构
        if (EMAIL.matcher(text).find() || PHONE.matcher(text).find()) {
            return true;
        }
        // 或具备「多行 key：value」的简历结构（至少 3 行带中文/英文冒号）
        long colonLines = java.util.Arrays.stream(text.split("\\R"))
                .filter(l -> l.matches(".*[：:].+"))
                .count();
        return colonLines >= 3;
    }

    private int clampScore(int score) {
        return Math.max(0, Math.min(100, score));
    }

    private String joinTextArray(JsonNode node) {
        if (node == null || !node.isArray()) {
            return null;
        }
        List<String> list = new ArrayList<>();
        node.forEach(n -> {
            if (n.isTextual()) {
                list.add(n.asText());
            }
        });
        return list.isEmpty() ? null : String.join("；", list);
    }

    private List<String> readStringArray(JsonNode node) {
        if (node == null || !node.isArray()) {
            return new ArrayList<>();
        }
        List<String> list = new ArrayList<>();
        node.forEach(n -> {
            if (n.isTextual()) {
                list.add(n.asText());
            }
        });
        return list;
    }

    private List<String> defaultSuggestions() {
        return Arrays.asList("建议补充更多量化成果（如性能提升、成本下降的具体数字）",
                "建议突出与目标岗位匹配的技术栈与项目角色",
                "建议补充项目经历的具体职责、产出与复盘");
    }
}

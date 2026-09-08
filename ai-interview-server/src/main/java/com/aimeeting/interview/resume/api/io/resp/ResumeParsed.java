package com.aimeeting.interview.resume.api.io.resp;

import com.aimeeting.interview.common.util.JsonUtil;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 简历结构化解析结果（与前端 ResumeParsed 对应）。
 *
 * <p>AI 返回的字段名可能与前端契约略有差异（如 {@code education} vs {@code educations}、
 * 项目里的 {@code tech}/{@code desc}），这里做兼容性解析，避免脏数据导致解析失败。
 */
@Data
public class ResumeParsed implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<String> skills;

    private List<ResumeProject> projects;

    private List<ResumeEducation> educations;

    private Integer workYears;

    private String summary;

    /**
     * 从 AI 返回的 JSON 字符串兼容解析为 {@link ResumeParsed}。
     *
     * @param json AI 返回的 JSON（含 isResume/skills/projects/education/...）
     * @return 解析结果，解析失败返回 null
     */
    public static ResumeParsed fromJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        JsonNode root = JsonUtil.parse(json, JsonNode.class);
        if (root == null || !root.isObject()) {
            return null;
        }
        ResumeParsed parsed = new ResumeParsed();
        JsonNode skills = root.get("skills");
        if (skills != null && skills.isArray()) {
            List<String> list = new ArrayList<>();
            skills.forEach(n -> {
                if (n.isTextual()) {
                    list.add(n.asText());
                }
            });
            parsed.setSkills(list);
        }
        JsonNode projects = root.get("projects");
        if (projects != null && projects.isArray()) {
            List<ResumeProject> list = new ArrayList<>();
            projects.forEach(p -> {
                if (p.isObject()) {
                    ResumeProject rp = new ResumeProject();
                    rp.setName(text(p, "name"));
                    rp.setRole(text(p, "role"));
                    rp.setDescription(text(p, "description") != null ? text(p, "description") : text(p, "desc"));
                    JsonNode tech = p.get("tech") != null ? p.get("tech") : p.get("techStack");
                    if (tech != null && tech.isArray()) {
                        List<String> ts = new ArrayList<>();
                        tech.forEach(t -> {
                            if (t.isTextual()) {
                                ts.add(t.asText());
                            }
                        });
                        rp.setTechStack(ts);
                    }
                    list.add(rp);
                }
            });
            parsed.setProjects(list);
        }
        JsonNode edu = root.get("educations") != null ? root.get("educations") : root.get("education");
        if (edu != null && edu.isArray()) {
            List<ResumeEducation> list = new ArrayList<>();
            edu.forEach(e -> {
                if (e.isObject()) {
                    ResumeEducation re = new ResumeEducation();
                    re.setSchool(text(e, "school"));
                    re.setMajor(text(e, "major"));
                    re.setDegree(text(e, "degree"));
                    re.setPeriod(text(e, "period"));
                    list.add(re);
                }
            });
            parsed.setEducations(list);
        }
        JsonNode wy = root.get("workYears");
        if (wy != null && wy.isNumber()) {
            parsed.setWorkYears(wy.asInt());
        }
        parsed.setSummary(text(root, "summary"));
        return parsed;
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v != null && v.isTextual() ? v.asText() : null;
    }
}

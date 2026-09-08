package com.aimeeting.interview.resume.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 简历实体（t_resume）。
 *
 * <p>parsed_json 存储 AI 解析的结构化 JSON；advantage / suggestions 以字符串存储
 * （advantage 为单段文本，suggestions 为 JSON 数组），读写经 {@code JsonUtil} 转换。
 */
@Data
@TableName("t_resume")
public class ResumeDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String title;

    private String rawText;

    private String fileUrl;

    private String parsedJson;

    private Integer score;

    private String advantage;

    private String suggestions;

    private String parsedBy;

    private Integer isDefault;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}

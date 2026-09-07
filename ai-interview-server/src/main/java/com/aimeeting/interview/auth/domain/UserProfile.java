package com.aimeeting.interview.auth.domain;

import lombok.Data;

/**
 * 用户资料领域实体（与 User 一对一）。
 *
 * <p>承载面试个性化所需的背景信息：目标岗位、工作年限、自我介绍、手机号。
 */
@Data
public class UserProfile {

    /** 主键。 */
    private Long id;

    /** 关联用户 ID。 */
    private Long userId;

    /** 目标岗位，如「Java 后端工程师」。 */
    private String targetPosition;

    /** 工作年限。 */
    private Integer workYears;

    /** 自我介绍。 */
    private String intro;

    /** 手机号。 */
    private String phone;
}

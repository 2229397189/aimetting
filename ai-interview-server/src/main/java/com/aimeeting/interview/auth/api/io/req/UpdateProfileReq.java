package com.aimeeting.interview.auth.api.io.req;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import lombok.Data;

/**
 * 修改个人资料请求（U-06）。
 *
 * <p>只更新非空字段，便于前端做局部保存。
 */
@Data
public class UpdateProfileReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 昵称。 */
    @Size(max = 64, message = "昵称长度不能超过 64")
    private String nickname;

    /** 头像 URL。 */
    @Size(max = 255, message = "头像地址长度不能超过 255")
    private String avatar;

    /** 邮箱。 */
    @Size(max = 128, message = "邮箱长度不能超过 128")
    private String email;

    /** 目标岗位。 */
    @Size(max = 64, message = "目标岗位长度不能超过 64")
    private String targetPosition;

    /** 工作年限，0-50。 */
    @Min(value = 0, message = "工作年限不能为负数")
    @Max(value = 50, message = "工作年限不能超过 50")
    private Integer workYears;

    /** 自我介绍。 */
    @Size(max = 500, message = "自我介绍长度不能超过 500")
    private String intro;

    /** 手机号。 */
    @Size(max = 32, message = "手机号长度不能超过 32")
    private String phone;
}

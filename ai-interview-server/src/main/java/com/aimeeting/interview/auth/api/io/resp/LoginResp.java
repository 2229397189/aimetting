package com.aimeeting.interview.auth.api.io.resp;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录返回体：令牌 + 用户资料，一次请求即可完成前端初始化（U-02）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResp implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 令牌信息。 */
    private TokenResp token;

    /** 用户资料。 */
    private UserProfileResp user;
}

package com.aimeeting.interview.common.convention.context;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 当前登录用户上下文。
 *
 * <p>关键设计：实例存放在 {@code HttpServletRequest} 的 attribute 中（键为
 * {@link #REQUEST_KEY}），**不使用 ThreadLocal**，避免异步线程 / 线程池复用导致登录态错乱。
 *
 * <p>提供 {@code @CurrentUser} 参数解析器与 {@code AuthInterceptor} 之间唯一的传递契约。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserContext implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 登录态在 request attribute 中的键名。
     *
     * <p>由 {@code AuthInterceptor#preHandle} 写入，
     * 由 {@code CurrentUserMethodArgumentResolver#resolveArgument} 读取。
     */
    public static final String REQUEST_KEY = "AI_INTERVIEW_USER_CONTEXT";

    /** 用户 ID。 */
    private Long userId;

    /** 用户名。 */
    private String username;

    /** 角色：USER / ADMIN。 */
    private String role;

    /**
     * 是否管理员。
     *
     * @return role == ADMIN 时返回 true
     */
    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
}

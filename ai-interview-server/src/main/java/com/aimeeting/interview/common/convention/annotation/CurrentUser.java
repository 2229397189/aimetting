package com.aimeeting.interview.common.convention.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注在 Controller 方法参数上，用于注入当前登录用户上下文。
 *
 * <p>支持三种形参类型（由 {@code CurrentUserMethodArgumentResolver} 分派）：
 * <ul>
 *   <li>{@code UserContext} —— 完整登录态（userId / username / role）</li>
 *   <li>{@code Long} —— 仅注入 userId</li>
 *   <li>{@code String} —— 仅注入 username</li>
 * </ul>
 *
 * <p>登录态由 {@code AuthInterceptor} 写入 **HttpServletRequest attribute**
 * （架构约束：禁止 ThreadLocal），因此不存在线程池复用导致的串号问题。
 *
 * <pre>{@code
 *   @GetMapping("/profile")
 *   public Result<UserProfileResp> profile(@CurrentUser UserContext ctx) { ... }
 * }</pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrentUser {
}

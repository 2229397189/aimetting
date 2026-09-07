package com.aimeeting.interview.common.web;

import com.aimeeting.interview.common.convention.annotation.CurrentUser;
import com.aimeeting.interview.common.convention.context.UserContext;
import com.aimeeting.interview.common.convention.errorcode.BaseErrorCode;
import com.aimeeting.interview.common.convention.exception.ClientException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * {@code @CurrentUser} 参数解析器。
 *
 * <p>关键设计：登录态从 {@code HttpServletRequest} 的 attribute 中读取（键
 * {@link UserContext#REQUEST_KEY}），<b>不使用 ThreadLocal</b>，
 * 因此不存在线程复用导致的登录态串号问题（架构约束 U-03）。
 *
 * <p>支持三种形参类型：
 * <ul>
 *   <li>{@code UserContext} —— 完整登录态</li>
 *   <li>{@code Long}        —— userId</li>
 *   <li>{@code String}      —— username</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CurrentUserMethodArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Object attribute = webRequest.getAttribute(UserContext.REQUEST_KEY, RequestAttributes.SCOPE_REQUEST);
        if (!(attribute instanceof UserContext userContext)) {
            throw new ClientException(BaseErrorCode.TOKEN_MISSING);
        }
        Class<?> parameterType = parameter.getParameterType();
        if (UserContext.class.isAssignableFrom(parameterType)) {
            return userContext;
        }
        if (Long.class.equals(parameterType) || long.class.equals(parameterType)) {
            return userContext.getUserId();
        }
        if (String.class.equals(parameterType)) {
            return userContext.getUsername();
        }
        throw new ClientException("不支持的 @CurrentUser 参数类型: " + parameterType.getName(),
                BaseErrorCode.PARAM_ERROR);
    }
}

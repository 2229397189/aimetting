package com.aimeeting.interview.common.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * SPA 前端回退控制器（history 模式深链支持）。
 *
 * <p>前端使用 {@code createWebHistory()}，URL 形如 {@code /resume}、{@code /interview/setup}。
 * 用户在任意页面刷新时，浏览器会向服务端请求该路径；本控制器将这些「非 API、非静态资源」
 * 的 GET 请求转发到根路径 {@code /}，由 Spring Boot 的欢迎页（classpath:/static/index.html）返回，
 * 再由前端路由渲染对应视图，避免刷新 404。
 *
 * <p>路径约束说明（避免死循环与误拦静态资源）：
 * <ul>
 *   <li>{@code (?!api|assets)}：首段不能是 api / assets，确保后端接口与打包 JS/CSS 不受影响；</li>
 *   <li>{@code (?!.*\\.)}：整段不允带点，排除 {@code /favicon.ico} 等静态文件；</li>
 *   <li>使用 {@code +} 而非 {@code *}：至少 1 个字符，使根路径 {@code /} 不被本控制器匹配，
 *       从而 {@code forward:/} 后由欢迎页处理器托管，不会回环到自身（否则 StackOverflow）。</li>
 * </ul>
 */
@Controller
public class SpaFallbackController {

    private static final String FORWARD_ROOT = "forward:/";

    @RequestMapping(
            value = {
                    "/{s1:^(?!api|assets)(?!.*\\.).+$}",
                    "/{s1:^(?!api|assets)(?!.*\\.).+$}/{s2:^(?!api|assets)(?!.*\\.).+$}",
                    "/{s1:^(?!api|assets)(?!.*\\.).+$}/{s2:^(?!api|assets)(?!.*\\.).+$}/{s3:^(?!api|assets)(?!.*\\.).+$}",
                    "/{s1:^(?!api|assets)(?!.*\\.).+$}/{s2:^(?!api|assets)(?!.*\\.).+$}/{s3:^(?!api|assets)(?!.*\\.).+$}/{s4:^(?!api|assets)(?!.*\\.).+$}"
            },
            method = RequestMethod.GET)
    public String fallback() {
        return FORWARD_ROOT;
    }
}

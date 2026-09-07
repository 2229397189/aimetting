package com.aimeeting.interview.common.util;

import cn.hutool.crypto.digest.DigestUtil;
import java.nio.charset.StandardCharsets;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * MD5 摘要工具，用于题干去重（BR-17）与请求/响应摘要。
 *
 * <p>注意：仅用于非安全场景（去重、摘要），密码一律走 BCrypt。
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Md5Util {

    /**
     * 计算字符串的 32 位小写 MD5。
     *
     * @param plain 原文
     * @return 32 位小写 MD5 串，入参为 null 时返回 null
     */
    public static String md5(String plain) {
        if (plain == null) {
            return null;
        }
        return DigestUtil.md5Hex(plain.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 计算原文的 MD5，空串安全（null -&gt; 空串）。
     *
     * @param plain 原文
     * @return 32 位小写 MD5 串
     */
    public static String md5Safe(String plain) {
        return md5(plain == null ? "" : plain);
    }

    /**
     * 判断原文与摘要是否匹配。
     *
     * @param plain 原文
     * @param digest MD5 摘要
     * @return 匹配返回 true
     */
    public static boolean matches(String plain, String digest) {
        if (plain == null || digest == null) {
            return false;
        }
        return md5(plain).equalsIgnoreCase(digest);
    }

    /**
     * 生成用于日志的短摘要（原文截断 512 字符后取 MD5），避免日志落完整 prompt。
     *
     * @param content 原文
     * @return MD5 摘要
     */
    public static String digestForLog(String content) {
        if (content == null) {
            return "";
        }
        String clipped = content.length() > 512 ? content.substring(0, 512) : content;
        return md5(clipped);
    }
}

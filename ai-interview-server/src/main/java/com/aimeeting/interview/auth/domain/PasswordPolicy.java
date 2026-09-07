package com.aimeeting.interview.auth.domain;

import com.aimeeting.interview.common.convention.errorcode.BaseErrorCode;
import com.aimeeting.interview.common.convention.exception.ClientException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码策略（BR-18）：8-20 位且必须同时含字母与数字；BCrypt(strength=10) 加盐存储。
 *
 * <p>设计为无状态工具类：{@link #ENCODER} 为线程安全的静态单例，
 * 因此可在领域层与单测中直接使用，无需 Spring 容器。
 */
public final class PasswordPolicy {

    /** 最小长度。 */
    public static final int MIN = 8;

    /** 最大长度。 */
    public static final int MAX = 20;

    /** BCrypt 强度。 */
    public static final int STRENGTH = 10;

    /** 线程安全的 BCrypt 编码器。 */
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder(STRENGTH);

    private PasswordPolicy() {
    }

    /**
     * 校验明文密码是否合规，不合规抛 {@code ClientException(A0102)}。
     *
     * @param rawPassword 明文密码
     */
    public static void validate(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < MIN || rawPassword.length() > MAX) {
            throw new ClientException("密码长度必须为 " + MIN + "-" + MAX + " 位",
                    BaseErrorCode.PARAM_LENGTH_INVALID);
        }
        boolean hasLetter = false;
        boolean hasDigit = false;
        for (int i = 0; i < rawPassword.length(); i++) {
            char c = rawPassword.charAt(i);
            if (Character.isLetter(c)) {
                hasLetter = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            }
        }
        if (!hasLetter || !hasDigit) {
            throw new ClientException("密码必须同时包含字母和数字", BaseErrorCode.PARAM_LENGTH_INVALID);
        }
    }

    /**
     * BCrypt 加密。
     *
     * @param rawPassword 明文密码
     * @return BCrypt 摘要
     */
    public static String encode(String rawPassword) {
        return ENCODER.encode(rawPassword);
    }

    /**
     * 明文与摘要比对。
     *
     * @param rawPassword 明文密码
     * @param encodedHash BCrypt 摘要
     * @return 匹配返回 true
     */
    public static boolean matches(String rawPassword, String encodedHash) {
        if (rawPassword == null || encodedHash == null || encodedHash.isBlank()) {
            return false;
        }
        return ENCODER.matches(rawPassword, encodedHash);
    }
}

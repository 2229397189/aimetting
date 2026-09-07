package com.aimeeting.interview.auth.service;

/**
 * JWT 令牌类型。
 *
 * <ul>
 *   <li>{@link #ACCESS} —— 业务访问令牌，有效期 2h</li>
 *   <li>{@link #REFRESH} —— 刷新令牌，有效期 7d，仅用于换发 accessToken</li>
 * </ul>
 */
public enum TokenType {

    /** 访问令牌。 */
    ACCESS,

    /** 刷新令牌。 */
    REFRESH;

    /**
     * 解析字符串为枚举，未知值返回 null。
     *
     * @param value 字符串
     * @return 令牌类型，未知返回 null
     */
    public static TokenType of(String value) {
        if (value == null) {
            return null;
        }
        for (TokenType type : values()) {
            if (type.name().equalsIgnoreCase(value.trim())) {
                return type;
            }
        }
        return null;
    }
}

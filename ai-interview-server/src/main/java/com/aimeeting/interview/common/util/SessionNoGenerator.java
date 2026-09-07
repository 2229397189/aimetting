package com.aimeeting.interview.common.util;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 会话编号生成器：{@code sessionNo = IM + yyyyMMdd + 8 位随机数字}（BR-21）。
 *
 * <p>形如 {@code IM20240907a3f9c1b2}（示例使用数字），单实例内用 {@link SecureRandom}
 * 生成，配合 DB 唯一索引 {@code uk_session_no} 保证全局唯一。
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SessionNoGenerator {

    /** 会话编号前缀。 */
    public static final String PREFIX = "IM";

    /** 日期部分格式。 */
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** 随机数字位数。 */
    private static final int RANDOM_LENGTH = 8;

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * 生成一个会话编号。
     *
     * @return 形如 {@code IM2024090712345678} 的编号
     */
    public static String generate() {
        return generate(LocalDate.now());
    }

    /**
     * 按指定日期生成会话编号（便于单测固定日期部分）。
     *
     * @param date 业务日期
     * @return 会话编号
     */
    public static String generate(LocalDate date) {
        StringBuilder sb = new StringBuilder(PREFIX);
        sb.append(date.format(DATE_FORMATTER));
        for (int i = 0; i < RANDOM_LENGTH; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    /**
     * 判断字符串是否符合会话编号格式。
     *
     * @param value 待校验值
     * @return 合法返回 true
     */
    public static boolean isValid(String value) {
        if (value == null || value.length() != PREFIX.length() + 8 + RANDOM_LENGTH) {
            return false;
        }
        if (!value.startsWith(PREFIX)) {
            return false;
        }
        for (int i = PREFIX.length(); i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}

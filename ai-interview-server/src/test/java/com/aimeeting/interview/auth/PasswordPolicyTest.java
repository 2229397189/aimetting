package com.aimeeting.interview.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aimeeting.interview.auth.domain.PasswordPolicy;
import com.aimeeting.interview.common.convention.errorcode.BaseErrorCode;
import com.aimeeting.interview.common.convention.exception.ClientException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link PasswordPolicy} 单测：长度 / 字符组合校验 + BCrypt 加密比对。
 */
class PasswordPolicyTest {

    @Test
    @DisplayName("合法密码（8-20 位含字母与数字）校验通过")
    void validPassword() {
        PasswordPolicy.validate("test12345");
        PasswordPolicy.validate("a1b2c3d4");
        PasswordPolicy.validate("Abcdefgh123456789012");
    }

    @Test
    @DisplayName("过短密码抛 A0102")
    void tooShortPassword() {
        ClientException exception = assertThrows(ClientException.class,
                () -> PasswordPolicy.validate("a1"));
        assertEquals(BaseErrorCode.PARAM_LENGTH_INVALID.code(), exception.getErrorCode());
    }

    @Test
    @DisplayName("超长密码抛 A0102")
    void tooLongPassword() {
        ClientException exception = assertThrows(ClientException.class,
                () -> PasswordPolicy.validate("a1b2c3d4e5f6g7h8i9j0k"));
        assertEquals(BaseErrorCode.PARAM_LENGTH_INVALID.code(), exception.getErrorCode());
    }

    @Test
    @DisplayName("纯数字密码抛 A0102")
    void pureDigitPassword() {
        ClientException exception = assertThrows(ClientException.class,
                () -> PasswordPolicy.validate("12345678"));
        assertEquals(BaseErrorCode.PARAM_LENGTH_INVALID.code(), exception.getErrorCode());
    }

    @Test
    @DisplayName("纯字母密码抛 A0102")
    void pureLetterPassword() {
        ClientException exception = assertThrows(ClientException.class,
                () -> PasswordPolicy.validate("abcdefgh"));
        assertEquals(BaseErrorCode.PARAM_LENGTH_INVALID.code(), exception.getErrorCode());
    }

    @Test
    @DisplayName("BCrypt 加密后摘要不等于明文，且同明文两次加密结果不同、matches 均为 true")
    void encodeAndMatch() {
        String raw = "test12345";
        String firstHash = PasswordPolicy.encode(raw);
        String secondHash = PasswordPolicy.encode(raw);

        assertNotEquals(raw, firstHash, "密文不能等于明文");
        assertTrue(firstHash.startsWith("$2a$"), "BCrypt 摘要应以 $2a$ 开头");
        assertNotEquals(firstHash, secondHash, "BCrypt 每次加盐，两次摘要应不同");

        assertTrue(PasswordPolicy.matches(raw, firstHash));
        assertTrue(PasswordPolicy.matches(raw, secondHash));
        assertFalse(PasswordPolicy.matches("wrong1234", firstHash));
    }

    @Test
    @DisplayName("空摘要 / 空明文比对返回 false，不抛异常")
    void matchWithNullSafe() {
        assertFalse(PasswordPolicy.matches(null, PasswordPolicy.encode("test12345")));
        assertFalse(PasswordPolicy.matches("test12345", null));
        assertFalse(PasswordPolicy.matches("test12345", ""));
    }
}

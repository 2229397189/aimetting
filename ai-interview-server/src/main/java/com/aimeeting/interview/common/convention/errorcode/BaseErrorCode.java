package com.aimeeting.interview.common.convention.errorcode;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 平台基础错误码枚举。
 *
 * <p>完整覆盖：
 * <ul>
 *   <li>A 类（客户端）：A0001 / A0100~A0104 / A0201~A0203 / A0301 / A0401~A0404 / A0501~A0502</li>
 *   <li>B 类（系统）：B0001 / B0101 / B0103 / B0301~B0303</li>
 *   <li>C 类（远程 AI）：C0001 / C0501~C0504</li>
 * </ul>
 *
 * <p>HTTP 状态映射见
 * {@code com.aimeeting.interview.common.web.HttpStatusResolver}。
 */
@AllArgsConstructor
public enum BaseErrorCode implements IErrorCode {

    /* ==================== A 客户端错误 ==================== */
    CLIENT_ERROR("A0001", "用户端错误"),

    PARAM_ERROR("A0100", "参数错误"),
    PASSWORD_ERROR("A0101", "用户名或密码错误"),
    PARAM_LENGTH_INVALID("A0102", "参数长度不合法"),
    PARAM_VALID_FAIL("A0103", "参数校验失败"),
    USER_EXIST("A0104", "用户名或邮箱已存在"),

    TOKEN_MISSING("A0201", "未登录或 Token 缺失"),
    TOKEN_EXPIRED("A0202", "Token 已过期"),
    REFRESH_TOKEN_EXPIRED("A0203", "登录状态已失效，请重新登录"),

    FORBIDDEN("A0301", "无权限访问该资源"),

    NOT_RESUME_TEXT("A0401", "内容看起来不是简历，请检查后重试"),
    FILE_TYPE_UNSUPPORTED("A0402", "仅支持 TXT / MD / DOCX / PDF 文件"),
    FILE_TOO_LARGE("A0403", "文件大小不能超过 5MB"),
    NOT_FOUND("A0404", "请求的接口不存在"),

    RATE_LIMITED("A0501", "操作过于频繁，请稍后再试"),
    ACCOUNT_LOCKED("A0502", "连续登录失败次数过多，账号已锁定 5 分钟"),

    /* ==================== B 系统错误 ==================== */
    SERVICE_ERROR("B0001", "系统繁忙，请稍后再试"),
    DB_UNAVAILABLE("B0101", "数据服务暂时不可用"),
    ACCOUNT_DISABLED("B0103", "账号已被禁用"),

    ILLEGAL_STATUS_TRANSITION("B0301", "当前会话状态不允许该操作"),
    SESSION_PAUSED("B0302", "会话已暂停，请先恢复后再作答"),
    OWNERSHIP_DENIED("B0303", "无权访问该资源"),

    /* ==================== C 远程 AI 错误 ==================== */
    REMOTE_ERROR("C0001", "远程服务调用失败"),
    AI_UNAVAILABLE("C0501", "AI 服务暂不可用，已切换备用方案"),
    AI_BUSY("C0502", "AI 服务繁忙，请稍后再试"),
    AI_WAIT_TIMEOUT("C0503", "AI 服务响应较慢，请稍后重试"),
    AI_TIMEOUT("C0504", "AI 服务响应超时，已切换备用方案"),

    /**
     * 账户额度耗尽 / 计费异常（供应商返回 402）。
     *
     * <p>属永久性错误：不重试、直接降级，并在日志与调用记录里显式标记，避免被误判为"服务不可用"
     * 而反复重试、放大成本。</p>
     */
    AI_QUOTA_EXHAUSTED("C0505", "AI 额度不足，请检查账户余额后重试");

    private final String code;

    private final String message;

    @Override
    public String code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }
}

package com.aimeeting.interview.common.concurrent.singleflight;

/**
 * follower 等待 owner 结果超时时抛出（对应错误码 C0503）。
 *
 * <p>语义：明确区分「AI 真的慢」与「AI 报错」，让上层可以做不同处理
 * （超时可提示用户重试，错误则直接降级）。
 */
public class FlightWaitTimeoutException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param message 错误描述
     */
    public FlightWaitTimeoutException(String message) {
        super(message);
    }

    /**
     * @param message 错误描述
     * @param cause   原始异常
     */
    public FlightWaitTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}

package com.aimeeting.interview.ai.guard;

import com.aimeeting.interview.ai.model.AiErrorType;
import com.aimeeting.interview.common.convention.exception.RemoteException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;

/**
 * AI 错误归类与重试决策（BR-09）。
 */
public final class AiErrorClassifier {

    private AiErrorClassifier() {
    }

    /**
     * 是否可重试。
     *
     * <p>TIMEOUT / UNAVAILABLE 属瞬时故障，退避后重试有意义；RATE_LIMIT(429) 通常是短时限制，
     * 也可退避重试。而 QUOTA(402 额度耗尽) / PARAMS(401、403 等鉴权或参数问题) 属永久性错误，
     * 重试既不能成功，还会继续消耗调用额度、放大成本并推迟降级，故一律不重试。</p>
     */
    public static boolean retryable(AiErrorType type) {
        return type == AiErrorType.TIMEOUT || type == AiErrorType.UNAVAILABLE
                || type == AiErrorType.RATE_LIMIT;
    }

    /** 从异常对象推断错误类型。 */
    public static AiErrorType classify(Throwable t) {
        if (t == null) {
            return AiErrorType.UNAVAILABLE;
        }
        if (t instanceof RemoteException re) {
            AiErrorType attached = re.getErrorType();
            if (attached != null) {
                return attached;
            }
        }
        Throwable cause = t;
        while (cause != null) {
            if (cause instanceof SocketTimeoutException || cause instanceof TimeoutException) {
                return AiErrorType.TIMEOUT;
            }
            String msg = cause.getMessage();
            if (msg != null) {
                String m = msg.toLowerCase();
                if (m.contains("timeout") || m.contains("timed out")) {
                    return AiErrorType.TIMEOUT;
                }
                if (m.contains("429") || m.contains("rate limit") || m.contains("too many requests")) {
                    return AiErrorType.RATE_LIMIT;
                }
                if (m.contains("connection refused") || m.contains("unknown host")
                        || m.contains("network is unreachable") || m.contains("500")
                        || m.contains("503") || m.contains("502")) {
                    return AiErrorType.UNAVAILABLE;
                }
            }
            cause = cause.getCause();
        }
        return AiErrorType.UNAVAILABLE;
    }
}

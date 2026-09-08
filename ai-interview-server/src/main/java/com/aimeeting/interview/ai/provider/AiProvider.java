package com.aimeeting.interview.ai.provider;

import com.aimeeting.interview.ai.model.AiRequest;
import com.aimeeting.interview.ai.model.AiStreamListener;
import com.aimeeting.interview.ai.model.AiTextResult;

/**
 * AI 供应商契约：真实（DeepSeek）与 Mock 同签名，便于一键切换。
 */
public interface AiProvider {

    /** 供应商名：deepseek | mock。 */
    String name();

    /** 当前是否可用（key 配置、网络预热等）。 */
    boolean available();

    /** 阻塞式调用。 */
    AiTextResult chat(AiRequest request);

    /** 流式调用，增量通过 listener 回传。 */
    void streamChat(AiRequest request, AiStreamListener listener);
}

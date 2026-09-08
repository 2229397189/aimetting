package com.aimeeting.interview.ai.provider;

import com.aimeeting.interview.ai.model.AiRequest;
import com.aimeeting.interview.ai.model.AiStreamListener;
import com.aimeeting.interview.ai.model.AiTextResult;
import com.aimeeting.interview.common.convention.errorcode.BaseErrorCode;
import com.aimeeting.interview.common.convention.exception.RemoteException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSource;

/**
 * OpenAI 兼容协议供应商（DeepSeek 等）。
 *
 * <p>使用 OkHttp 同步/流式调用；流式走 SSE（{@code text/event-stream}），逐行解析
 * {@code data: {...}} 增量并回传给 listener。超时由构造时传入的 OkHttpClient 控制。
 */
@Slf4j
public class OpenAiCompatProvider implements AiProvider {

    private final String baseUrl;
    private final String apiKey;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String defaultModel;

    public OpenAiCompatProvider(String baseUrl, String apiKey, OkHttpClient httpClient,
                                ObjectMapper objectMapper, String defaultModel) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        this.apiKey = apiKey;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.defaultModel = defaultModel;
    }

    @Override
    public String name() {
        return "deepseek";
    }

    @Override
    public boolean available() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public AiTextResult chat(AiRequest request) {
        long start = System.currentTimeMillis();
        String body = buildBody(request, false);
        Request httpReq = buildRequest(body);
        try (Response resp = httpClient.newCall(httpReq).execute()) {
            if (!resp.isSuccessful() || resp.body() == null) {
                throw new RemoteException("AI 服务返回异常: HTTP " + resp.code(),
                        BaseErrorCode.REMOTE_ERROR, com.aimeeting.interview.ai.model.AiErrorType.UNAVAILABLE);
            }
            String text = resp.body().string();
            JsonNode root = objectMapper.readTree(text);
            String content = extractContent(root);
            JsonNode usage = root.path("usage");
            int promptTokens = usage.path("prompt_tokens").asInt(0);
            int completionTokens = usage.path("completion_tokens").asInt(0);
            return new AiTextResult(content, promptTokens, completionTokens,
                    System.currentTimeMillis() - start, modelOf(request));
        } catch (RemoteException e) {
            throw e;
        } catch (Exception e) {
            throw new RemoteException("AI 调用失败: " + e.getMessage(), e,
                    BaseErrorCode.REMOTE_ERROR, com.aimeeting.interview.ai.model.AiErrorType.UNAVAILABLE);
        }
    }

    @Override
    public void streamChat(AiRequest request, AiStreamListener listener) {
        long start = System.currentTimeMillis();
        String body = buildBody(request, true);
        Request httpReq = buildRequest(body);
        StringBuilder full = new StringBuilder();
        int[] tokens = {0, 0};
        try (Response resp = httpClient.newCall(httpReq).execute()) {
            if (!resp.isSuccessful() || resp.body() == null) {
                listener.onError(new RemoteException("AI 流式服务返回异常: HTTP " + resp.code(),
                        BaseErrorCode.REMOTE_ERROR, com.aimeeting.interview.ai.model.AiErrorType.UNAVAILABLE));
                return;
            }
            BufferedSource source = resp.body().source();
            while (!source.exhausted()) {
                String line = source.readUtf8Line();
                if (line == null) {
                    break;
                }
                if (line.startsWith("data:")) {
                    String data = line.substring(5).trim();
                    if ("[DONE]".equals(data)) {
                        break;
                    }
                    try {
                        JsonNode node = objectMapper.readTree(data);
                        if (node.has("error")) {
                            listener.onError(new RemoteException("AI 流式错误: " + node.path("error").path("message").asText(),
                                    BaseErrorCode.REMOTE_ERROR, com.aimeeting.interview.ai.model.AiErrorType.UNAVAILABLE));
                            return;
                        }
                        JsonNode delta = node.path("choices").path(0).path("delta").path("content");
                        if (!delta.isMissingNode() && !delta.isNull()) {
                            String piece = delta.asText();
                            full.append(piece);
                            listener.onDelta(piece);
                        }
                        JsonNode usage = node.path("usage");
                        if (!usage.isMissingNode()) {
                            tokens[0] = usage.path("prompt_tokens").asInt(tokens[0]);
                            tokens[1] = usage.path("completion_tokens").asInt(tokens[1]);
                        }
                    } catch (Exception ignore) {
                        // 跳过无法解析的控制行
                    }
                }
            }
            listener.onComplete(new AiTextResult(full.toString(), tokens[0], tokens[1],
                    System.currentTimeMillis() - start, modelOf(request)));
        } catch (Exception e) {
            listener.onError(new RemoteException("AI 流式调用失败: " + e.getMessage(), e,
                    BaseErrorCode.REMOTE_ERROR, com.aimeeting.interview.ai.model.AiErrorType.UNAVAILABLE));
        }
    }

    private String buildBody(AiRequest request, boolean stream) {
        try {
            List<Map<String, String>> messages = new ArrayList<>();
            if (request.getSystemPrompt() != null && !request.getSystemPrompt().isBlank()) {
                messages.add(Map.of("role", "system", "content", request.getSystemPrompt()));
            }
            messages.add(Map.of("role", "user", "content", request.getUserPrompt()));
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", modelOf(request));
            body.put("messages", messages);
            body.put("stream", stream);
            if (request.getTemperature() != null) {
                body.put("temperature", request.getTemperature());
            }
            if (request.getMaxTokens() != null) {
                body.put("max_tokens", request.getMaxTokens());
            }
            if (request.isJsonMode()) {
                body.put("response_format", Map.of("type", "json_object"));
            }
            return objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new RemoteException("AI 请求体构造失败", e, BaseErrorCode.REMOTE_ERROR, com.aimeeting.interview.ai.model.AiErrorType.PARAMS);
        }
    }

    private Request buildRequest(String body) {
        return new Request.Builder()
                .url(baseUrl + "v1/chat/completions")
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(body, MediaType.parse("application/json")))
                .build();
    }

    private String extractContent(JsonNode root) {
        return root.path("choices").path(0).path("message").path("content").asText("");
    }

    private String modelOf(AiRequest request) {
        return request.getModel() != null ? request.getModel() : defaultModel;
    }
}

package com.aimeeting.interview.ai.provider;

import com.aimeeting.interview.ai.model.AiBizType;
import com.aimeeting.interview.ai.model.AiRequest;
import com.aimeeting.interview.ai.model.AiStreamListener;
import com.aimeeting.interview.ai.model.AiTextResult;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

/**
 * Mock 供应商：规则引擎，按字符切片模拟流式输出（每 ~20 字符 sleep 30ms），永不抛异常。
 *
 * <p>用于：AI key 未配置 / 联网受限 / 测试 场景，保证平台在任何环境下都能跑通完整流程。
 */
@Slf4j
public class MockAiProvider implements AiProvider {

    @Override
    public String name() {
        return "mock";
    }

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public AiTextResult chat(AiRequest request) {
        String content = mockContent(request);
        // 模拟思考延迟
        sleep(400 + ThreadLocalRandom.current().nextInt(400));
        return new AiTextResult(content, 0, content.length() / 2, 600, "mock");
    }

    @Override
    public void streamChat(AiRequest request, AiStreamListener listener) {
        String content = mockContent(request);
        long start = System.currentTimeMillis();
        int step = 20;
        try {
            for (int i = 0; i < content.length(); i += step) {
                int end = Math.min(content.length(), i + step);
                listener.onDelta(content.substring(i, end));
                sleep(30);
            }
            listener.onComplete(new AiTextResult(content, 0, content.length() / 2,
                    System.currentTimeMillis() - start, "mock"));
        } catch (Exception e) {
            listener.onError(e);
        }
    }

    /** 根据业务类型生成可解析的占位内容。 */
    private String mockContent(AiRequest request) {
        AiBizType biz = request.getBizType();
        if (biz == null) {
            return "（Mock）收到请求。";
        }
        switch (biz) {
            case QUESTION:
                return "{\"title\":\"请简述 " + truncate(request.getUserPrompt(), 40)
                        + " 的核心要点\",\"referencePoints\":[\"概念定义\",\"适用场景\",\"与其它方案的对比\"],"
                        + "\"analysis\":\"（Mock 生成，未接入真实大模型）\"}";
            case EVALUATE:
                return "{\"comment\":\"这是一道考察基础理解的题目。整体回答抓住了核心概念，但缺少与替代方案的对比分析，建议补充一层权衡。\","
                        + "\"score\":78,\"highlights\":[\"答出了核心概念\"],\"gaps\":[\"缺少对比分析\"],"
                        + "\"needFollowUp\":true,\"followUpQuestion\":\"能否进一步说明它和替代方案的差异？\",\"improvedAnswer\":\"\"}";
            case FOLLOW_UP:
                return "{\"comment\":\"（Mock）追问补充回答评估完成，已补充对比维度。\",\"score\":82,"
                        + "\"highlights\":[\"补充了对比维度\"],\"gaps\":[\"可再深入\"],"
                        + "\"needFollowUp\":false,\"followUpQuestion\":\"\",\"improvedAnswer\":\"\"}";
            case RESUME:
                return "{\"skills\":[\"Java\",\"Spring Boot\",\"MySQL\"],\"summary\":\"（Mock 解析）具备后端开发基础。\"}";
            case REPORT:
                return "{\"totalScore\":80,\"dimensions\":{\"PROFESSIONAL\":82,\"EXPRESSION\":78,"
                        + "\"LOGIC\":80,\"PROJECT_DEPTH\":79,\"POTENTIAL\":81},"
                        + "\"highlights\":[\"基础扎实\"],\"improvements\":[\"加强项目深度表达\"],"
                        + "\"actions\":[\"复习分布式相关八股\"],\"overallComment\":\"（Mock 报告）整体表现良好。\"}";
            default:
                return "（Mock）" + truncate(request.getUserPrompt(), 60);
        }
    }

    private String truncate(String s, int n) {
        if (s == null) {
            return "";
        }
        return s.length() <= n ? s : s.substring(0, n);
    }

    private void sleep(long ms) {
        try {
            TimeUnit.MILLISECONDS.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

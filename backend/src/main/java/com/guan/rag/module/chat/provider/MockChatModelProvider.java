package com.guan.rag.module.chat.provider;

import com.guan.rag.config.RagProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "rag.chat", name = "provider", havingValue = "mock", matchIfMissing = true)
@RequiredArgsConstructor
public class MockChatModelProvider implements ChatModelProvider {

    private final RagProperties ragProperties;

    @Override
    public ChatResult chat(String prompt) {
        long start = System.currentTimeMillis();
        String answer;
        if (prompt.contains("知识库中没有找到相关依据")) {
            answer = "知识库中没有找到相关依据。";
        } else if (prompt.contains("知识库上下文：") && prompt.contains("用户问题：")) {
            int contextStart = prompt.indexOf("知识库上下文：") + "知识库上下文：".length();
            int questionStart = prompt.indexOf("用户问题：");
            String context = prompt.substring(contextStart, questionStart).trim();
            String question = prompt.substring(questionStart + "用户问题：".length()).trim();
            if (context.isBlank() || context.equals("（无相关上下文）")) {
                answer = "知识库中没有找到相关依据。";
            } else {
                String snippet = context.length() > 200 ? context.substring(0, 200) + "..." : context;
                answer = "根据知识库内容，针对问题「" + question + "」的参考回答如下：\n\n" + snippet;
            }
        } else {
            answer = "（Mock）已收到您的问题，当前为演示模式回答。";
        }
        long latency = System.currentTimeMillis() - start;
        int promptTokens = Math.max(prompt.length() / 4, 1);
        int completionTokens = Math.max(answer.length() / 4, 1);
        return new ChatResult(answer, promptTokens, completionTokens, latency);
    }

    @Override
    public String model() {
        return ragProperties.getChat().getModel();
    }
}

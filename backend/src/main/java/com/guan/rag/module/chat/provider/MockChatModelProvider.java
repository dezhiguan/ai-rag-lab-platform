package com.guan.rag.module.chat.provider;

import com.guan.rag.config.RagProperties;
import com.guan.rag.module.chat.support.ChatRelevanceFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MockChatModelProvider implements ChatModelProvider {

    private static final String NO_ANSWER = "知识库中没有找到相关依据。";

    private final RagProperties ragProperties;

    @Override
    public ChatResult chat(String prompt) {
        long start = System.currentTimeMillis();
        String answer = resolveAnswer(prompt);
        long latency = System.currentTimeMillis() - start;
        int promptTokens = Math.max(prompt.length() / 4, 1);
        int completionTokens = Math.max(answer.length() / 4, 1);
        return new ChatResult(answer, promptTokens, completionTokens, latency);
    }

    @Override
    public String model() {
        return ragProperties.getChat().getModel();
    }

    private String resolveAnswer(String prompt) {
        if (!prompt.contains("知识库上下文：") || !prompt.contains("用户问题：")) {
            return "（Mock）已收到您的问题，当前为演示模式回答。";
        }

        int contextStart = prompt.indexOf("知识库上下文：") + "知识库上下文：".length();
        int questionStart = prompt.indexOf("用户问题：");
        String context = prompt.substring(contextStart, questionStart).trim();
        String question = prompt.substring(questionStart + "用户问题：".length()).trim();

        if (context.isBlank()
                || "（无相关上下文）".equals(context)
                || !ChatRelevanceFilter.hasNgramOverlap(question, context)) {
            return NO_ANSWER;
        }

        String excerpt = extractPrimaryExcerpt(context);
        if (excerpt.isBlank()) {
            return NO_ANSWER;
        }
        return "根据知识库内容：\n\n" + excerpt;
    }

    private String extractPrimaryExcerpt(String context) {
        int marker = context.indexOf("【片段1】");
        String body = marker >= 0 ? context.substring(marker) : context;
        int headerEnd = body.indexOf('\n');
        if (headerEnd >= 0) {
            body = body.substring(headerEnd + 1);
        }
        int nextFragment = body.indexOf("【片段");
        if (nextFragment > 0) {
            body = body.substring(0, nextFragment);
        }
        body = body.trim();
        if (body.length() > 600) {
            return body.substring(0, 600) + "...";
        }
        return body;
    }
}

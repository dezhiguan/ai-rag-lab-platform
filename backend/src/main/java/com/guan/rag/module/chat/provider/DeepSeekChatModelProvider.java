package com.guan.rag.module.chat.provider;

import com.guan.rag.common.http.OpenAiCompatibleClient;
import com.guan.rag.config.RagProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * DeepSeek OpenAI-compatible Chat Completions。
 */
@Component
@RequiredArgsConstructor
public class DeepSeekChatModelProvider implements ChatModelProvider {

    private final RagProperties ragProperties;
    private final OpenAiCompatibleClient httpClient;

    @Override
    public ChatResult chat(String prompt) {
        long start = System.currentTimeMillis();
        RagProperties.Chat cfg = ragProperties.getChat();
        OpenAiCompatibleClient.ChatCompletionResult result = httpClient.createChatCompletion(
                cfg.getBaseUrl(),
                cfg.getApiKey(),
                cfg.getModel(),
                prompt
        );
        long latency = System.currentTimeMillis() - start;
        return new ChatResult(
                result.content(),
                result.promptTokens() > 0 ? result.promptTokens() : Math.max(prompt.length() / 4, 1),
                result.completionTokens() > 0 ? result.completionTokens() : Math.max(result.content().length() / 4, 1),
                latency
        );
    }

    @Override
    public String model() {
        return ragProperties.getChat().getModel();
    }
}

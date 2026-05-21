package com.guan.rag.module.chat.provider;

import com.guan.rag.common.exception.BusinessException;
import com.guan.rag.config.RagProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "rag.chat", name = "provider", havingValue = "openai")
@RequiredArgsConstructor
public class OpenAiChatModelProvider implements ChatModelProvider {

    private final RagProperties ragProperties;

    @Override
    public ChatResult chat(String prompt) {
        if (ragProperties.getChat().getApiKey() == null || ragProperties.getChat().getApiKey().isBlank()) {
            throw new BusinessException("未配置 Chat API Key，请设置 rag.chat.api-key 或切换为 mock");
        }
        throw new BusinessException("OpenAI Chat 尚未接入，请使用 rag.chat.provider=mock");
    }

    @Override
    public String model() {
        return ragProperties.getChat().getModel();
    }
}

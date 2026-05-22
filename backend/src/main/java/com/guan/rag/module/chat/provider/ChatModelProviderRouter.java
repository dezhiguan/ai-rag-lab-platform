package com.guan.rag.module.chat.provider;

import com.guan.rag.common.exception.BusinessException;
import com.guan.rag.config.RagProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * 按 rag.chat.provider 路由到 mock / deepseek。
 */
@Component
@Primary
@RequiredArgsConstructor
public class ChatModelProviderRouter implements ChatModelProvider {

    private final RagProperties ragProperties;
    private final MockChatModelProvider mockChatModelProvider;
    private final DeepSeekChatModelProvider deepSeekChatModelProvider;

    @Override
    public ChatResult chat(String prompt) {
        return delegate().chat(prompt);
    }

    @Override
    public String model() {
        return delegate().model();
    }

    public String configuredProvider() {
        return ragProperties.getChat().getProvider();
    }

    public String delegateType() {
        return delegate().getClass().getSimpleName();
    }

    private ChatModelProvider delegate() {
        String provider = ragProperties.getChat().getProvider();
        if (provider == null || provider.isBlank()) {
            return mockChatModelProvider;
        }
        return switch (provider.trim().toLowerCase()) {
            case "mock" -> mockChatModelProvider;
            case "deepseek" -> deepSeekChatModelProvider;
            default -> throw new BusinessException(
                    "不支持的 chat provider: " + provider + "，可选: mock, deepseek");
        };
    }
}

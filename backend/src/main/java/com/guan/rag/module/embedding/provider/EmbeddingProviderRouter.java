package com.guan.rag.module.embedding.provider;

import com.guan.rag.common.exception.BusinessException;
import com.guan.rag.config.RagProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * 按 rag.embedding.provider 路由到 mock / qwen。
 */
@Component
@Primary
@RequiredArgsConstructor
public class EmbeddingProviderRouter implements EmbeddingProvider {

    private final RagProperties ragProperties;
    private final MockEmbeddingProvider mockEmbeddingProvider;
    private final QwenEmbeddingProvider qwenEmbeddingProvider;

    @Override
    public float[] embed(String text) {
        return delegate().embed(text);
    }

    @Override
    public String model() {
        return delegate().model();
    }

    @Override
    public int dimension() {
        return delegate().dimension();
    }

    public String configuredProvider() {
        return ragProperties.getEmbedding().getProvider();
    }

    public String delegateType() {
        return delegate().getClass().getSimpleName();
    }

    private EmbeddingProvider delegate() {
        String provider = ragProperties.getEmbedding().getProvider();
        if (provider == null || provider.isBlank()) {
            return mockEmbeddingProvider;
        }
        return switch (provider.trim().toLowerCase()) {
            case "mock" -> mockEmbeddingProvider;
            case "qwen" -> qwenEmbeddingProvider;
            default -> throw new BusinessException(
                    "不支持的 embedding provider: " + provider + "，可选: mock, qwen");
        };
    }
}

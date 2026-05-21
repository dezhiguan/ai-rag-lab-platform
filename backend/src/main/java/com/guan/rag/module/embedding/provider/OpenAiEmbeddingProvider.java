package com.guan.rag.module.embedding.provider;

import com.guan.rag.common.exception.BusinessException;
import com.guan.rag.config.RagProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "rag.embedding", name = "provider", havingValue = "openai")
@RequiredArgsConstructor
public class OpenAiEmbeddingProvider implements EmbeddingProvider {

    private final RagProperties ragProperties;

    @Override
    public float[] embed(String text) {
        if (ragProperties.getEmbedding().getApiKey() == null
                || ragProperties.getEmbedding().getApiKey().isBlank()) {
            throw new BusinessException("未配置 Embedding API Key，请设置 rag.embedding.api-key 或切换为 mock");
        }
        throw new BusinessException("OpenAI Embedding 尚未接入，请使用 rag.embedding.provider=mock");
    }

    @Override
    public String model() {
        return ragProperties.getEmbedding().getModel();
    }

    @Override
    public int dimension() {
        return ragProperties.getEmbedding().getDimension();
    }
}

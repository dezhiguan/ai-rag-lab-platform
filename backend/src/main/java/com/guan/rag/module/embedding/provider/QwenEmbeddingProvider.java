package com.guan.rag.module.embedding.provider;

import com.guan.rag.common.http.OpenAiCompatibleClient;
import com.guan.rag.config.RagProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 通义千问 / DashScope OpenAI-compatible Embedding。
 */
@Component
@RequiredArgsConstructor
public class QwenEmbeddingProvider implements EmbeddingProvider {

    private final RagProperties ragProperties;
    private final OpenAiCompatibleClient httpClient;

    @Override
    public float[] embed(String text) {
        RagProperties.Embedding cfg = ragProperties.getEmbedding();
        return httpClient.createEmbedding(
                cfg.getBaseUrl(),
                cfg.getApiKey(),
                cfg.getModel(),
                cfg.getDimension(),
                text == null ? "" : text
        );
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

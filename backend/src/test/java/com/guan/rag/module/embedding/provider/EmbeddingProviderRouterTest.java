package com.guan.rag.module.embedding.provider;

import com.guan.rag.common.exception.BusinessException;
import com.guan.rag.config.RagProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmbeddingProviderRouterTest {

    @Test
    void routesToMockByDefault() {
        RagProperties props = new RagProperties();
        props.getEmbedding().setProvider("mock");
        EmbeddingProviderRouter router = new EmbeddingProviderRouter(
                props,
                new MockEmbeddingProvider(props),
                new QwenEmbeddingProvider(props, null)
        );
        assertEquals("MockEmbeddingProvider", router.delegateType());
        assertEquals(384, router.embed("test").length);
    }

    @Test
    void rejectsUnsupportedProvider() {
        RagProperties props = new RagProperties();
        props.getEmbedding().setProvider("openai");
        EmbeddingProviderRouter router = new EmbeddingProviderRouter(
                props,
                new MockEmbeddingProvider(props),
                new QwenEmbeddingProvider(props, null)
        );
        BusinessException ex = assertThrows(BusinessException.class, router::dimension);
        assertTrue(ex.getMessage().contains("mock"));
        assertTrue(ex.getMessage().contains("qwen"));
    }
}

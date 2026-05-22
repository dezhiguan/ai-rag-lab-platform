package com.guan.rag.config;

import com.guan.rag.module.chat.provider.ChatModelProvider;
import com.guan.rag.module.embedding.provider.EmbeddingProvider;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class RagProviderConfig {

    private final RagProperties ragProperties;
    private final EmbeddingProvider embeddingProvider;
    private final ChatModelProvider chatModelProvider;

    @PostConstruct
    public void logActiveProviders() {
        log.info(
                "V2.5 RAG providers: embedding={} / {} ({} dim), chat={} / {}",
                ragProperties.getEmbedding().getProvider(),
                embeddingProvider.model(),
                embeddingProvider.dimension(),
                ragProperties.getChat().getProvider(),
                chatModelProvider.model()
        );
        log.info("Embedding implementation: {}", embeddingProvider.getClass().getSimpleName());
        log.info("Chat implementation: {}", chatModelProvider.getClass().getSimpleName());
    }
}

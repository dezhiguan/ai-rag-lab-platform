package com.guan.rag.module.embedding.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EmbeddingStatusResponse {

    private long totalChunks;
    private long embeddedChunks;
    private long notEmbeddedChunks;
}

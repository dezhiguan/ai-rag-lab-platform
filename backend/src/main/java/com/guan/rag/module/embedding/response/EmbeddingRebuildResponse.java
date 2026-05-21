package com.guan.rag.module.embedding.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EmbeddingRebuildResponse {

    private long totalChunks;
    private long embeddedChunks;
    private String embeddingModel;
    private int embeddingDimension;
}

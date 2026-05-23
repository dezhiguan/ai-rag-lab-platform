package com.guan.rag.module.chat.support;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ContextFilterOptions {

    private Integer maxChunks;

    private Double minScore;

    private Double maxScoreGap;
}

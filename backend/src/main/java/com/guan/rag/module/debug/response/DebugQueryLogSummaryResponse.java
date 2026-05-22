package com.guan.rag.module.debug.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DebugQueryLogSummaryResponse {

    private Long queryLogId;
    private Long kbId;
    private String question;
    private String embeddingProvider;
    private String embeddingModel;
    private String chatProvider;
    private String chatModel;
    private Integer topK;
    private Long retrievalTimeMs;
    private Long generationTimeMs;
    private Long totalTimeMs;
    private LocalDateTime createdAt;
}

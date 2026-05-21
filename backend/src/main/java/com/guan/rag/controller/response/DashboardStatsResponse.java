package com.guan.rag.controller.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardStatsResponse {

    private long knowledgeBaseCount;
    private long documentCount;
    private long chunkCount;
    private long embeddedChunkCount;
    private long chatSessionCount;
    private long chatMessageCount;
    private boolean sampleDataInitialized;
}

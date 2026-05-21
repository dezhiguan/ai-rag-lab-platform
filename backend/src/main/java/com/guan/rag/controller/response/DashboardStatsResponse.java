package com.guan.rag.controller.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardStatsResponse {

    private long knowledgeBaseCount;
    private long documentCount;
    private long chunkCount;
    private boolean sampleDataInitialized;
}

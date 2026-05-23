package com.guan.rag.module.metrics.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RagMetricsResponse {

    private long totalQueryCount;
    private long todayQueryCount;
    private long avgTotalTimeMs;
    private long avgRetrievalTimeMs;
    private long avgGenerationTimeMs;
    private SearchModeStatsResponse searchModeStats;
    private RerankStatsResponse rerankStats;
    private List<SlowQueryResponse> slowQueries;

    @Data
    @Builder
    public static class SearchModeStatsResponse {
        private long vectorCount;
        private long bm25Count;
        private long hybridCount;
    }

    @Data
    @Builder
    public static class RerankStatsResponse {
        private long enabledCount;
        private long disabledCount;
    }

    @Data
    @Builder
    public static class SlowQueryResponse {
        private Long queryLogId;
        private String question;
        private String searchMode;
        private boolean enableRerank;
        private Long totalTimeMs;
        private String createdAt;
    }
}

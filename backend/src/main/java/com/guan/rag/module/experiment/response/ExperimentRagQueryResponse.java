package com.guan.rag.module.experiment.response;

import com.guan.rag.module.debug.response.DebugLatencyResponse;
import com.guan.rag.module.debug.response.DebugRetrievedChunkResponse;
import com.guan.rag.module.token.response.TokenUsageResponse;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ExperimentRagQueryResponse {

    private Long queryLogId;
    private Long kbId;
    private String question;
    private String answer;
    private String prompt;
    private String context;
    private List<DebugRetrievedChunkResponse> retrievedChunks;
    private List<DebugRetrievedChunkResponse> contextChunks;
    private DebugLatencyResponse latency;
    private ExperimentParamsResponse usedParams;
    private ExperimentImpactResponse impact;
    private TokenUsageResponse tokenUsage;

    @Data
    @Builder
    public static class ExperimentParamsResponse {
        private Integer topK;
        private String searchMode;
        private Boolean enableRerank;
        private Integer maxChunks;
        private Double minScore;
        private Double maxScoreGap;
    }

    @Data
    @Builder
    public static class ExperimentImpactResponse {
        private int retrievedCount;
        private int contextCount;
        private int filteredCount;
    }
}

package com.guan.rag.module.querylog.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RagSlowQueryAnalysisResponse {

    private long totalCount;
    private List<RagSlowQueryItemResponse> records;

    @Data
    @Builder
    public static class RagSlowQueryItemResponse {
        private Long queryLogId;
        private String question;
        private String searchMode;
        private boolean enableRerank;
        private Long retrievalTimeMs;
        private Long generationTimeMs;
        private Long totalTimeMs;
        private Integer retrievalChunkCount;
        private List<String> slowReasons;
        private List<String> suggestions;
        private String createdAt;
    }
}

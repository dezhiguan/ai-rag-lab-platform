package com.guan.rag.module.querylog.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RagQueryLogItemResponse {

    private Long queryLogId;
    private String question;
    private String searchMode;
    private boolean enableRerank;
    private Long totalTimeMs;
    private Long retrievalTimeMs;
    private Long generationTimeMs;
    private String createdAt;
}

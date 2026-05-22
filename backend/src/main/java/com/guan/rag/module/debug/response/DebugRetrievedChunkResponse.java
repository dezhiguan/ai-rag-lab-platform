package com.guan.rag.module.debug.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DebugRetrievedChunkResponse {

    private Long documentId;
    private String documentName;
    private Long chunkId;
    private Integer chunkIndex;
    private Double score;
    private Integer rankPosition;
    private String content;
    private Boolean usedInPrompt;
    /** 未进入 Prompt 时的原因：SCORE_TOO_LOW / SCORE_GAP_TOO_LARGE / EXCEED_MAX_CONTEXT_CHUNKS */
    private String filterReason;
}

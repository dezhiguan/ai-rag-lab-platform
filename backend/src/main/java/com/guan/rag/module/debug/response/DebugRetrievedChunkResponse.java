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
}

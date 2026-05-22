package com.guan.rag.module.debug.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DebugQueryResponse {

    private Long queryLogId;
    private Long kbId;
    private String question;
    private String searchMode;
    private String embeddingProvider;
    private String embeddingModel;
    private String chatProvider;
    private String chatModel;
    private List<DebugRetrievedChunkResponse> retrievedChunks;
    private List<DebugRetrievedChunkResponse> contextChunks;
    private String context;
    private String prompt;
    private String answer;
    private DebugLatencyResponse latency;
}

package com.guan.rag.module.chat.support;

import com.guan.rag.module.retrieval.response.RetrievedChunkResponse;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class ContextFilterResult {

    private List<RetrievedChunkResponse> contextChunks;
    /** chunkId -> 是否进入 Prompt 及过滤原因 */
    private Map<Long, ChunkFilterDecision> decisions;

    @Data
    @Builder
    public static class ChunkFilterDecision {
        private boolean usedInPrompt;
        /** 未进入 Prompt 时的原因；进入时为 null */
        private String filterReason;
    }
}

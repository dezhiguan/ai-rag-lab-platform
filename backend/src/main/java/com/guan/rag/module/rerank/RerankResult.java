package com.guan.rag.module.rerank;

import com.guan.rag.module.retrieval.response.RetrievedChunkResponse;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RerankResult {

    private List<RerankedItem> items;

    @Data
    @Builder
    public static class RerankedItem {

        private RetrievedChunkResponse chunk;
        private Integer originalRank;
        private Integer rerankRank;
        private Double rerankScore;
    }
}

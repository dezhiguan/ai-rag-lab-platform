package com.guan.rag.module.rerank;

import com.guan.rag.module.retrieval.response.RetrievedChunkResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RerankServiceTest {

    private final RerankService rerankService = new RerankService();

    @Test
    void rerank_proprietaryTermPromotesMatchingChunk() {
        List<RetrievedChunkResponse> chunks = List.of(
                chunk(1L, "guideline.md", 0.3, "项目背景与 PgVector 规划"),
                chunk(2L, "api-spec.md", 0.5, "错误码 SMS_429 表示发送过于频繁")
        );

        RerankResult result = rerankService.rerank("SMS_429 是什么意思？", chunks);

        assertEquals(2, result.getItems().size());
        assertEquals(2L, result.getItems().get(0).getChunk().getChunkId());
        assertEquals(2, result.getItems().get(0).getOriginalRank());
        assertEquals(1, result.getItems().get(0).getRerankRank());
        assertTrue(result.getItems().get(0).getRerankScore() > result.getItems().get(1).getRerankScore());
    }

    @Test
    void rerank_preservesOriginalRank() {
        List<RetrievedChunkResponse> chunks = List.of(
                chunk(10L, "a.md", 0.9, "alpha"),
                chunk(20L, "b.md", 0.5, "beta")
        );

        RerankResult result = rerankService.rerank("alpha", chunks);

        RerankResult.RerankedItem first = result.getItems().stream()
                .filter(i -> i.getChunk().getChunkId().equals(10L))
                .findFirst()
                .orElseThrow();
        assertEquals(1, first.getOriginalRank());
    }

    @Test
    void toNormalizedForFilter_scalesToOne() {
        List<RetrievedChunkResponse> chunks = List.of(
                chunk(1L, "a.md", 0.5, "SMS_429"),
                chunk(2L, "b.md", 0.3, "other")
        );
        RerankResult result = rerankService.rerank("SMS_429", chunks);
        List<RetrievedChunkResponse> normalized = rerankService.toNormalizedForFilter(result);

        assertEquals(1.0, normalized.get(0).getScore(), 1e-9);
    }

    private static RetrievedChunkResponse chunk(Long id, String name, double score, String content) {
        return RetrievedChunkResponse.builder()
                .chunkId(id)
                .documentId(id)
                .documentName(name)
                .chunkIndex(0)
                .score(score)
                .content(content)
                .build();
    }
}

package com.guan.rag.module.search.hybrid;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * V5-02：仅验证 {@link HybridSearchMerger} 融合排序逻辑，不涉及页面、接口、HYBRID searchMode。
 */
class HybridSearchMergerTest {

    private final HybridSearchMerger merger = new HybridSearchMerger();

    @Test
    void mergesVectorAndBm25ByChunkId() {
        List<HybridSearchMergeItem> vector = List.of(
                item(1L, "guideline.md", 0.3),
                item(2L, "api-spec.md", 0.55)
        );
        List<HybridSearchMergeItem> bm25 = List.of(
                item(2L, "api-spec.md", 12.0),
                item(3L, "troubleshooting.md", 8.0)
        );

        List<HybridSearchResult> results = merger.merge(vector, bm25, 10);

        assertEquals(3, results.size());
        HybridSearchResult merged = results.stream()
                .filter(r -> r.getChunkId().equals(2L))
                .findFirst()
                .orElseThrow();
        assertEquals(2, merged.getVectorRank());
        assertEquals(1, merged.getBm25Rank());
        assertEquals("api-spec.md", merged.getDocumentName());
        assertEquals(0.55, merged.getVectorScore());
        assertEquals(1.0, merged.getBm25Score(), 1e-9);
    }

    @Test
    void normalizesBm25ScoreByTopScore() {
        List<HybridSearchMergeItem> bm25 = List.of(
                item(10L, "top.md", 20.0),
                item(20L, "second.md", 10.0),
                item(30L, "third.md", 5.0)
        );

        List<HybridSearchResult> results = merger.merge(List.of(), bm25, 5);

        assertEquals(1.0, find(results, 10L).getBm25Score(), 1e-9);
        assertEquals(0.5, find(results, 20L).getBm25Score(), 1e-9);
        assertEquals(0.25, find(results, 30L).getBm25Score(), 1e-9);
    }

    @Test
    void sortsByHybridScoreDescending() {
        List<HybridSearchMergeItem> vector = List.of(
                item(1L, "a.md", 0.9),
                item(2L, "b.md", 0.5)
        );
        List<HybridSearchMergeItem> bm25 = List.of(
                item(2L, "b.md", 10.0),
                item(3L, "c.md", 8.0)
        );

        List<HybridSearchResult> results = merger.merge(vector, bm25, 5);

        assertEquals(2L, results.get(0).getChunkId());
        for (int i = 0; i < results.size() - 1; i++) {
            assertTrue(results.get(i).getHybridScore() >= results.get(i + 1).getHybridScore());
        }
        assertTrue(results.get(0).getHybridScore() > find(results, 1L).getHybridScore());
        assertTrue(results.get(0).getHybridScore() > find(results, 3L).getHybridScore());
    }

    @Test
    void truncatesToTopK() {
        List<HybridSearchMergeItem> vector = List.of(
                item(1L, "a.md", 0.9),
                item(2L, "b.md", 0.8),
                item(3L, "c.md", 0.7)
        );

        List<HybridSearchResult> results = merger.merge(vector, List.of(), 2);

        assertEquals(2, results.size());
        assertEquals(1L, results.get(0).getChunkId());
        assertEquals(2L, results.get(1).getChunkId());
    }

    @Test
    void returnsEmptyForNullEmptyOrInvalidTopKWithoutException() {
        assertDoesNotThrow(() -> {
            assertTrue(merger.merge(null, null, 5).isEmpty());
            assertTrue(merger.merge(List.of(), List.of(), 5).isEmpty());
            assertTrue(merger.merge(List.of(item(1L, "a.md", 0.5)), null, 0).isEmpty());
            assertTrue(merger.merge(null, List.of(item(1L, "a.md", 1.0)), -1).isEmpty());
        });
    }

    @Test
    void normalizeBm25Scores_emptyOrZeroMax_handlesSafely() {
        assertTrue(HybridSearchMerger.normalizeBm25Scores(List.of()).isEmpty());
        List<HybridSearchMergeItem> zero = List.of(item(1L, "z.md", 0.0));
        List<HybridSearchMergeItem> normalized = HybridSearchMerger.normalizeBm25Scores(zero);
        assertEquals(1, normalized.size());
        assertEquals(0.0, normalized.get(0).getScore(), 1e-9);
    }

    @Test
    void duplicateChunkIdInSameList_usesFirstRankOnly() {
        List<HybridSearchMergeItem> vector = List.of(
                item(1L, "a.md", 0.9),
                item(1L, "a-dup.md", 0.1)
        );

        List<HybridSearchResult> results = merger.merge(vector, List.of(), 5);

        assertEquals(1, results.size());
        assertEquals(1, results.get(0).getVectorRank());
        assertEquals("a.md", results.get(0).getDocumentName());
        assertNull(results.get(0).getBm25Rank());
    }

    private static HybridSearchResult find(List<HybridSearchResult> results, long chunkId) {
        return results.stream()
                .filter(r -> r.getChunkId().equals(chunkId))
                .findFirst()
                .orElseThrow();
    }

    private static HybridSearchMergeItem item(Long chunkId, String docName, double score) {
        return HybridSearchMergeItem.builder()
                .chunkId(chunkId)
                .documentId(chunkId)
                .documentName(docName)
                .chunkIndex(0)
                .score(score)
                .content("content-" + docName)
                .build();
    }
}

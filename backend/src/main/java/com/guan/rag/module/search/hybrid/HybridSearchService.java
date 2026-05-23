package com.guan.rag.module.search.hybrid;

import com.guan.rag.module.retrieval.response.RetrievedChunkResponse;
import com.guan.rag.module.retrieval.service.VectorRetrievalService;
import com.guan.rag.module.search.service.Bm25SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Vector + BM25 双路召回，经 {@link HybridSearchMerger} RRF 融合。
 */
@Service
@RequiredArgsConstructor
public class HybridSearchService {

    private final VectorRetrievalService vectorRetrievalService;
    private final Bm25SearchService bm25SearchService;
    private final HybridSearchMerger hybridSearchMerger;

    public List<HybridSearchResult> search(Long kbId, String question, int topK) {
        List<RetrievedChunkResponse> vector = vectorRetrievalService.retrieve(kbId, question, topK);
        List<RetrievedChunkResponse> bm25 = bm25SearchService.retrieve(kbId, question, topK);
        return hybridSearchMerger.merge(toMergeItems(vector), toMergeItems(bm25), topK);
    }

    public List<RetrievedChunkResponse> retrieve(Long kbId, String question, int topK) {
        return toRetrievedChunks(search(kbId, question, topK), false);
    }

    /**
     * RRF 分数量纲与向量 [0,1] 不同，按 Top1 归一化后再做 Context 过滤。
     */
    public List<RetrievedChunkResponse> retrieveNormalizedForFilter(Long kbId, String question, int topK) {
        return toRetrievedChunks(search(kbId, question, topK), true);
    }

    private List<HybridSearchMergeItem> toMergeItems(List<RetrievedChunkResponse> chunks) {
        List<HybridSearchMergeItem> items = new ArrayList<>();
        for (RetrievedChunkResponse chunk : chunks) {
            if (chunk == null || chunk.getChunkId() == null) {
                continue;
            }
            items.add(HybridSearchMergeItem.builder()
                    .chunkId(chunk.getChunkId())
                    .documentId(chunk.getDocumentId())
                    .documentName(chunk.getDocumentName())
                    .chunkIndex(chunk.getChunkIndex())
                    .content(chunk.getContent())
                    .score(chunk.getScore())
                    .build());
        }
        return items;
    }

    private List<RetrievedChunkResponse> toRetrievedChunks(List<HybridSearchResult> merged, boolean normalizeScore) {
        if (merged.isEmpty()) {
            return List.of();
        }
        double maxScore = merged.stream()
                .mapToDouble(r -> r.getHybridScore() != null ? r.getHybridScore() : 0.0)
                .max()
                .orElse(1.0);
        if (maxScore <= 0) {
            maxScore = 1.0;
        }
        double divisor = maxScore;
        List<RetrievedChunkResponse> results = new ArrayList<>(merged.size());
        for (HybridSearchResult item : merged) {
            double raw = item.getHybridScore() != null ? item.getHybridScore() : 0.0;
            double score = normalizeScore ? raw / divisor : raw;
            results.add(RetrievedChunkResponse.builder()
                    .documentId(item.getDocumentId())
                    .documentName(item.getDocumentName())
                    .chunkId(item.getChunkId())
                    .chunkIndex(item.getChunkIndex())
                    .score(score)
                    .content(item.getContent())
                    .build());
        }
        return results;
    }
}

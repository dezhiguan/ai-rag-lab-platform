package com.guan.rag.module.search.hybrid;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Vector + BM25 应用层融合排序（RRF）。
 * <p>
 * 公式：{@code hybridScore = Σ 1 / (k + rank)}，rank 为 1-based 列表位置。
 * 仅出现在一路的 Chunk 仍可获得该路贡献分。
 */
@Component
public class HybridSearchMerger {

    public static final int DEFAULT_RRF_K = 60;

    private final int rrfK;

    public HybridSearchMerger() {
        this(DEFAULT_RRF_K);
    }

    public HybridSearchMerger(int rrfK) {
        if (rrfK < 0) {
            throw new IllegalArgumentException("rrfK must be non-negative");
        }
        this.rrfK = rrfK;
    }

    /**
     * @param vectorResults Vector 召回（按 score 降序为佳）
     * @param bm25Results   BM25 召回（按 score 降序为佳）
     * @param topK          返回条数上限；&lt; 1 时返回空列表
     */
    public List<HybridSearchResult> merge(
            List<HybridSearchMergeItem> vectorResults,
            List<HybridSearchMergeItem> bm25Results,
            int topK) {
        if (topK < 1) {
            return List.of();
        }
        List<HybridSearchMergeItem> vector = vectorResults != null ? vectorResults : List.of();
        List<HybridSearchMergeItem> bm25 = normalizeBm25Scores(
                bm25Results != null ? bm25Results : List.of());
        if (vector.isEmpty() && bm25.isEmpty()) {
            return List.of();
        }

        Map<Long, Acc> byChunk = new HashMap<>();

        for (int i = 0; i < vector.size(); i++) {
            HybridSearchMergeItem item = vector.get(i);
            if (item == null || item.getChunkId() == null) {
                continue;
            }
            int rank = i + 1;
            Acc acc = byChunk.computeIfAbsent(item.getChunkId(), id -> new Acc());
            if (acc.vectorRank == null) {
                acc.vectorRank = rank;
                acc.vectorScore = item.getScore();
                acc.mergeMetadata(item);
            }
        }

        for (int i = 0; i < bm25.size(); i++) {
            HybridSearchMergeItem item = bm25.get(i);
            if (item == null || item.getChunkId() == null) {
                continue;
            }
            int rank = i + 1;
            Acc acc = byChunk.computeIfAbsent(item.getChunkId(), id -> new Acc());
            if (acc.bm25Rank == null) {
                acc.bm25Rank = rank;
                acc.bm25Score = item.getScore();
                acc.mergeMetadata(item);
            }
        }

        List<HybridSearchResult> merged = new ArrayList<>(byChunk.size());
        for (Map.Entry<Long, Acc> entry : byChunk.entrySet()) {
            Acc acc = entry.getValue();
            double hybrid = 0.0;
            if (acc.vectorRank != null) {
                hybrid += rrfContribution(acc.vectorRank);
            }
            if (acc.bm25Rank != null) {
                hybrid += rrfContribution(acc.bm25Rank);
            }
            merged.add(HybridSearchResult.builder()
                    .chunkId(entry.getKey())
                    .documentId(acc.documentId)
                    .documentName(acc.documentName)
                    .chunkIndex(acc.chunkIndex)
                    .content(acc.content)
                    .hybridScore(hybrid)
                    .vectorScore(acc.vectorScore)
                    .bm25Score(acc.bm25Score)
                    .vectorRank(acc.vectorRank)
                    .bm25Rank(acc.bm25Rank)
                    .build());
        }

        merged.sort(Comparator.comparing(HybridSearchResult::getHybridScore, Comparator.reverseOrder())
                .thenComparing(HybridSearchResult::getChunkId));

        if (merged.size() <= topK) {
            return merged;
        }
        return new ArrayList<>(merged.subList(0, topK));
    }

    private double rrfContribution(int rank) {
        return 1.0 / (rrfK + rank);
    }

    /**
     * BM25 原始分数量纲与向量不同，按 Top1 归一化到 [0,1] 后写入结果的 {@code bm25Score}。
     * 列表顺序不变，RRF 仍按原始名次计算。
     */
    static List<HybridSearchMergeItem> normalizeBm25Scores(List<HybridSearchMergeItem> bm25) {
        if (bm25.isEmpty()) {
            return List.of();
        }
        double maxScore = bm25.stream()
                .filter(item -> item != null && item.getScore() != null)
                .mapToDouble(HybridSearchMergeItem::getScore)
                .max()
                .orElse(1.0);
        if (maxScore <= 0) {
            maxScore = 1.0;
        }
        double divisor = maxScore;
        List<HybridSearchMergeItem> normalized = new ArrayList<>(bm25.size());
        for (HybridSearchMergeItem item : bm25) {
            if (item == null) {
                continue;
            }
            double raw = item.getScore() != null ? item.getScore() : 0.0;
            normalized.add(HybridSearchMergeItem.builder()
                    .chunkId(item.getChunkId())
                    .documentId(item.getDocumentId())
                    .documentName(item.getDocumentName())
                    .chunkIndex(item.getChunkIndex())
                    .content(item.getContent())
                    .score(raw / divisor)
                    .build());
        }
        return normalized;
    }

    private static final class Acc {
        Long documentId;
        String documentName;
        Integer chunkIndex;
        String content;
        Double vectorScore;
        Double bm25Score;
        Integer vectorRank;
        Integer bm25Rank;

        void mergeMetadata(HybridSearchMergeItem item) {
            if (documentId == null) {
                documentId = item.getDocumentId();
            }
            if (documentName == null) {
                documentName = item.getDocumentName();
            }
            if (chunkIndex == null) {
                chunkIndex = item.getChunkIndex();
            }
            if (content == null) {
                content = item.getContent();
            }
        }
    }
}

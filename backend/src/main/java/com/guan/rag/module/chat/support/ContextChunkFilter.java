package com.guan.rag.module.chat.support;

import com.guan.rag.config.RagProperties;
import com.guan.rag.module.retrieval.response.RetrievedChunkResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * V3 轻量级 Context 组装过滤：召回 TopK 全量展示，仅高分且与 Top1 差距可控的 Chunk 进入 Prompt。
 */
@Component
@RequiredArgsConstructor
public class ContextChunkFilter {

    private final RagProperties ragProperties;

    public ContextFilterResult filter(List<RetrievedChunkResponse> retrieved) {
        if (retrieved == null || retrieved.isEmpty()) {
            return ContextFilterResult.builder()
                    .contextChunks(List.of())
                    .decisions(Map.of())
                    .build();
        }

        RagProperties.Context cfg = ragProperties.getContext();
        int maxChunks = Math.max(cfg.getMaxChunks(), 1);
        double minScore = cfg.getMinScore();
        double maxScoreGap = cfg.getMaxScoreGap();

        List<RetrievedChunkResponse> sorted = retrieved.stream()
                .sorted(Comparator.comparing(
                        RetrievedChunkResponse::getScore,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        double top1Score = scoreOf(sorted.get(0));

        List<RetrievedChunkResponse> eligible = new ArrayList<>();
        for (RetrievedChunkResponse chunk : sorted) {
            double score = scoreOf(chunk);
            if (score < minScore) {
                continue;
            }
            if (top1Score - score > maxScoreGap) {
                continue;
            }
            eligible.add(chunk);
        }

        List<RetrievedChunkResponse> contextChunks;
        if (eligible.isEmpty()) {
            contextChunks = List.of(sorted.get(0));
        } else {
            contextChunks = eligible.stream().limit(maxChunks).collect(Collectors.toList());
        }

        Set<Long> usedChunkIds = contextChunks.stream()
                .map(RetrievedChunkResponse::getChunkId)
                .collect(Collectors.toCollection(HashSet::new));

        Map<Long, ContextFilterResult.ChunkFilterDecision> decisions = new HashMap<>();
        for (RetrievedChunkResponse chunk : retrieved) {
            Long chunkId = chunk.getChunkId();
            if (usedChunkIds.contains(chunkId)) {
                decisions.put(chunkId, ContextFilterResult.ChunkFilterDecision.builder()
                        .usedInPrompt(true)
                        .filterReason(null)
                        .build());
            } else {
                decisions.put(chunkId, ContextFilterResult.ChunkFilterDecision.builder()
                        .usedInPrompt(false)
                        .filterReason(resolveFilterReason(chunk, top1Score, minScore, maxScoreGap, eligible, maxChunks))
                        .build());
            }
        }

        return ContextFilterResult.builder()
                .contextChunks(contextChunks)
                .decisions(decisions)
                .build();
    }

    private String resolveFilterReason(
            RetrievedChunkResponse chunk,
            double top1Score,
            double minScore,
            double maxScoreGap,
            List<RetrievedChunkResponse> eligible,
            int maxChunks
    ) {
        double score = scoreOf(chunk);
        if (score < minScore) {
            return ContextFilterReason.SCORE_TOO_LOW;
        }
        if (top1Score - score > maxScoreGap) {
            return ContextFilterReason.SCORE_GAP_TOO_LARGE;
        }
        int indexInEligible = indexOfChunk(eligible, chunk.getChunkId());
        if (indexInEligible >= 0 && indexInEligible >= maxChunks) {
            return ContextFilterReason.EXCEED_MAX_CONTEXT_CHUNKS;
        }
        return ContextFilterReason.EXCEED_MAX_CONTEXT_CHUNKS;
    }

    private int indexOfChunk(List<RetrievedChunkResponse> eligible, Long chunkId) {
        for (int i = 0; i < eligible.size(); i++) {
            if (eligible.get(i).getChunkId().equals(chunkId)) {
                return i;
            }
        }
        return -1;
    }

    private double scoreOf(RetrievedChunkResponse chunk) {
        return chunk.getScore() != null ? chunk.getScore() : 0.0;
    }
}

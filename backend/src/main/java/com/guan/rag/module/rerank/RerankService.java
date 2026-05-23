package com.guan.rag.module.rerank;

import com.guan.rag.module.retrieval.response.RetrievedChunkResponse;
import com.guan.rag.module.search.util.SearchTermExtractor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 轻量级本地 Reranker：基于问题关键词与专有词命中，不接外部模型。
 */
@Service
public class RerankService {

    private static final double PROPRIETARY_TERM_BOOST = 5.0;
    private static final double KEYWORD_HIT_BOOST = 1.5;
    private static final double RETRIEVAL_SCORE_WEIGHT = 0.2;
    private static final double ORIGINAL_RANK_WEIGHT = 0.08;

    private static final Pattern TOKEN_SPLIT = Pattern.compile("[\\s，。！？、；：\"'（）\\[\\]{}<>]+");
    private static final Set<String> STOP_WORDS = Set.of(
            "什么", "意思", "怎么", "如何", "为什么", "请问", "是否", "可以", "这个", "那个", "一下"
    );

    public RerankResult rerank(String question, List<RetrievedChunkResponse> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return RerankResult.builder().items(List.of()).build();
        }
        List<String> proprietaryTerms = SearchTermExtractor.extractFromQuery(question);
        List<String> keywords = collectKeywords(question, proprietaryTerms);

        List<Scored> scored = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            RetrievedChunkResponse chunk = chunks.get(i);
            if (chunk == null) {
                continue;
            }
            int originalRank = i + 1;
            double raw = scoreChunk(chunk, proprietaryTerms, keywords, originalRank, chunks.size());
            scored.add(new Scored(chunk, originalRank, raw));
        }

        scored.sort(Comparator.comparingDouble(Scored::rawScore).reversed()
                .thenComparingInt(s -> s.originalRank));

        List<RerankResult.RerankedItem> items = new ArrayList<>(scored.size());
        for (int i = 0; i < scored.size(); i++) {
            Scored s = scored.get(i);
            items.add(RerankResult.RerankedItem.builder()
                    .chunk(s.chunk)
                    .originalRank(s.originalRank)
                    .rerankRank(i + 1)
                    .rerankScore(s.rawScore)
                    .build());
        }
        return RerankResult.builder().items(items).build();
    }

    /**
     * 将 rerank 分按 Top1 归一化到 [0,1]，供 Context 过滤使用。
     */
    public List<RetrievedChunkResponse> toNormalizedForFilter(RerankResult result) {
        if (result == null || result.getItems() == null || result.getItems().isEmpty()) {
            return List.of();
        }
        double max = result.getItems().stream()
                .mapToDouble(i -> i.getRerankScore() != null ? i.getRerankScore() : 0.0)
                .max()
                .orElse(1.0);
        if (max <= 0) {
            max = 1.0;
        }
        double divisor = max;
        List<RetrievedChunkResponse> normalized = new ArrayList<>();
        for (RerankResult.RerankedItem item : result.getItems()) {
            RetrievedChunkResponse c = item.getChunk();
            double raw = item.getRerankScore() != null ? item.getRerankScore() : 0.0;
            normalized.add(RetrievedChunkResponse.builder()
                    .documentId(c.getDocumentId())
                    .documentName(c.getDocumentName())
                    .chunkId(c.getChunkId())
                    .chunkIndex(c.getChunkIndex())
                    .score(raw / divisor)
                    .content(c.getContent())
                    .build());
        }
        return normalized;
    }

    private double scoreChunk(
            RetrievedChunkResponse chunk,
            List<String> proprietaryTerms,
            List<String> keywords,
            int originalRank,
            int total
    ) {
        String content = chunk.getContent() != null ? chunk.getContent() : "";
        double score = 0.0;

        for (String term : proprietaryTerms) {
            if (content.contains(term)) {
                score += PROPRIETARY_TERM_BOOST;
            }
        }

        Set<String> counted = new HashSet<>(proprietaryTerms);
        for (String keyword : keywords) {
            if (counted.contains(keyword)) {
                continue;
            }
            if (content.contains(keyword)) {
                score += KEYWORD_HIT_BOOST;
            }
        }

        if (chunk.getScore() != null) {
            score += chunk.getScore() * RETRIEVAL_SCORE_WEIGHT;
        }

        if (total > 0) {
            score += ORIGINAL_RANK_WEIGHT * (total - originalRank + 1) / (double) total;
        }
        return score;
    }

    private List<String> collectKeywords(String question, List<String> proprietaryTerms) {
        Set<String> terms = new HashSet<>(proprietaryTerms);
        if (question == null || question.isBlank()) {
            return List.copyOf(terms);
        }
        for (String part : TOKEN_SPLIT.split(question.trim())) {
            String token = part.trim();
            if (token.length() < 2) {
                continue;
            }
            if (STOP_WORDS.contains(token)) {
                continue;
            }
            terms.add(token);
            if (token.matches("[A-Za-z0-9_/-]+")) {
                terms.add(token.toLowerCase(Locale.ROOT));
            }
        }
        return List.copyOf(terms);
    }

    private record Scored(RetrievedChunkResponse chunk, int originalRank, double rawScore) {
    }
}

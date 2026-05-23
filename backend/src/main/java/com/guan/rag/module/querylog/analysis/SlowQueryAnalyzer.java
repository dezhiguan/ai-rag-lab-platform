package com.guan.rag.module.querylog.analysis;

import com.guan.rag.module.debug.entity.DebugQueryLog;
import com.guan.rag.module.search.SearchMode;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class SlowQueryAnalyzer {

    public static final long TOTAL_SLOW_MS = 3000L;
    public static final long RETRIEVAL_SLOW_MS = 1000L;
    public static final long GENERATION_SLOW_MS = 2000L;

    private SlowQueryAnalyzer() {
    }

    public static AnalysisResult analyze(DebugQueryLog log, int retrievalChunkCount) {
        long retrievalMs = nullSafe(log.getRetrievalTimeMs());
        long generationMs = nullSafe(log.getGenerationTimeMs());
        long totalMs = nullSafe(log.getTotalTimeMs());
        boolean enableRerank = log.getEnableRerank() != null && log.getEnableRerank() == 1;
        SearchMode searchMode = resolveSearchMode(log.getSearchMode());

        List<String> reasons = new ArrayList<>();
        Set<String> suggestions = new LinkedHashSet<>();

        if (retrievalMs >= RETRIEVAL_SLOW_MS) {
            reasons.add("检索耗时高");
            suggestions.add("建议降低 topK");
            suggestions.addAll(retrievalSuggestions(searchMode));
        }
        if (generationMs >= GENERATION_SLOW_MS) {
            reasons.add("生成耗时高");
            suggestions.add("建议检查 Chat Model Provider 配置与网络");
            suggestions.add("建议缩短 Prompt 或减少进入 Context 的 Chunk 数");
        }
        if (totalMs >= TOTAL_SLOW_MS) {
            reasons.add("总耗时高");
        }
        if (enableRerank && (retrievalMs >= RETRIEVAL_SLOW_MS || totalMs >= TOTAL_SLOW_MS)) {
            reasons.add("启用重排导致耗时增加");
            if (retrievalChunkCount > 0) {
                suggestions.add("当前召回约 " + retrievalChunkCount + " 个候选 Chunk，建议减少 topK 以降低重排开销");
            } else {
                suggestions.add("建议减少候选 Chunk 数量（降低 topK）");
            }
        }

        return AnalysisResult.builder()
                .slowReasons(reasons)
                .suggestions(List.copyOf(suggestions))
                .build();
    }

    public static boolean isSlow(DebugQueryLog log) {
        long retrievalMs = nullSafe(log.getRetrievalTimeMs());
        long generationMs = nullSafe(log.getGenerationTimeMs());
        long totalMs = nullSafe(log.getTotalTimeMs());
        return totalMs >= TOTAL_SLOW_MS
                || retrievalMs >= RETRIEVAL_SLOW_MS
                || generationMs >= GENERATION_SLOW_MS;
    }

    private static List<String> retrievalSuggestions(SearchMode searchMode) {
        return switch (searchMode) {
            case BM25 -> List.of(
                    "建议检查 Elasticsearch 连接与索引 rag_document_chunk 状态",
                    "建议确认 BM25 索引已重建且文档已同步"
            );
            case HYBRID -> List.of(
                    "建议检查 Elasticsearch 与 PgVector 状态",
                    "建议优化 ES 索引并确认向量重建完成"
            );
            default -> List.of(
                    "建议检查 PgVector 向量索引与 Embedding 状态",
                    "建议确认 Chunk 已完成向量化"
            );
        };
    }

    private static SearchMode resolveSearchMode(String searchMode) {
        if (searchMode == null || searchMode.isBlank()) {
            return SearchMode.VECTOR;
        }
        try {
            return SearchMode.valueOf(searchMode.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return SearchMode.VECTOR;
        }
    }

    private static long nullSafe(Long value) {
        return value == null ? 0L : value;
    }

    @Data
    @Builder
    public static class AnalysisResult {
        private List<String> slowReasons;
        private List<String> suggestions;
    }
}

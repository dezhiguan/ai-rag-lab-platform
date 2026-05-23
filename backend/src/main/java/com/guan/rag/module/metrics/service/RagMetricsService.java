package com.guan.rag.module.metrics.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guan.rag.module.debug.entity.DebugQueryLog;
import com.guan.rag.module.debug.mapper.DebugQueryLogMapper;
import com.guan.rag.module.metrics.mapper.RagMetricsMapper;
import com.guan.rag.module.metrics.model.RagMetricsCountRow;
import com.guan.rag.module.metrics.response.RagMetricsResponse;
import com.guan.rag.module.search.SearchMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RagMetricsService {

    private static final int SLOW_QUERY_LIMIT = 10;

    private final RagMetricsMapper ragMetricsMapper;
    private final DebugQueryLogMapper debugQueryLogMapper;

    public RagMetricsResponse getMetrics() {
        return RagMetricsResponse.builder()
                .totalQueryCount(ragMetricsMapper.countTotalQueries())
                .todayQueryCount(ragMetricsMapper.countTodayQueries())
                .avgTotalTimeMs(Math.round(ragMetricsMapper.avgTotalTimeMs()))
                .avgRetrievalTimeMs(Math.round(ragMetricsMapper.avgRetrievalTimeMs()))
                .avgGenerationTimeMs(Math.round(ragMetricsMapper.avgGenerationTimeMs()))
                .searchModeStats(buildSearchModeStats())
                .rerankStats(buildRerankStats())
                .slowQueries(listSlowQueries())
                .build();
    }

    private RagMetricsResponse.SearchModeStatsResponse buildSearchModeStats() {
        long vectorCount = 0;
        long bm25Count = 0;
        long hybridCount = 0;
        for (RagMetricsCountRow row : ragMetricsMapper.countBySearchMode()) {
            SearchMode mode = resolveSearchMode(row.getLabel());
            long count = row.getCount() == null ? 0L : row.getCount();
            switch (mode) {
                case BM25 -> bm25Count += count;
                case HYBRID -> hybridCount += count;
                default -> vectorCount += count;
            }
        }
        return RagMetricsResponse.SearchModeStatsResponse.builder()
                .vectorCount(vectorCount)
                .bm25Count(bm25Count)
                .hybridCount(hybridCount)
                .build();
    }

    private RagMetricsResponse.RerankStatsResponse buildRerankStats() {
        long enabledCount = 0;
        long disabledCount = 0;
        for (RagMetricsCountRow row : ragMetricsMapper.countByEnableRerank()) {
            long count = row.getCount() == null ? 0L : row.getCount();
            if (isRerankEnabled(row.getLabel())) {
                enabledCount += count;
            } else {
                disabledCount += count;
            }
        }
        return RagMetricsResponse.RerankStatsResponse.builder()
                .enabledCount(enabledCount)
                .disabledCount(disabledCount)
                .build();
    }

    private List<RagMetricsResponse.SlowQueryResponse> listSlowQueries() {
        return debugQueryLogMapper.selectList(
                new LambdaQueryWrapper<DebugQueryLog>()
                        .isNotNull(DebugQueryLog::getTotalTimeMs)
                        .orderByDesc(DebugQueryLog::getTotalTimeMs)
                        .last("LIMIT " + SLOW_QUERY_LIMIT)
        ).stream().map(this::toSlowQuery).toList();
    }

    private RagMetricsResponse.SlowQueryResponse toSlowQuery(DebugQueryLog log) {
        boolean enableRerank = log.getEnableRerank() != null && log.getEnableRerank() == 1;
        return RagMetricsResponse.SlowQueryResponse.builder()
                .queryLogId(log.getId())
                .question(log.getQuestion())
                .searchMode(resolveSearchMode(log.getSearchMode()).name())
                .enableRerank(enableRerank)
                .totalTimeMs(log.getTotalTimeMs())
                .createdAt(log.getCreatedAt() != null ? log.getCreatedAt().toString() : null)
                .build();
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

    private static boolean isRerankEnabled(String label) {
        if (label == null || label.isBlank()) {
            return false;
        }
        return "1".equals(label.trim());
    }
}

package com.guan.rag.module.querylog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guan.rag.module.debug.entity.DebugQueryLog;
import com.guan.rag.module.debug.mapper.DebugQueryLogMapper;
import com.guan.rag.module.querylog.analysis.SlowQueryAnalyzer;
import com.guan.rag.module.querylog.mapper.RagQueryLogAnalysisMapper;
import com.guan.rag.module.querylog.model.QueryLogChunkCountRow;
import com.guan.rag.module.querylog.response.RagSlowQueryAnalysisResponse;
import com.guan.rag.module.search.SearchMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RagSlowQueryService {

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 100;

    private final DebugQueryLogMapper debugQueryLogMapper;
    private final RagQueryLogAnalysisMapper ragQueryLogAnalysisMapper;

    public RagSlowQueryAnalysisResponse analyzeSlowQueries(Integer limit) {
        int resolvedLimit = resolveLimit(limit);

        LambdaQueryWrapper<DebugQueryLog> wrapper = new LambdaQueryWrapper<DebugQueryLog>()
                .and(w -> w.ge(DebugQueryLog::getTotalTimeMs, SlowQueryAnalyzer.TOTAL_SLOW_MS)
                        .or()
                        .ge(DebugQueryLog::getRetrievalTimeMs, SlowQueryAnalyzer.RETRIEVAL_SLOW_MS)
                        .or()
                        .ge(DebugQueryLog::getGenerationTimeMs, SlowQueryAnalyzer.GENERATION_SLOW_MS))
                .orderByDesc(DebugQueryLog::getTotalTimeMs)
                .last("LIMIT " + resolvedLimit);

        List<DebugQueryLog> logs = debugQueryLogMapper.selectList(wrapper);
        Map<Long, Integer> chunkCountMap = loadRetrievalChunkCounts(logs);

        List<RagSlowQueryAnalysisResponse.RagSlowQueryItemResponse> records = logs.stream()
                .map(log -> toSlowQueryItem(log, chunkCountMap.getOrDefault(log.getId(), 0)))
                .toList();

        return RagSlowQueryAnalysisResponse.builder()
                .totalCount(ragQueryLogAnalysisMapper.countSlowQueries())
                .records(records)
                .build();
    }

    private Map<Long, Integer> loadRetrievalChunkCounts(List<DebugQueryLog> logs) {
        if (logs.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> queryLogIds = logs.stream().map(DebugQueryLog::getId).toList();
        return ragQueryLogAnalysisMapper.countRetrievalChunksByQueryLogIds(queryLogIds).stream()
                .collect(Collectors.toMap(
                        QueryLogChunkCountRow::getQueryLogId,
                        row -> row.getChunkCount() == null ? 0 : row.getChunkCount().intValue(),
                        (left, right) -> left
                ));
    }

    private RagSlowQueryAnalysisResponse.RagSlowQueryItemResponse toSlowQueryItem(
            DebugQueryLog log,
            int retrievalChunkCount
    ) {
        SlowQueryAnalyzer.AnalysisResult analysis = SlowQueryAnalyzer.analyze(log, retrievalChunkCount);
        boolean enableRerank = log.getEnableRerank() != null && log.getEnableRerank() == 1;
        return RagSlowQueryAnalysisResponse.RagSlowQueryItemResponse.builder()
                .queryLogId(log.getId())
                .question(log.getQuestion())
                .searchMode(resolveSearchMode(log.getSearchMode()).name())
                .enableRerank(enableRerank)
                .retrievalTimeMs(log.getRetrievalTimeMs())
                .generationTimeMs(log.getGenerationTimeMs())
                .totalTimeMs(log.getTotalTimeMs())
                .retrievalChunkCount(retrievalChunkCount)
                .slowReasons(analysis.getSlowReasons())
                .suggestions(analysis.getSuggestions())
                .createdAt(log.getCreatedAt() != null ? log.getCreatedAt().toString() : null)
                .build();
    }

    private static int resolveLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
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
}

package com.guan.rag.module.querylog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guan.rag.module.debug.entity.DebugQueryLog;
import com.guan.rag.module.debug.mapper.DebugQueryLogMapper;
import com.guan.rag.module.querylog.response.RagQueryLogItemResponse;
import com.guan.rag.module.querylog.response.RagQueryLogPageResponse;
import com.guan.rag.module.search.SearchMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class RagQueryLogService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final long SLOW_QUERY_THRESHOLD_MS = 3000L;

    private final DebugQueryLogMapper debugQueryLogMapper;

    public RagQueryLogPageResponse pageQueryLogs(
            Integer page,
            Integer pageSize,
            String keyword,
            String searchMode,
            Boolean enableRerank,
            Boolean slowOnly
    ) {
        int currentPage = page == null || page < 1 ? DEFAULT_PAGE : page;
        int size = pageSize == null || pageSize < 1 ? DEFAULT_PAGE_SIZE : pageSize;

        LambdaQueryWrapper<DebugQueryLog> wrapper = new LambdaQueryWrapper<DebugQueryLog>()
                .orderByDesc(DebugQueryLog::getCreatedAt);

        if (StringUtils.hasText(keyword)) {
            wrapper.like(DebugQueryLog::getQuestion, keyword.trim());
        }
        applySearchModeFilter(wrapper, searchMode);
        applyEnableRerankFilter(wrapper, enableRerank);
        if (Boolean.TRUE.equals(slowOnly)) {
            wrapper.gt(DebugQueryLog::getTotalTimeMs, SLOW_QUERY_THRESHOLD_MS);
        }

        Page<DebugQueryLog> result = debugQueryLogMapper.selectPage(new Page<>(currentPage, size), wrapper);
        return RagQueryLogPageResponse.builder()
                .total(result.getTotal())
                .page(currentPage)
                .pageSize(size)
                .records(result.getRecords().stream().map(this::toItem).toList())
                .build();
    }

    private void applySearchModeFilter(LambdaQueryWrapper<DebugQueryLog> wrapper, String searchMode) {
        if (!StringUtils.hasText(searchMode)) {
            return;
        }
        SearchMode mode = SearchMode.from(searchMode.trim());
        if (mode == SearchMode.VECTOR) {
            wrapper.and(w -> w.eq(DebugQueryLog::getSearchMode, SearchMode.VECTOR.name())
                    .or()
                    .isNull(DebugQueryLog::getSearchMode)
                    .or()
                    .eq(DebugQueryLog::getSearchMode, ""));
            return;
        }
        wrapper.eq(DebugQueryLog::getSearchMode, mode.name());
    }

    private void applyEnableRerankFilter(LambdaQueryWrapper<DebugQueryLog> wrapper, Boolean enableRerank) {
        if (enableRerank == null) {
            return;
        }
        if (enableRerank) {
            wrapper.eq(DebugQueryLog::getEnableRerank, 1);
            return;
        }
        wrapper.and(w -> w.eq(DebugQueryLog::getEnableRerank, 0).or().isNull(DebugQueryLog::getEnableRerank));
    }

    private RagQueryLogItemResponse toItem(DebugQueryLog log) {
        boolean rerankEnabled = log.getEnableRerank() != null && log.getEnableRerank() == 1;
        return RagQueryLogItemResponse.builder()
                .queryLogId(log.getId())
                .question(log.getQuestion())
                .searchMode(resolveSearchMode(log.getSearchMode()).name())
                .enableRerank(rerankEnabled)
                .totalTimeMs(log.getTotalTimeMs())
                .retrievalTimeMs(log.getRetrievalTimeMs())
                .generationTimeMs(log.getGenerationTimeMs())
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
}

package com.guan.rag.module.debug.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guan.rag.common.exception.BusinessException;
import com.guan.rag.module.debug.entity.DebugQueryLog;
import com.guan.rag.module.debug.entity.DebugRetrievalLog;
import com.guan.rag.module.debug.mapper.DebugQueryLogMapper;
import com.guan.rag.module.debug.mapper.DebugRetrievalLogMapper;
import com.guan.rag.module.debug.request.DebugQueryRequest;
import com.guan.rag.module.debug.response.DebugLatencyResponse;
import com.guan.rag.module.debug.response.DebugQueryLogSummaryResponse;
import com.guan.rag.module.debug.response.DebugQueryResponse;
import com.guan.rag.module.debug.response.DebugRetrievedChunkResponse;
import com.guan.rag.module.rag.pipeline.RagQueryPipelineRequest;
import com.guan.rag.module.rag.pipeline.RagQueryPipelineService;
import com.guan.rag.module.kb.service.KnowledgeBaseService;
import com.guan.rag.module.search.SearchMode;
import com.guan.rag.module.token.TokenUsageService;
import com.guan.rag.module.token.response.TokenUsageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DebugService {

    private static final int DEFAULT_HISTORY_LIMIT = 50;

    private final KnowledgeBaseService knowledgeBaseService;
    private final RagQueryPipelineService ragQueryPipelineService;
    private final DebugQueryLogMapper debugQueryLogMapper;
    private final DebugRetrievalLogMapper debugRetrievalLogMapper;
    private final TokenUsageService tokenUsageService;

    @Transactional
    public DebugQueryResponse query(DebugQueryRequest request) {
        return ragQueryPipelineService.execute(RagQueryPipelineRequest.builder()
                .kbId(request.getKbId())
                .question(request.getQuestion())
                .topK(request.getTopK())
                .searchMode(request.getSearchMode())
                .enableRerank(request.getEnableRerank())
                .persistLog(true)
                .build());
    }

    public List<DebugQueryLogSummaryResponse> listQueryLogs(Long kbId) {
        if (kbId != null) {
            knowledgeBaseService.requireKb(kbId);
        }
        LambdaQueryWrapper<DebugQueryLog> wrapper = new LambdaQueryWrapper<DebugQueryLog>()
                .orderByDesc(DebugQueryLog::getCreatedAt)
                .last("LIMIT " + DEFAULT_HISTORY_LIMIT);
        if (kbId != null) {
            wrapper.eq(DebugQueryLog::getKbId, kbId);
        }
        return debugQueryLogMapper.selectList(wrapper).stream()
                .map(this::toSummary)
                .toList();
    }

    public DebugQueryResponse getQueryLogDetail(Long queryLogId) {
        DebugQueryLog queryLog = requireQueryLog(queryLogId);
        List<DebugRetrievedChunkResponse> chunks = debugRetrievalLogMapper.selectList(
                new LambdaQueryWrapper<DebugRetrievalLog>()
                        .eq(DebugRetrievalLog::getQueryLogId, queryLogId)
                        .orderByAsc(DebugRetrievalLog::getRankPosition)
        ).stream().map(this::toRetrievedChunk).toList();

        List<DebugRetrievedChunkResponse> contextChunks = chunks.stream()
                .filter(c -> Boolean.TRUE.equals(c.getUsedInPrompt()))
                .toList();

        boolean enableRerank = queryLog.getEnableRerank() != null && queryLog.getEnableRerank() == 1;
        return DebugQueryResponse.builder()
                .queryLogId(queryLog.getId())
                .kbId(queryLog.getKbId())
                .question(queryLog.getQuestion())
                .searchMode(queryLog.getSearchMode() != null ? queryLog.getSearchMode() : SearchMode.VECTOR.name())
                .enableRerank(enableRerank)
                .embeddingProvider(queryLog.getEmbeddingProvider())
                .embeddingModel(queryLog.getEmbeddingModel())
                .chatProvider(queryLog.getChatProvider())
                .chatModel(queryLog.getChatModel())
                .retrievedChunks(chunks)
                .contextChunks(contextChunks)
                .context(queryLog.getContext())
                .prompt(queryLog.getPrompt())
                .answer(queryLog.getAnswer())
                .latency(DebugLatencyResponse.builder()
                        .retrievalTimeMs(queryLog.getRetrievalTimeMs())
                        .generationTimeMs(queryLog.getGenerationTimeMs())
                        .totalTimeMs(queryLog.getTotalTimeMs())
                        .build())
                .tokenUsage(resolveTokenUsage(queryLog))
                .build();
    }

    private TokenUsageResponse resolveTokenUsage(DebugQueryLog queryLog) {
        TokenUsageResponse persisted = TokenUsageResponse.from(tokenUsageService.fromPersisted(
                new TokenUsageService.DebugQueryLogTokenFields(
                        queryLog.getQuestionTokens(),
                        queryLog.getContextTokens(),
                        queryLog.getSystemPromptTokens(),
                        queryLog.getAnswerTokens(),
                        queryLog.getInputTokens(),
                        queryLog.getOutputTokens(),
                        queryLog.getTotalTokens(),
                        queryLog.getEstimatedCost(),
                        queryLog.getEmbeddingTokens(),
                        queryLog.getEmbeddingCost(),
                        queryLog.getTotalCost(),
                        queryLog.getPriceConfigured(),
                        queryLog.getEmbeddingProvider(),
                        queryLog.getEmbeddingModel(),
                        queryLog.getChatProvider(),
                        queryLog.getChatModel()
                )
        ));
        if (persisted != null) {
            return persisted;
        }
        return TokenUsageResponse.from(tokenUsageService.buildUsage(
                queryLog.getQuestion(),
                queryLog.getContext(),
                queryLog.getAnswer(),
                queryLog.getEmbeddingProvider(),
                queryLog.getEmbeddingModel(),
                queryLog.getChatProvider(),
                queryLog.getChatModel(),
                null,
                null
        ));
    }

    private DebugRetrievedChunkResponse toRetrievedChunk(DebugRetrievalLog log) {
        return DebugRetrievedChunkResponse.builder()
                .documentId(log.getDocumentId())
                .documentName(log.getDocumentName())
                .chunkId(log.getChunkId())
                .chunkIndex(log.getChunkIndex())
                .score(log.getScore())
                .rankPosition(log.getRankPosition())
                .content(log.getContent())
                .usedInPrompt(log.getUsedInPrompt() != null && log.getUsedInPrompt() == 1)
                .filterReason(log.getFilterReason())
                .originalRank(log.getOriginalRank())
                .rerankRank(log.getRerankRank())
                .rerankScore(log.getRerankScore())
                .build();
    }

    private DebugQueryLog requireQueryLog(Long queryLogId) {
        DebugQueryLog queryLog = debugQueryLogMapper.selectById(queryLogId);
        if (queryLog == null) {
            throw new BusinessException("Debug 查询记录不存在");
        }
        return queryLog;
    }

    private DebugQueryLogSummaryResponse toSummary(DebugQueryLog log) {
        return DebugQueryLogSummaryResponse.builder()
                .queryLogId(log.getId())
                .kbId(log.getKbId())
                .question(log.getQuestion())
                .embeddingProvider(log.getEmbeddingProvider())
                .embeddingModel(log.getEmbeddingModel())
                .chatProvider(log.getChatProvider())
                .chatModel(log.getChatModel())
                .topK(log.getTopK())
                .retrievalTimeMs(log.getRetrievalTimeMs())
                .generationTimeMs(log.getGenerationTimeMs())
                .totalTimeMs(log.getTotalTimeMs())
                .createdAt(log.getCreatedAt())
                .build();
    }
}

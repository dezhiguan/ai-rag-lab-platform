package com.guan.rag.module.debug.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guan.rag.common.exception.BusinessException;
import com.guan.rag.module.chat.prompt.PromptBuilder;
import com.guan.rag.module.chat.provider.ChatModelProvider;
import com.guan.rag.module.chat.provider.ChatModelProviderRouter;
import com.guan.rag.module.chat.support.ContextChunkFilter;
import com.guan.rag.module.chat.support.ContextFilterResult;
import com.guan.rag.module.chat.support.ContextTextBuilder;
import com.guan.rag.module.debug.entity.DebugQueryLog;
import com.guan.rag.module.debug.entity.DebugRetrievalLog;
import com.guan.rag.module.debug.mapper.DebugQueryLogMapper;
import com.guan.rag.module.debug.mapper.DebugRetrievalLogMapper;
import com.guan.rag.module.debug.request.DebugQueryRequest;
import com.guan.rag.module.debug.response.DebugLatencyResponse;
import com.guan.rag.module.debug.response.DebugQueryLogSummaryResponse;
import com.guan.rag.module.debug.response.DebugQueryResponse;
import com.guan.rag.module.debug.response.DebugRetrievedChunkResponse;
import com.guan.rag.module.embedding.provider.EmbeddingProviderRouter;
import com.guan.rag.module.kb.service.KnowledgeBaseService;
import com.guan.rag.module.retrieval.response.RetrievedChunkResponse;
import com.guan.rag.module.retrieval.service.VectorRetrievalService;
import com.guan.rag.module.search.SearchMode;
import com.guan.rag.module.search.service.Bm25SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DebugService {

    private static final int DEFAULT_HISTORY_LIMIT = 50;

    private final KnowledgeBaseService knowledgeBaseService;
    private final VectorRetrievalService vectorRetrievalService;
    private final Bm25SearchService bm25SearchService;
    private final ContextChunkFilter contextChunkFilter;
    private final PromptBuilder promptBuilder;
    private final ChatModelProvider chatModelProvider;
    private final EmbeddingProviderRouter embeddingProviderRouter;
    private final ChatModelProviderRouter chatModelProviderRouter;
    private final DebugQueryLogMapper debugQueryLogMapper;
    private final DebugRetrievalLogMapper debugRetrievalLogMapper;

    @Transactional
    public DebugQueryResponse query(DebugQueryRequest request) {
        long totalStart = System.currentTimeMillis();
        knowledgeBaseService.requireKb(request.getKbId());
        int topK = request.getTopK() == null ? 5 : request.getTopK();
        String question = request.getQuestion().trim();
        SearchMode searchMode = SearchMode.from(request.getSearchMode());

        String embeddingProvider = embeddingProviderRouter.configuredProvider();
        String embeddingModel = embeddingProviderRouter.model();
        String chatProvider = chatModelProviderRouter.configuredProvider();
        String chatModel = chatModelProviderRouter.model();

        long retrievalStart = System.currentTimeMillis();
        List<RetrievedChunkResponse> retrieved = retrieveChunks(searchMode, request.getKbId(), question, topK);
        long retrievalTimeMs = System.currentTimeMillis() - retrievalStart;

        List<RetrievedChunkResponse> forFilter = searchMode == SearchMode.BM25
                ? bm25SearchService.retrieveNormalizedForFilter(request.getKbId(), question, topK)
                : retrieved;
        ContextFilterResult filterResult = contextChunkFilter.filter(forFilter);
        List<DebugRetrievedChunkResponse> retrievedChunks = toRetrievedChunks(retrieved, filterResult);
        List<DebugRetrievedChunkResponse> contextChunks = toContextChunkResponses(
                filterResult.getContextChunks(), filterResult);

        String context = ContextTextBuilder.build(filterResult.getContextChunks());
        String prompt = promptBuilder.build(context, question);

        long generationStart = System.currentTimeMillis();
        ChatModelProvider.ChatResult result = chatModelProvider.chat(prompt);
        long generationTimeMs = System.currentTimeMillis() - generationStart;
        if (result.latencyMs() > 0) {
            generationTimeMs = result.latencyMs();
        }

        long totalTimeMs = System.currentTimeMillis() - totalStart;

        DebugQueryLog queryLog = new DebugQueryLog();
        queryLog.setKbId(request.getKbId());
        queryLog.setQuestion(question);
        queryLog.setPrompt(prompt);
        queryLog.setContext(context);
        queryLog.setAnswer(result.answer());
        queryLog.setTopK(topK);
        queryLog.setSearchMode(searchMode.name());
        queryLog.setEmbeddingProvider(embeddingProvider);
        queryLog.setEmbeddingModel(embeddingModel);
        queryLog.setChatProvider(chatProvider);
        queryLog.setChatModel(chatModel);
        queryLog.setRetrievalTimeMs(retrievalTimeMs);
        queryLog.setGenerationTimeMs(generationTimeMs);
        queryLog.setTotalTimeMs(totalTimeMs);
        debugQueryLogMapper.insert(queryLog);

        saveRetrievalLogs(queryLog.getId(), request.getKbId(), retrievedChunks);

        return DebugQueryResponse.builder()
                .queryLogId(queryLog.getId())
                .kbId(request.getKbId())
                .question(question)
                .searchMode(searchMode.name())
                .embeddingProvider(embeddingProvider)
                .embeddingModel(embeddingModel)
                .chatProvider(chatProvider)
                .chatModel(chatModel)
                .retrievedChunks(retrievedChunks)
                .contextChunks(contextChunks)
                .context(context)
                .prompt(prompt)
                .answer(result.answer())
                .latency(DebugLatencyResponse.builder()
                        .retrievalTimeMs(retrievalTimeMs)
                        .generationTimeMs(generationTimeMs)
                        .totalTimeMs(totalTimeMs)
                        .build())
                .build();
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

        return DebugQueryResponse.builder()
                .queryLogId(queryLog.getId())
                .kbId(queryLog.getKbId())
                .question(queryLog.getQuestion())
                .searchMode(queryLog.getSearchMode() != null ? queryLog.getSearchMode() : SearchMode.VECTOR.name())
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
                .build();
    }

    private List<RetrievedChunkResponse> retrieveChunks(SearchMode searchMode, Long kbId, String question, int topK) {
        if (searchMode == SearchMode.BM25) {
            return bm25SearchService.retrieve(kbId, question, topK);
        }
        return vectorRetrievalService.retrieve(kbId, question, topK);
    }

    private void saveRetrievalLogs(Long queryLogId, Long kbId, List<DebugRetrievedChunkResponse> chunks) {
        for (DebugRetrievedChunkResponse chunk : chunks) {
            DebugRetrievalLog log = new DebugRetrievalLog();
            log.setQueryLogId(queryLogId);
            log.setKbId(kbId);
            log.setDocumentId(chunk.getDocumentId());
            log.setDocumentName(chunk.getDocumentName());
            log.setChunkId(chunk.getChunkId());
            log.setChunkIndex(chunk.getChunkIndex());
            log.setScore(chunk.getScore());
            log.setContent(chunk.getContent());
            log.setRankPosition(chunk.getRankPosition());
            log.setUsedInPrompt(Boolean.TRUE.equals(chunk.getUsedInPrompt()) ? 1 : 0);
            log.setFilterReason(chunk.getFilterReason());
            debugRetrievalLogMapper.insert(log);
        }
    }

    private List<DebugRetrievedChunkResponse> toRetrievedChunks(
            List<RetrievedChunkResponse> retrieved,
            ContextFilterResult filterResult
    ) {
        Map<Long, ContextFilterResult.ChunkFilterDecision> decisions = filterResult.getDecisions();
        List<DebugRetrievedChunkResponse> result = new ArrayList<>();
        for (int i = 0; i < retrieved.size(); i++) {
            RetrievedChunkResponse item = retrieved.get(i);
            ContextFilterResult.ChunkFilterDecision decision = decisions.get(item.getChunkId());
            result.add(DebugRetrievedChunkResponse.builder()
                    .documentId(item.getDocumentId())
                    .documentName(item.getDocumentName())
                    .chunkId(item.getChunkId())
                    .chunkIndex(item.getChunkIndex())
                    .score(item.getScore())
                    .rankPosition(i + 1)
                    .content(item.getContent())
                    .usedInPrompt(decision != null && decision.isUsedInPrompt())
                    .filterReason(decision != null ? decision.getFilterReason() : null)
                    .build());
        }
        return result;
    }

    private List<DebugRetrievedChunkResponse> toContextChunkResponses(
            List<RetrievedChunkResponse> contextChunks,
            ContextFilterResult filterResult
    ) {
        List<DebugRetrievedChunkResponse> result = new ArrayList<>();
        for (int i = 0; i < contextChunks.size(); i++) {
            RetrievedChunkResponse item = contextChunks.get(i);
            result.add(DebugRetrievedChunkResponse.builder()
                    .documentId(item.getDocumentId())
                    .documentName(item.getDocumentName())
                    .chunkId(item.getChunkId())
                    .chunkIndex(item.getChunkIndex())
                    .score(item.getScore())
                    .rankPosition(i + 1)
                    .content(item.getContent())
                    .usedInPrompt(true)
                    .filterReason(null)
                    .build());
        }
        return result;
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

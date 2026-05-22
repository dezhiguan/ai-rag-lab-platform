package com.guan.rag.module.debug.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guan.rag.common.exception.BusinessException;
import com.guan.rag.module.chat.prompt.PromptBuilder;
import com.guan.rag.module.chat.provider.ChatModelProvider;
import com.guan.rag.module.chat.provider.ChatModelProviderRouter;
import com.guan.rag.module.chat.support.ChatRelevanceFilter;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DebugService {

    private static final int DEFAULT_HISTORY_LIMIT = 50;

    private final KnowledgeBaseService knowledgeBaseService;
    private final VectorRetrievalService vectorRetrievalService;
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

        String embeddingProvider = embeddingProviderRouter.configuredProvider();
        String embeddingModel = embeddingProviderRouter.model();
        String chatProvider = chatModelProviderRouter.configuredProvider();
        String chatModel = chatModelProviderRouter.model();

        long retrievalStart = System.currentTimeMillis();
        List<RetrievedChunkResponse> retrieved = vectorRetrievalService.retrieve(
                request.getKbId(), question, topK);
        long retrievalTimeMs = System.currentTimeMillis() - retrievalStart;

        List<DebugRetrievedChunkResponse> retrievedChunks = toRetrievedChunks(retrieved);

        List<RetrievedChunkResponse> relevant = ChatRelevanceFilter.filter(question, retrieved);
        String context = buildContext(relevant);
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
                .embeddingProvider(embeddingProvider)
                .embeddingModel(embeddingModel)
                .chatProvider(chatProvider)
                .chatModel(chatModel)
                .retrievedChunks(retrievedChunks)
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

        return DebugQueryResponse.builder()
                .queryLogId(queryLog.getId())
                .kbId(queryLog.getKbId())
                .question(queryLog.getQuestion())
                .embeddingProvider(queryLog.getEmbeddingProvider())
                .embeddingModel(queryLog.getEmbeddingModel())
                .chatProvider(queryLog.getChatProvider())
                .chatModel(queryLog.getChatModel())
                .retrievedChunks(chunks)
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
            debugRetrievalLogMapper.insert(log);
        }
    }

    private List<DebugRetrievedChunkResponse> toRetrievedChunks(List<RetrievedChunkResponse> retrieved) {
        List<DebugRetrievedChunkResponse> result = new ArrayList<>();
        for (int i = 0; i < retrieved.size(); i++) {
            RetrievedChunkResponse item = retrieved.get(i);
            result.add(DebugRetrievedChunkResponse.builder()
                    .documentId(item.getDocumentId())
                    .documentName(item.getDocumentName())
                    .chunkId(item.getChunkId())
                    .chunkIndex(item.getChunkIndex())
                    .score(item.getScore())
                    .rankPosition(i + 1)
                    .content(item.getContent())
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
                .build();
    }

    private String buildContext(List<RetrievedChunkResponse> retrieved) {
        if (retrieved.isEmpty()) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        for (int i = 0; i < retrieved.size(); i++) {
            RetrievedChunkResponse item = retrieved.get(i);
            parts.add("【片段" + (i + 1) + "】文档：" + item.getDocumentName()
                    + "，Chunk #" + item.getChunkIndex()
                    + "\n" + item.getContent());
        }
        return String.join("\n\n", parts);
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

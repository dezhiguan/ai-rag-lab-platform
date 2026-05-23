package com.guan.rag.module.rag.pipeline;

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
import com.guan.rag.module.debug.response.DebugLatencyResponse;
import com.guan.rag.module.debug.response.DebugQueryResponse;
import com.guan.rag.module.debug.response.DebugRetrievedChunkResponse;
import com.guan.rag.module.embedding.provider.EmbeddingProviderRouter;
import com.guan.rag.module.kb.service.KnowledgeBaseService;
import com.guan.rag.module.rerank.RerankResult;
import com.guan.rag.module.rerank.RerankService;
import com.guan.rag.module.retrieval.response.RetrievedChunkResponse;
import com.guan.rag.module.retrieval.service.VectorRetrievalService;
import com.guan.rag.module.search.SearchMode;
import com.guan.rag.module.search.hybrid.HybridSearchResult;
import com.guan.rag.module.search.hybrid.HybridSearchService;
import com.guan.rag.module.search.service.Bm25SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RagQueryPipelineService {

    private final KnowledgeBaseService knowledgeBaseService;
    private final VectorRetrievalService vectorRetrievalService;
    private final Bm25SearchService bm25SearchService;
    private final HybridSearchService hybridSearchService;
    private final RerankService rerankService;
    private final ContextChunkFilter contextChunkFilter;
    private final PromptBuilder promptBuilder;
    private final ChatModelProvider chatModelProvider;
    private final EmbeddingProviderRouter embeddingProviderRouter;
    private final ChatModelProviderRouter chatModelProviderRouter;
    private final DebugQueryLogMapper debugQueryLogMapper;
    private final DebugRetrievalLogMapper debugRetrievalLogMapper;

    @Transactional
    public DebugQueryResponse execute(RagQueryPipelineRequest request) {
        long totalStart = System.currentTimeMillis();
        knowledgeBaseService.requireKb(request.getKbId());
        int topK = request.getTopK() == null ? 5 : request.getTopK();
        String question = request.getQuestion().trim();
        SearchMode searchMode = SearchMode.from(request.getSearchMode());
        boolean enableRerank = Boolean.TRUE.equals(request.getEnableRerank());

        String embeddingProvider = embeddingProviderRouter.configuredProvider();
        String embeddingModel = embeddingProviderRouter.model();
        String chatProvider = chatModelProviderRouter.configuredProvider();
        String chatModel = chatModelProviderRouter.model();

        long retrievalStart = System.currentTimeMillis();
        List<HybridSearchResult> hybridResults = null;
        List<RetrievedChunkResponse> candidates;
        if (searchMode == SearchMode.HYBRID) {
            hybridResults = hybridSearchService.search(request.getKbId(), question, topK);
            candidates = hybridSearchService.fromHybridResults(hybridResults, false);
        } else {
            candidates = retrieveChunks(searchMode, request.getKbId(), question, topK);
        }
        long retrievalTimeMs = System.currentTimeMillis() - retrievalStart;

        RerankResult rerankResult = null;
        List<RetrievedChunkResponse> forFilter;
        if (enableRerank) {
            rerankResult = rerankService.rerank(question, candidates);
            forFilter = rerankService.toNormalizedForFilter(rerankResult);
        } else {
            forFilter = switch (searchMode) {
                case BM25 -> bm25SearchService.retrieveNormalizedForFilter(request.getKbId(), question, topK);
                case HYBRID -> hybridSearchService.toRetrievedChunksForFilter(hybridResults);
                default -> candidates;
            };
        }
        ContextFilterResult filterResult = contextChunkFilter.filter(forFilter, request.getContextFilter());
        List<DebugRetrievedChunkResponse> retrievedChunks;
        if (enableRerank) {
            retrievedChunks = toRetrievedChunksWithRerank(rerankResult, filterResult, hybridResults);
        } else if (searchMode == SearchMode.HYBRID) {
            retrievedChunks = toRetrievedChunksFromHybrid(hybridResults, filterResult);
        } else {
            retrievedChunks = toRetrievedChunks(candidates, filterResult);
        }
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

        Long queryLogId = null;
        if (request.isPersistLog()) {
            DebugQueryLog queryLog = new DebugQueryLog();
            queryLog.setKbId(request.getKbId());
            queryLog.setQuestion(question);
            queryLog.setPrompt(prompt);
            queryLog.setContext(context);
            queryLog.setAnswer(result.answer());
            queryLog.setTopK(topK);
            queryLog.setSearchMode(searchMode.name());
            queryLog.setEnableRerank(enableRerank ? 1 : 0);
            queryLog.setEmbeddingProvider(embeddingProvider);
            queryLog.setEmbeddingModel(embeddingModel);
            queryLog.setChatProvider(chatProvider);
            queryLog.setChatModel(chatModel);
            queryLog.setRetrievalTimeMs(retrievalTimeMs);
            queryLog.setGenerationTimeMs(generationTimeMs);
            queryLog.setTotalTimeMs(totalTimeMs);
            debugQueryLogMapper.insert(queryLog);
            queryLogId = queryLog.getId();
            saveRetrievalLogs(queryLogId, request.getKbId(), retrievedChunks);
        }

        return DebugQueryResponse.builder()
                .queryLogId(queryLogId)
                .kbId(request.getKbId())
                .question(question)
                .searchMode(searchMode.name())
                .enableRerank(enableRerank)
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

    private List<RetrievedChunkResponse> retrieveChunks(SearchMode searchMode, Long kbId, String question, int topK) {
        return switch (searchMode) {
            case BM25 -> bm25SearchService.retrieve(kbId, question, topK);
            case HYBRID -> hybridSearchService.retrieve(kbId, question, topK);
            default -> vectorRetrievalService.retrieve(kbId, question, topK);
        };
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
            log.setOriginalRank(chunk.getOriginalRank());
            log.setRerankRank(chunk.getRerankRank());
            log.setRerankScore(chunk.getRerankScore());
            debugRetrievalLogMapper.insert(log);
        }
    }

    private List<DebugRetrievedChunkResponse> toRetrievedChunksWithRerank(
            RerankResult rerankResult,
            ContextFilterResult filterResult,
            List<HybridSearchResult> hybridResults
    ) {
        Map<Long, HybridSearchResult> hybridByChunkId = hybridResults == null
                ? Map.of()
                : hybridResults.stream()
                .filter(h -> h.getChunkId() != null)
                .collect(Collectors.toMap(HybridSearchResult::getChunkId, Function.identity(), (a, b) -> a));
        Map<Long, ContextFilterResult.ChunkFilterDecision> decisions = filterResult.getDecisions();
        List<DebugRetrievedChunkResponse> result = new ArrayList<>();
        for (RerankResult.RerankedItem item : rerankResult.getItems()) {
            RetrievedChunkResponse chunk = item.getChunk();
            ContextFilterResult.ChunkFilterDecision decision = decisions.get(chunk.getChunkId());
            DebugRetrievedChunkResponse.DebugRetrievedChunkResponseBuilder builder = DebugRetrievedChunkResponse.builder()
                    .documentId(chunk.getDocumentId())
                    .documentName(chunk.getDocumentName())
                    .chunkId(chunk.getChunkId())
                    .chunkIndex(chunk.getChunkIndex())
                    .score(chunk.getScore())
                    .rankPosition(item.getRerankRank())
                    .content(chunk.getContent())
                    .usedInPrompt(decision != null && decision.isUsedInPrompt())
                    .filterReason(decision != null ? decision.getFilterReason() : null)
                    .originalRank(item.getOriginalRank())
                    .rerankRank(item.getRerankRank())
                    .rerankScore(item.getRerankScore());
            HybridSearchResult hybrid = hybridByChunkId.get(chunk.getChunkId());
            if (hybrid != null) {
                builder.matchedByVector(hybrid.getVectorRank() != null)
                        .matchedByBm25(hybrid.getBm25Rank() != null)
                        .vectorScore(hybrid.getVectorScore())
                        .bm25Score(hybrid.getBm25Score())
                        .hybridScore(hybrid.getHybridScore());
            }
            result.add(builder.build());
        }
        return result;
    }

    private List<DebugRetrievedChunkResponse> toRetrievedChunksFromHybrid(
            List<HybridSearchResult> hybridResults,
            ContextFilterResult filterResult
    ) {
        Map<Long, ContextFilterResult.ChunkFilterDecision> decisions = filterResult.getDecisions();
        List<DebugRetrievedChunkResponse> result = new ArrayList<>();
        for (int i = 0; i < hybridResults.size(); i++) {
            HybridSearchResult item = hybridResults.get(i);
            ContextFilterResult.ChunkFilterDecision decision = decisions.get(item.getChunkId());
            boolean matchedVector = item.getVectorRank() != null;
            boolean matchedBm25 = item.getBm25Rank() != null;
            result.add(DebugRetrievedChunkResponse.builder()
                    .documentId(item.getDocumentId())
                    .documentName(item.getDocumentName())
                    .chunkId(item.getChunkId())
                    .chunkIndex(item.getChunkIndex())
                    .score(item.getHybridScore())
                    .rankPosition(i + 1)
                    .content(item.getContent())
                    .usedInPrompt(decision != null && decision.isUsedInPrompt())
                    .filterReason(decision != null ? decision.getFilterReason() : null)
                    .matchedByVector(matchedVector)
                    .matchedByBm25(matchedBm25)
                    .vectorScore(item.getVectorScore())
                    .bm25Score(item.getBm25Score())
                    .hybridScore(item.getHybridScore())
                    .build());
        }
        return result;
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
}

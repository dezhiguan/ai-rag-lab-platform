package com.guan.rag.module.experiment.service;

import com.guan.rag.module.chat.support.ContextFilterOptions;
import com.guan.rag.module.debug.response.DebugQueryResponse;
import com.guan.rag.module.experiment.request.ExperimentRagQueryRequest;
import com.guan.rag.module.experiment.response.ExperimentRagQueryResponse;
import com.guan.rag.module.rag.pipeline.RagQueryPipelineRequest;
import com.guan.rag.module.rag.pipeline.RagQueryPipelineService;
import com.guan.rag.module.search.SearchMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExperimentService {

    private final RagQueryPipelineService ragQueryPipelineService;

    public ExperimentRagQueryResponse runRagQuery(ExperimentRagQueryRequest request) {
        int topK = request.getTopK() == null ? 5 : request.getTopK();
        int maxChunks = request.getMaxChunks() == null ? 2 : request.getMaxChunks();
        double minScore = request.getMinScore() == null ? 0.45 : request.getMinScore();
        double maxScoreGap = request.getMaxScoreGap() == null ? 0.35 : request.getMaxScoreGap();
        SearchMode searchMode = SearchMode.from(request.getSearchMode());
        boolean enableRerank = Boolean.TRUE.equals(request.getEnableRerank());

        DebugQueryResponse pipelineResult = ragQueryPipelineService.execute(RagQueryPipelineRequest.builder()
                .kbId(request.getKbId())
                .question(request.getQuestion())
                .topK(topK)
                .searchMode(searchMode.name())
                .enableRerank(enableRerank)
                .contextFilter(ContextFilterOptions.builder()
                        .maxChunks(maxChunks)
                        .minScore(minScore)
                        .maxScoreGap(maxScoreGap)
                        .build())
                .persistLog(true)
                .build());

        int retrievedCount = pipelineResult.getRetrievedChunks() == null
                ? 0
                : pipelineResult.getRetrievedChunks().size();
        int contextCount = pipelineResult.getContextChunks() == null
                ? 0
                : pipelineResult.getContextChunks().size();

        return ExperimentRagQueryResponse.builder()
                .queryLogId(pipelineResult.getQueryLogId())
                .kbId(pipelineResult.getKbId())
                .question(pipelineResult.getQuestion())
                .answer(pipelineResult.getAnswer())
                .prompt(pipelineResult.getPrompt())
                .context(pipelineResult.getContext())
                .retrievedChunks(pipelineResult.getRetrievedChunks())
                .contextChunks(pipelineResult.getContextChunks())
                .latency(pipelineResult.getLatency())
                .usedParams(ExperimentRagQueryResponse.ExperimentParamsResponse.builder()
                        .topK(topK)
                        .searchMode(searchMode.name())
                        .enableRerank(enableRerank)
                        .maxChunks(maxChunks)
                        .minScore(minScore)
                        .maxScoreGap(maxScoreGap)
                        .build())
                .impact(ExperimentRagQueryResponse.ExperimentImpactResponse.builder()
                        .retrievedCount(retrievedCount)
                        .contextCount(contextCount)
                        .filteredCount(Math.max(retrievedCount - contextCount, 0))
                        .build())
                .tokenUsage(pipelineResult.getTokenUsage())
                .build();
    }
}

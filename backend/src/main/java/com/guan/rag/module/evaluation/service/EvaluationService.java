package com.guan.rag.module.evaluation.service;

import com.guan.rag.module.evaluation.EvaluationTestCatalog;
import com.guan.rag.module.evaluation.request.EvaluationCompareRequest;
import com.guan.rag.module.evaluation.request.EvaluationRunRequest;
import com.guan.rag.module.evaluation.response.EvaluationCaseResultResponse;
import com.guan.rag.module.evaluation.response.EvaluationCompareCaseResponse;
import com.guan.rag.module.evaluation.response.EvaluationCompareResponse;
import com.guan.rag.module.evaluation.response.EvaluationModeHitResponse;
import com.guan.rag.module.evaluation.response.EvaluationModeSummaryResponse;
import com.guan.rag.module.evaluation.response.EvaluationRunResponse;
import com.guan.rag.module.evaluation.response.EvaluationTestCaseResponse;
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EvaluationService {

    private static final List<SearchMode> COMPARE_MODES = List.of(
            SearchMode.VECTOR,
            SearchMode.BM25,
            SearchMode.HYBRID
    );

    private final KnowledgeBaseService knowledgeBaseService;
    private final VectorRetrievalService vectorRetrievalService;
    private final Bm25SearchService bm25SearchService;
    private final HybridSearchService hybridSearchService;
    private final RerankService rerankService;

    public List<EvaluationTestCaseResponse> listBuiltinCases() {
        return EvaluationTestCatalog.builtinCases();
    }

    public EvaluationRunResponse run(EvaluationRunRequest request) {
        knowledgeBaseService.requireKb(request.getKbId());
        SearchMode searchMode = SearchMode.from(request.getSearchMode());
        int topK = request.getTopK() == null ? 5 : request.getTopK();
        boolean enableRerank = Boolean.TRUE.equals(request.getEnableRerank());
        return runSingleMode(request.getKbId(), searchMode, topK, enableRerank);
    }

    public EvaluationCompareResponse compare(EvaluationCompareRequest request) {
        knowledgeBaseService.requireKb(request.getKbId());
        int topK = request.getTopK() == null ? 5 : request.getTopK();
        boolean enableRerank = Boolean.TRUE.equals(request.getEnableRerank());

        List<EvaluationTestCaseResponse> testCases = EvaluationTestCatalog.builtinCases();
        Map<String, EvaluationCompareCaseResponse.EvaluationCompareCaseResponseBuilder> rowBuilders =
                new LinkedHashMap<>();
        for (EvaluationTestCaseResponse testCase : testCases) {
            rowBuilders.put(testCase.getCaseId(), EvaluationCompareCaseResponse.builder()
                    .caseId(testCase.getCaseId())
                    .question(testCase.getQuestion())
                    .expectedDocument(testCase.getExpectedDocument()));
        }

        List<EvaluationModeSummaryResponse> summaries = new ArrayList<>();
        for (SearchMode mode : COMPARE_MODES) {
            EvaluationRunResponse modeRun = runSingleMode(request.getKbId(), mode, topK, enableRerank);
            summaries.add(toModeSummary(modeRun));
            for (EvaluationCaseResultResponse result : modeRun.getResults()) {
                EvaluationModeHitResponse hit = EvaluationModeHitResponse.builder()
                        .searchMode(mode.name())
                        .actualTop1Document(result.getActualTop1Document())
                        .passed(result.getPassed())
                        .latencyMs(result.getLatencyMs())
                        .build();
                EvaluationCompareCaseResponse.EvaluationCompareCaseResponseBuilder builder =
                        rowBuilders.get(result.getCaseId());
                if (builder != null) {
                    switch (mode) {
                        case BM25 -> builder.bm25(hit);
                        case HYBRID -> builder.hybrid(hit);
                        default -> builder.vector(hit);
                    }
                }
            }
        }

        List<EvaluationCompareCaseResponse> cases = rowBuilders.values().stream()
                .map(EvaluationCompareCaseResponse.EvaluationCompareCaseResponseBuilder::build)
                .toList();

        return EvaluationCompareResponse.builder()
                .kbId(request.getKbId())
                .enableRerank(enableRerank)
                .modeSummaries(summaries)
                .cases(cases)
                .build();
    }

    private EvaluationRunResponse runSingleMode(
            Long kbId,
            SearchMode searchMode,
            int topK,
            boolean enableRerank
    ) {
        List<EvaluationTestCaseResponse> cases = EvaluationTestCatalog.builtinCases();
        List<EvaluationCaseResultResponse> results = new ArrayList<>();
        long totalLatencyMs = 0;
        int passed = 0;

        for (EvaluationTestCaseResponse testCase : cases) {
            long start = System.currentTimeMillis();
            String actualTop1 = resolveTop1Document(kbId, testCase.getQuestion(), searchMode, topK, enableRerank);
            long latencyMs = System.currentTimeMillis() - start;
            totalLatencyMs += latencyMs;

            boolean casePassed = isPass(testCase.getExpectedDocument(), actualTop1);
            if (casePassed) {
                passed++;
            }

            results.add(EvaluationCaseResultResponse.builder()
                    .caseId(testCase.getCaseId())
                    .question(testCase.getQuestion())
                    .expectedDocument(testCase.getExpectedDocument())
                    .actualTop1Document(actualTop1 != null ? actualTop1 : "")
                    .passed(casePassed)
                    .searchMode(searchMode.name())
                    .latencyMs(latencyMs)
                    .message(buildMessage(testCase.getExpectedDocument(), actualTop1, casePassed))
                    .build());
        }

        int total = cases.size();
        int failed = total - passed;
        double passRate = total > 0 ? (double) passed / total : 0.0;
        long avgLatencyMs = total > 0 ? totalLatencyMs / total : 0L;

        return EvaluationRunResponse.builder()
                .kbId(kbId)
                .searchMode(searchMode.name())
                .enableRerank(enableRerank)
                .totalCount(total)
                .passedCount(passed)
                .failedCount(failed)
                .passRate(passRate)
                .totalLatencyMs(totalLatencyMs)
                .avgLatencyMs(avgLatencyMs)
                .results(results)
                .build();
    }

    private EvaluationModeSummaryResponse toModeSummary(EvaluationRunResponse run) {
        return EvaluationModeSummaryResponse.builder()
                .searchMode(run.getSearchMode())
                .totalCount(run.getTotalCount())
                .passedCount(run.getPassedCount())
                .failedCount(run.getFailedCount())
                .passRate(run.getPassRate())
                .totalLatencyMs(run.getTotalLatencyMs())
                .avgLatencyMs(run.getAvgLatencyMs())
                .build();
    }

    private String resolveTop1Document(
            Long kbId,
            String question,
            SearchMode searchMode,
            int topK,
            boolean enableRerank
    ) {
        List<RetrievedChunkResponse> chunks = retrieveCandidates(kbId, question, searchMode, topK);
        if (chunks.isEmpty()) {
            return null;
        }
        if (!enableRerank) {
            return chunks.get(0).getDocumentName();
        }
        RerankResult rerankResult = rerankService.rerank(question, chunks);
        if (rerankResult.getItems() == null || rerankResult.getItems().isEmpty()) {
            return null;
        }
        RetrievedChunkResponse top = rerankResult.getItems().get(0).getChunk();
        return top != null ? top.getDocumentName() : null;
    }

    private List<RetrievedChunkResponse> retrieveCandidates(
            Long kbId,
            String question,
            SearchMode searchMode,
            int topK
    ) {
        if (searchMode == SearchMode.HYBRID) {
            List<HybridSearchResult> hybridResults = hybridSearchService.search(kbId, question, topK);
            return hybridSearchService.fromHybridResults(hybridResults, false);
        }
        return switch (searchMode) {
            case BM25 -> bm25SearchService.retrieve(kbId, question, topK);
            default -> vectorRetrievalService.retrieve(kbId, question, topK);
        };
    }

    static boolean isPass(String expectedDocument, String actualTop1Document) {
        if (expectedDocument == null || expectedDocument.isBlank()) {
            return false;
        }
        if (actualTop1Document == null || actualTop1Document.isBlank()) {
            return false;
        }
        return actualTop1Document.contains(expectedDocument);
    }

    private static String buildMessage(String expected, String actual, boolean passed) {
        if (passed) {
            return "Top1 命中期望文档";
        }
        if (actual == null || actual.isBlank()) {
            return "无召回结果";
        }
        return "期望 Top1 含 " + expected + "，实际为 " + actual;
    }
}

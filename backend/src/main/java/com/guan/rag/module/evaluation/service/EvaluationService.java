package com.guan.rag.module.evaluation.service;

import com.guan.rag.module.evaluation.EvaluationTestCatalog;
import com.guan.rag.module.evaluation.request.EvaluationRunRequest;
import com.guan.rag.module.evaluation.response.EvaluationCaseResultResponse;
import com.guan.rag.module.evaluation.response.EvaluationRunResponse;
import com.guan.rag.module.evaluation.response.EvaluationTestCaseResponse;
import com.guan.rag.module.kb.service.KnowledgeBaseService;
import com.guan.rag.module.retrieval.response.RetrievedChunkResponse;
import com.guan.rag.module.retrieval.service.VectorRetrievalService;
import com.guan.rag.module.search.SearchMode;
import com.guan.rag.module.search.hybrid.HybridSearchService;
import com.guan.rag.module.search.service.Bm25SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EvaluationService {

    private final KnowledgeBaseService knowledgeBaseService;
    private final VectorRetrievalService vectorRetrievalService;
    private final Bm25SearchService bm25SearchService;
    private final HybridSearchService hybridSearchService;

    public List<EvaluationTestCaseResponse> listBuiltinCases() {
        return EvaluationTestCatalog.builtinCases();
    }

    public EvaluationRunResponse run(EvaluationRunRequest request) {
        knowledgeBaseService.requireKb(request.getKbId());
        SearchMode searchMode = SearchMode.from(request.getSearchMode());
        int topK = request.getTopK() == null ? 5 : request.getTopK();

        List<EvaluationTestCaseResponse> cases = EvaluationTestCatalog.builtinCases();
        List<EvaluationCaseResultResponse> results = new ArrayList<>();
        long totalLatencyMs = 0;
        int passed = 0;

        for (EvaluationTestCaseResponse testCase : cases) {
            long start = System.currentTimeMillis();
            String actualTop1 = resolveTop1Document(request.getKbId(), testCase.getQuestion(), searchMode, topK);
            long latencyMs = System.currentTimeMillis() - start;
            totalLatencyMs += latencyMs;

            boolean casePassed = isPass(testCase.getExpectedDocument(), actualTop1);
            if (casePassed) {
                passed++;
            }

            String message = buildMessage(testCase.getExpectedDocument(), actualTop1, casePassed);
            results.add(EvaluationCaseResultResponse.builder()
                    .caseId(testCase.getCaseId())
                    .question(testCase.getQuestion())
                    .expectedDocument(testCase.getExpectedDocument())
                    .actualTop1Document(actualTop1 != null ? actualTop1 : "")
                    .passed(casePassed)
                    .searchMode(searchMode.name())
                    .latencyMs(latencyMs)
                    .message(message)
                    .build());
        }

        int total = cases.size();
        int failed = total - passed;
        double passRate = total > 0 ? (double) passed / total : 0.0;

        return EvaluationRunResponse.builder()
                .kbId(request.getKbId())
                .searchMode(searchMode.name())
                .totalCount(total)
                .passedCount(passed)
                .failedCount(failed)
                .passRate(passRate)
                .totalLatencyMs(totalLatencyMs)
                .results(results)
                .build();
    }

    private String resolveTop1Document(Long kbId, String question, SearchMode searchMode, int topK) {
        List<RetrievedChunkResponse> chunks = retrieve(searchMode, kbId, question, topK);
        if (chunks.isEmpty()) {
            return null;
        }
        RetrievedChunkResponse top = chunks.get(0);
        return top.getDocumentName();
    }

    private List<RetrievedChunkResponse> retrieve(SearchMode searchMode, Long kbId, String question, int topK) {
        return switch (searchMode) {
            case BM25 -> bm25SearchService.retrieve(kbId, question, topK);
            case HYBRID -> hybridSearchService.retrieve(kbId, question, topK);
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

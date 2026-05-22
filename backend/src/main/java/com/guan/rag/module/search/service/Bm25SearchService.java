package com.guan.rag.module.search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.guan.rag.common.exception.BusinessException;
import com.guan.rag.config.RagProperties;
import com.guan.rag.module.kb.service.KnowledgeBaseService;
import com.guan.rag.module.retrieval.response.RetrievedChunkResponse;
import com.guan.rag.module.search.request.Bm25SearchRequest;
import com.guan.rag.module.search.response.Bm25SearchResponse;
import com.guan.rag.module.search.response.SearchResultItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class Bm25SearchService {

    private final ElasticsearchClient elasticsearchClient;
    private final KnowledgeBaseService knowledgeBaseService;
    private final RagProperties ragProperties;
    private final EsIndexService esIndexService;

    public Bm25SearchResponse search(Bm25SearchRequest request) {
        knowledgeBaseService.requireKb(request.getKbId());
        String query = request.getQuery().trim();
        int topK = request.getTopK() == null ? 5 : request.getTopK();
        List<SearchResultItemResponse> results = searchInternal(request.getKbId(), query, topK);
        return Bm25SearchResponse.builder()
                .query(query)
                .results(results)
                .build();
    }

    public List<RetrievedChunkResponse> retrieve(Long kbId, String question, int topK) {
        List<SearchResultItemResponse> items = searchInternal(kbId, question.trim(), topK);
        List<RetrievedChunkResponse> responses = new ArrayList<>();
        for (SearchResultItemResponse item : items) {
            responses.add(RetrievedChunkResponse.builder()
                    .documentId(item.getDocumentId())
                    .documentName(item.getDocumentName())
                    .chunkId(item.getChunkId())
                    .chunkIndex(item.getChunkIndex())
                    .score(item.getScore())
                    .content(item.getContent())
                    .build());
        }
        return responses;
    }

    /**
     * BM25 分数用于 V3 Context 过滤时需归一化到 [0,1]（与向量分数量纲一致）。
     */
    public List<RetrievedChunkResponse> retrieveNormalizedForFilter(Long kbId, String question, int topK) {
        List<RetrievedChunkResponse> raw = retrieve(kbId, question, topK);
        if (raw.isEmpty()) {
            return raw;
        }
        double maxScore = raw.stream()
                .mapToDouble(c -> c.getScore() != null ? c.getScore() : 0.0)
                .max()
                .orElse(1.0);
        if (maxScore <= 0) {
            maxScore = 1.0;
        }
        double divisor = maxScore;
        List<RetrievedChunkResponse> normalized = new ArrayList<>();
        for (RetrievedChunkResponse item : raw) {
            double score = item.getScore() != null ? item.getScore() : 0.0;
            normalized.add(RetrievedChunkResponse.builder()
                    .documentId(item.getDocumentId())
                    .documentName(item.getDocumentName())
                    .chunkId(item.getChunkId())
                    .chunkIndex(item.getChunkIndex())
                    .score(score / divisor)
                    .content(item.getContent())
                    .build());
        }
        return normalized;
    }

    private List<SearchResultItemResponse> searchInternal(Long kbId, String query, int topK) {
        String indexName = esIndexService.indexName();
        int limit = topK <= 0 ? 5 : topK;
        try {
            SearchRequest searchRequest = SearchRequest.of(s -> s
                    .index(indexName)
                    .size(limit)
                    .query(Query.of(q -> q.bool(b -> b
                            .must(m -> m.term(t -> t.field("kbId").value(kbId)))
                            .must(m -> m.match(ma -> ma.field("content").query(query)))
                    )))
            );

            SearchResponse<Map> response = elasticsearchClient.search(searchRequest, Map.class);
            List<SearchResultItemResponse> results = new ArrayList<>();
            for (Hit<Map> hit : response.hits().hits()) {
                Map<String, Object> source = hit.source();
                if (source == null) {
                    continue;
                }
                results.add(SearchResultItemResponse.builder()
                        .documentId(toLong(source.get("documentId")))
                        .documentName(toString(source.get("documentName")))
                        .chunkId(toLong(source.get("chunkId")))
                        .chunkIndex(toInteger(source.get("chunkIndex")))
                        .score(hit.score() != null ? hit.score() : 0.0)
                        .content(toString(source.get("content")))
                        .build());
            }
            return results;
        } catch (IOException e) {
            throw new BusinessException("BM25 检索失败，请确认 Elasticsearch 已启动且已执行索引重建: " + e.getMessage());
        }
    }

    private static Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(value.toString());
    }

    private static Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(value.toString());
    }

    private static String toString(Object value) {
        return value == null ? "" : value.toString();
    }
}

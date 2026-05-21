package com.guan.rag.module.retrieval.service;

import com.guan.rag.common.util.VectorUtils;
import com.guan.rag.module.embedding.mapper.ChunkEmbeddingMapper;
import com.guan.rag.module.embedding.provider.EmbeddingProvider;
import com.guan.rag.module.kb.service.KnowledgeBaseService;
import com.guan.rag.module.retrieval.model.VectorSearchHit;
import com.guan.rag.module.retrieval.response.RetrievedChunkResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VectorRetrievalService {

    private final KnowledgeBaseService knowledgeBaseService;
    private final ChunkEmbeddingMapper chunkEmbeddingMapper;
    private final EmbeddingProvider embeddingProvider;

    public List<RetrievedChunkResponse> retrieve(Long kbId, String question, int topK) {
        knowledgeBaseService.requireKb(kbId);
        int limit = topK <= 0 ? 5 : topK;
        float[] queryVector = embeddingProvider.embed(question);
        String vectorLiteral = VectorUtils.toPgVectorLiteral(queryVector);

        List<VectorSearchHit> hits = chunkEmbeddingMapper.searchSimilar(kbId, vectorLiteral, limit);
        return hits.stream().map(this::toResponse).toList();
    }

    private RetrievedChunkResponse toResponse(VectorSearchHit hit) {
        return RetrievedChunkResponse.builder()
                .documentId(hit.getDocumentId())
                .documentName(hit.getDocumentName())
                .chunkId(hit.getChunkId())
                .chunkIndex(hit.getChunkIndex())
                .score(hit.getScore())
                .content(hit.getContent())
                .build();
    }
}

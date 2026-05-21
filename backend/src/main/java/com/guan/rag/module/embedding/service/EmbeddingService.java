package com.guan.rag.module.embedding.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guan.rag.common.util.VectorUtils;
import com.guan.rag.module.document.entity.DocumentChunk;
import com.guan.rag.module.document.mapper.DocumentChunkMapper;
import com.guan.rag.module.embedding.entity.ChunkEmbedding;
import com.guan.rag.module.embedding.mapper.ChunkEmbeddingMapper;
import com.guan.rag.module.embedding.provider.EmbeddingProvider;
import com.guan.rag.module.embedding.response.EmbeddingRebuildResponse;
import com.guan.rag.module.embedding.response.EmbeddingStatusResponse;
import com.guan.rag.module.kb.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final KnowledgeBaseService knowledgeBaseService;
    private final DocumentChunkMapper documentChunkMapper;
    private final ChunkEmbeddingMapper chunkEmbeddingMapper;
    private final EmbeddingProvider embeddingProvider;

    @Transactional
    public EmbeddingRebuildResponse rebuild(Long kbId) {
        knowledgeBaseService.requireKb(kbId);
        List<DocumentChunk> chunks = documentChunkMapper.selectList(
                new LambdaQueryWrapper<DocumentChunk>().eq(DocumentChunk::getKbId, kbId)
        );

        String model = embeddingProvider.model();
        int dimension = embeddingProvider.dimension();
        Set<Long> chunkIds = new HashSet<>();

        for (DocumentChunk chunk : chunks) {
            chunkIds.add(chunk.getId());
            float[] vector = embeddingProvider.embed(chunk.getContent());
            String vectorLiteral = VectorUtils.toPgVectorLiteral(vector);

            ChunkEmbedding existing = chunkEmbeddingMapper.selectOne(
                    new LambdaQueryWrapper<ChunkEmbedding>().eq(ChunkEmbedding::getChunkId, chunk.getId())
            );

            if (existing != null) {
                existing.setKbId(kbId);
                existing.setDocumentId(chunk.getDocumentId());
                existing.setEmbeddingModel(model);
                existing.setEmbeddingDimension(dimension);
                existing.setEmbeddingVector(vectorLiteral);
                chunkEmbeddingMapper.updateEmbedding(existing);
            } else {
                ChunkEmbedding entity = new ChunkEmbedding();
                entity.setKbId(kbId);
                entity.setDocumentId(chunk.getDocumentId());
                entity.setChunkId(chunk.getId());
                entity.setEmbeddingModel(model);
                entity.setEmbeddingDimension(dimension);
                entity.setEmbeddingVector(vectorLiteral);
                chunkEmbeddingMapper.insertEmbedding(entity);
            }
        }

        if (!chunkIds.isEmpty()) {
            chunkEmbeddingMapper.delete(
                    new LambdaQueryWrapper<ChunkEmbedding>()
                            .eq(ChunkEmbedding::getKbId, kbId)
                            .notIn(ChunkEmbedding::getChunkId, chunkIds)
            );
        } else {
            chunkEmbeddingMapper.delete(
                    new LambdaQueryWrapper<ChunkEmbedding>().eq(ChunkEmbedding::getKbId, kbId)
            );
        }

        long embedded = chunkEmbeddingMapper.countByKbId(kbId);
        return EmbeddingRebuildResponse.builder()
                .totalChunks(chunks.size())
                .embeddedChunks(embedded)
                .embeddingModel(model)
                .embeddingDimension(dimension)
                .build();
    }

    public EmbeddingStatusResponse status(Long kbId) {
        knowledgeBaseService.requireKb(kbId);
        long totalChunks = documentChunkMapper.selectCount(
                new LambdaQueryWrapper<DocumentChunk>().eq(DocumentChunk::getKbId, kbId)
        );
        long embeddedChunks = chunkEmbeddingMapper.countByKbId(kbId);
        return EmbeddingStatusResponse.builder()
                .totalChunks(totalChunks)
                .embeddedChunks(embeddedChunks)
                .notEmbeddedChunks(Math.max(totalChunks - embeddedChunks, 0))
                .build();
    }

    public long countEmbeddedChunks() {
        return chunkEmbeddingMapper.selectCount(null);
    }
}

package com.guan.rag.module.embedding.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guan.rag.common.util.VectorUtils;
import com.guan.rag.module.document.entity.Document;
import com.guan.rag.module.document.entity.DocumentChunk;
import com.guan.rag.module.document.mapper.DocumentChunkMapper;
import com.guan.rag.module.document.mapper.DocumentMapper;
import com.guan.rag.module.embedding.entity.ChunkEmbedding;
import com.guan.rag.module.embedding.mapper.ChunkEmbeddingMapper;
import com.guan.rag.module.embedding.provider.EmbeddingProvider;
import com.guan.rag.module.embedding.response.EmbeddingRebuildResponse;
import com.guan.rag.module.embedding.response.EmbeddingStatusResponse;
import com.guan.rag.module.kb.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final KnowledgeBaseService knowledgeBaseService;
    private final DocumentChunkMapper documentChunkMapper;
    private final DocumentMapper documentMapper;
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

        // 清空旧向量，避免维度或算法变更后残留脏数据
        chunkEmbeddingMapper.delete(
                new LambdaQueryWrapper<ChunkEmbedding>().eq(ChunkEmbedding::getKbId, kbId)
        );

        for (DocumentChunk chunk : chunks) {
            float[] vector = embeddingProvider.embed(textForEmbedding(chunk));
            if (vector.length != dimension) {
                throw new IllegalStateException(
                        "Embedding 维度不一致: 期望 " + dimension + ", 实际 " + vector.length);
            }
            ChunkEmbedding entity = new ChunkEmbedding();
            entity.setKbId(kbId);
            entity.setDocumentId(chunk.getDocumentId());
            entity.setChunkId(chunk.getId());
            entity.setEmbeddingModel(model);
            entity.setEmbeddingDimension(dimension);
            entity.setEmbeddingVector(VectorUtils.toPgVectorLiteral(vector));
            chunkEmbeddingMapper.insertEmbedding(entity);
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
        int expectedDimension = embeddingProvider.dimension();
        long totalChunks = documentChunkMapper.selectCount(
                new LambdaQueryWrapper<DocumentChunk>().eq(DocumentChunk::getKbId, kbId)
        );
        long embeddedChunks = chunkEmbeddingMapper.selectCount(
                new LambdaQueryWrapper<ChunkEmbedding>()
                        .eq(ChunkEmbedding::getKbId, kbId)
                        .eq(ChunkEmbedding::getEmbeddingDimension, expectedDimension)
                        .eq(ChunkEmbedding::getEmbeddingModel, embeddingProvider.model())
        );
        return EmbeddingStatusResponse.builder()
                .totalChunks(totalChunks)
                .embeddedChunks(embeddedChunks)
                .notEmbeddedChunks(Math.max(totalChunks - embeddedChunks, 0))
                .build();
    }

    /**
     * 向量化时强化 Markdown 标题行，提升「排查 / API / 规范」类问题的召回稳定性。
     */
    private String textForEmbedding(DocumentChunk chunk) {
        String content = chunk.getContent();
        if (content == null || content.isBlank()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        Document document = documentMapper.selectById(chunk.getDocumentId());
        if (document != null && document.getFileName() != null) {
            sb.append(document.getFileName()).append('\n');
        }
        int newline = content.indexOf('\n');
        if (newline > 0) {
            String titleLine = content.substring(0, newline).trim();
            if (!titleLine.isEmpty()) {
                sb.append(titleLine).append('\n').append(titleLine).append('\n');
            }
        }
        sb.append(content);
        return sb.toString();
    }

    public long countEmbeddedChunks() {
        return chunkEmbeddingMapper.selectCount(
                new LambdaQueryWrapper<ChunkEmbedding>()
                        .eq(ChunkEmbedding::getEmbeddingDimension, embeddingProvider.dimension())
        );
    }
}

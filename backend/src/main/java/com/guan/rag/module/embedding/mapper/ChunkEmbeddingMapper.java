package com.guan.rag.module.embedding.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guan.rag.module.embedding.entity.ChunkEmbedding;
import com.guan.rag.module.retrieval.model.VectorSearchHit;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ChunkEmbeddingMapper extends BaseMapper<ChunkEmbedding> {

    @Select("SELECT COUNT(*) FROM chunk_embedding WHERE kb_id = #{kbId}")
    long countByKbId(@Param("kbId") Long kbId);

    @Insert("""
            INSERT INTO chunk_embedding
            (kb_id, document_id, chunk_id, embedding_model, embedding_dimension, embedding, created_at, updated_at)
            VALUES
            (#{kbId}, #{documentId}, #{chunkId}, #{embeddingModel}, #{embeddingDimension},
             CAST(#{embeddingVector} AS vector), NOW(), NOW())
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertEmbedding(ChunkEmbedding entity);

    @Update("""
            UPDATE chunk_embedding
            SET embedding_model = #{embeddingModel},
                embedding_dimension = #{embeddingDimension},
                embedding = CAST(#{embeddingVector} AS vector),
                updated_at = NOW()
            WHERE id = #{id}
            """)
    int updateEmbedding(ChunkEmbedding entity);

    @Select("""
            SELECT ce.chunk_id AS chunkId,
                   ce.document_id AS documentId,
                   dc.chunk_index AS chunkIndex,
                   d.file_name AS documentName,
                   dc.content AS content,
                   (1 - (ce.embedding <=> CAST(#{queryVector} AS vector))) AS score
            FROM chunk_embedding ce
            INNER JOIN document_chunk dc ON ce.chunk_id = dc.id AND dc.deleted = 0
            INNER JOIN document d ON ce.document_id = d.id AND d.deleted = 0
            WHERE ce.kb_id = #{kbId}
            ORDER BY ce.embedding <=> CAST(#{queryVector} AS vector)
            LIMIT #{topK}
            """)
    List<VectorSearchHit> searchSimilar(
            @Param("kbId") Long kbId,
            @Param("queryVector") String queryVector,
            @Param("topK") int topK);
}

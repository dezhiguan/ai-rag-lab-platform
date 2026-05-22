package com.guan.rag.module.embedding.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guan.rag.common.exception.BusinessException;
import com.guan.rag.module.embedding.entity.ChunkEmbedding;
import com.guan.rag.module.embedding.mapper.ChunkEmbeddingMapper;
import com.guan.rag.module.embedding.provider.EmbeddingProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmbeddingConsistencyService {

    private final ChunkEmbeddingMapper chunkEmbeddingMapper;
    private final EmbeddingProvider embeddingProvider;

    /**
     * 校验知识库已向量化数据与当前 Embedding 配置（model + dimension）一致。
     */
    public void ensureKbVectorsMatchCurrentConfig(Long kbId) {
        int expectedDimension = embeddingProvider.dimension();
        String expectedModel = embeddingProvider.model();

        long mismatchCount = chunkEmbeddingMapper.selectCount(
                new LambdaQueryWrapper<ChunkEmbedding>()
                        .eq(ChunkEmbedding::getKbId, kbId)
                        .and(w -> w.ne(ChunkEmbedding::getEmbeddingDimension, expectedDimension)
                                .or()
                                .ne(ChunkEmbedding::getEmbeddingModel, expectedModel))
        );
        if (mismatchCount > 0) {
            throw new BusinessException(
                    "知识库向量与当前 Embedding 配置不一致（provider 配置 model="
                            + expectedModel + ", dimension=" + expectedDimension
                            + "）。请执行 POST /api/kb/" + kbId + "/embedding/rebuild 重新向量化。");
        }
    }
}

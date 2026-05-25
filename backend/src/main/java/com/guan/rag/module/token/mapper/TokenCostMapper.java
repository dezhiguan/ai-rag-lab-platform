package com.guan.rag.module.token.mapper;

import com.guan.rag.module.token.model.EmbeddingCostStatsRow;
import com.guan.rag.module.token.model.TokenCostStatsRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TokenCostMapper {

    @Select("""
            SELECT
                COUNT(*)::bigint AS queryCount,
                COALESCE(SUM(total_tokens), 0)::bigint AS totalTokens,
                COALESCE(SUM(COALESCE(total_cost, estimated_cost, 0)), 0) AS totalCost,
                COALESCE(AVG(total_tokens), 0) AS avgTokens,
                COALESCE(AVG(COALESCE(total_cost, estimated_cost, 0)), 0) AS avgCost
            FROM rag_query_log
            WHERE deleted = 0
              AND total_tokens IS NOT NULL
              AND total_tokens > 0
            """)
    TokenCostStatsRow aggregateAll();

    @Select("""
            SELECT
                COUNT(*)::bigint AS queryCount,
                COALESCE(SUM(total_tokens), 0)::bigint AS totalTokens,
                COALESCE(SUM(COALESCE(total_cost, estimated_cost, 0)), 0) AS totalCost,
                COALESCE(AVG(total_tokens), 0) AS avgTokens,
                COALESCE(AVG(COALESCE(total_cost, estimated_cost, 0)), 0) AS avgCost
            FROM rag_query_log
            WHERE deleted = 0
              AND total_tokens IS NOT NULL
              AND total_tokens > 0
              AND created_at >= NOW() - INTERVAL '7 days'
            """)
    TokenCostStatsRow aggregateRecent();

    @Select("""
            SELECT
                COUNT(*)::bigint AS queryCount,
                COALESCE(SUM(COALESCE(embedding_tokens, 0)), 0)::bigint AS embeddingTokens,
                COALESCE(SUM(COALESCE(embedding_cost, 0)), 0) AS embeddingCost,
                COALESCE(AVG(COALESCE(embedding_tokens, 0)), 0) AS avgEmbeddingTokens,
                COALESCE(AVG(COALESCE(embedding_cost, 0)), 0) AS avgEmbeddingCost
            FROM rag_query_log
            WHERE deleted = 0
              AND total_tokens IS NOT NULL
              AND total_tokens > 0
              AND LOWER(COALESCE(embedding_provider, '')) = 'qwen'
            """)
    EmbeddingCostStatsRow aggregateEmbeddingAll();

    @Select("""
            SELECT
                COUNT(*)::bigint AS queryCount,
                COALESCE(SUM(COALESCE(embedding_tokens, 0)), 0)::bigint AS embeddingTokens,
                COALESCE(SUM(COALESCE(embedding_cost, 0)), 0) AS embeddingCost,
                COALESCE(AVG(COALESCE(embedding_tokens, 0)), 0) AS avgEmbeddingTokens,
                COALESCE(AVG(COALESCE(embedding_cost, 0)), 0) AS avgEmbeddingCost
            FROM rag_query_log
            WHERE deleted = 0
              AND total_tokens IS NOT NULL
              AND total_tokens > 0
              AND LOWER(COALESCE(embedding_provider, '')) = 'qwen'
              AND created_at >= NOW() - INTERVAL '7 days'
            """)
    EmbeddingCostStatsRow aggregateEmbeddingRecent();
}

package com.guan.rag.module.token.mapper;

import com.guan.rag.module.token.model.TokenCostStatsRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TokenCostMapper {

    @Select("""
            SELECT
                COUNT(*)::bigint AS queryCount,
                COALESCE(SUM(total_tokens), 0)::bigint AS totalTokens,
                COALESCE(SUM(estimated_cost), 0) AS totalCost,
                COALESCE(AVG(total_tokens), 0) AS avgTokens,
                COALESCE(AVG(estimated_cost), 0) AS avgCost
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
                COALESCE(SUM(estimated_cost), 0) AS totalCost,
                COALESCE(AVG(total_tokens), 0) AS avgTokens,
                COALESCE(AVG(estimated_cost), 0) AS avgCost
            FROM rag_query_log
            WHERE deleted = 0
              AND total_tokens IS NOT NULL
              AND total_tokens > 0
              AND created_at >= NOW() - INTERVAL '7 days'
            """)
    TokenCostStatsRow aggregateRecent();
}

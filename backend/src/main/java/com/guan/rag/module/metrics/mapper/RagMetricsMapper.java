package com.guan.rag.module.metrics.mapper;

import com.guan.rag.module.metrics.model.RagMetricsCountRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RagMetricsMapper {

    @Select("SELECT COUNT(*) FROM rag_query_log WHERE deleted = 0")
    long countTotalQueries();

    @Select("SELECT COUNT(*) FROM rag_query_log WHERE deleted = 0 AND created_at >= CURRENT_DATE")
    long countTodayQueries();

    @Select("""
            SELECT COALESCE(AVG(total_time_ms), 0)
            FROM rag_query_log
            WHERE deleted = 0 AND total_time_ms IS NOT NULL
            """)
    double avgTotalTimeMs();

    @Select("""
            SELECT COALESCE(AVG(retrieval_time_ms), 0)
            FROM rag_query_log
            WHERE deleted = 0 AND retrieval_time_ms IS NOT NULL
            """)
    double avgRetrievalTimeMs();

    @Select("""
            SELECT COALESCE(AVG(generation_time_ms), 0)
            FROM rag_query_log
            WHERE deleted = 0 AND generation_time_ms IS NOT NULL
            """)
    double avgGenerationTimeMs();

    @Select("""
            SELECT search_mode AS label, COUNT(*) AS count
            FROM rag_query_log
            WHERE deleted = 0
            GROUP BY search_mode
            """)
    List<RagMetricsCountRow> countBySearchMode();

    @Select("""
            SELECT enable_rerank AS label, COUNT(*) AS count
            FROM rag_query_log
            WHERE deleted = 0
            GROUP BY enable_rerank
            """)
    List<RagMetricsCountRow> countByEnableRerank();
}

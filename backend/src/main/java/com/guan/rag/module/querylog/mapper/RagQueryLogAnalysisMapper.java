package com.guan.rag.module.querylog.mapper;

import com.guan.rag.module.querylog.model.QueryLogChunkCountRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RagQueryLogAnalysisMapper {

    @Select("""
            <script>
            SELECT query_log_id AS queryLogId, COUNT(*) AS chunkCount
            FROM rag_retrieval_log
            WHERE query_log_id IN
            <foreach collection="queryLogIds" item="id" open="(" separator="," close=")">
              #{id}
            </foreach>
            GROUP BY query_log_id
            </script>
            """)
    List<QueryLogChunkCountRow> countRetrievalChunksByQueryLogIds(@Param("queryLogIds") List<Long> queryLogIds);

    @Select("""
            SELECT COUNT(*)
            FROM rag_query_log
            WHERE deleted = 0
              AND (
                total_time_ms >= 3000
                OR retrieval_time_ms >= 1000
                OR generation_time_ms >= 2000
              )
            """)
    long countSlowQueries();
}

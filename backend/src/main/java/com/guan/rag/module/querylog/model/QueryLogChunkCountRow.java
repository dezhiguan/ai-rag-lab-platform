package com.guan.rag.module.querylog.model;

import lombok.Data;

@Data
public class QueryLogChunkCountRow {

    private Long queryLogId;

    private Long chunkCount;
}

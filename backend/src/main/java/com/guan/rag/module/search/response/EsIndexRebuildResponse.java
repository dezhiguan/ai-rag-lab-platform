package com.guan.rag.module.search.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EsIndexRebuildResponse {

    private String indexName;
    private int syncedCount;
}

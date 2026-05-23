package com.guan.rag.module.querylog.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RagQueryLogPageResponse {

    private long total;
    private int page;
    private int pageSize;
    private List<RagQueryLogItemResponse> records;
}

package com.guan.rag.module.retrieval.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RetrievalTestResponse {

    private String query;
    private List<RetrievedChunkResponse> results;
}

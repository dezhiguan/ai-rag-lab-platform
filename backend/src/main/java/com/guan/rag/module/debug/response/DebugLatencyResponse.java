package com.guan.rag.module.debug.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DebugLatencyResponse {

    private Long retrievalTimeMs;
    private Long generationTimeMs;
    private Long totalTimeMs;
}

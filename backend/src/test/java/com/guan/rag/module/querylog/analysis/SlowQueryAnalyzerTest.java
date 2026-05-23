package com.guan.rag.module.querylog.analysis;

import com.guan.rag.module.debug.entity.DebugQueryLog;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlowQueryAnalyzerTest {

    @Test
    void isSlow_whenAnyThresholdExceeded() {
        DebugQueryLog log = new DebugQueryLog();
        log.setTotalTimeMs(2500L);
        log.setRetrievalTimeMs(500L);
        log.setGenerationTimeMs(1500L);
        assertFalse(SlowQueryAnalyzer.isSlow(log));

        log.setTotalTimeMs(3000L);
        assertTrue(SlowQueryAnalyzer.isSlow(log));

        log.setTotalTimeMs(1000L);
        log.setRetrievalTimeMs(1000L);
        assertTrue(SlowQueryAnalyzer.isSlow(log));

        log.setRetrievalTimeMs(500L);
        log.setGenerationTimeMs(2000L);
        assertTrue(SlowQueryAnalyzer.isSlow(log));
    }

    @Test
    void analyze_generatesReasonsAndSuggestions() {
        DebugQueryLog log = new DebugQueryLog();
        log.setSearchMode("HYBRID");
        log.setEnableRerank(1);
        log.setRetrievalTimeMs(1200L);
        log.setGenerationTimeMs(2500L);
        log.setTotalTimeMs(4000L);

        SlowQueryAnalyzer.AnalysisResult result = SlowQueryAnalyzer.analyze(log, 8);

        assertTrue(result.getSlowReasons().contains("检索耗时高"));
        assertTrue(result.getSlowReasons().contains("生成耗时高"));
        assertTrue(result.getSlowReasons().contains("总耗时高"));
        assertTrue(result.getSlowReasons().contains("启用重排导致耗时增加"));
        assertTrue(result.getSuggestions().stream().anyMatch(s -> s.contains("topK")));
        assertTrue(result.getSuggestions().stream().anyMatch(s -> s.contains("Provider")));
    }
}

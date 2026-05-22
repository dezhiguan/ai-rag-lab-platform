package com.guan.rag.module.chat.support;

import com.guan.rag.config.RagProperties;
import com.guan.rag.module.retrieval.response.RetrievedChunkResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContextChunkFilterTest {

    private ContextChunkFilter filter;

    @BeforeEach
    void setUp() {
        RagProperties props = new RagProperties();
        props.getContext().setMaxChunks(2);
        props.getContext().setMinScore(0.45);
        props.getContext().setMaxScoreGap(0.35);
        filter = new ContextChunkFilter(props);
    }

    @Test
    void filtersWeakChunksForSmsTroubleshootingScenario() {
        List<RetrievedChunkResponse> retrieved = List.of(
                chunk(23L, "03-troubleshooting.md", 0.8230),
                chunk(22L, "02-api-spec.md", 0.6193),
                chunk(21L, "01-project-guideline.md", 0.2701)
        );

        ContextFilterResult result = filter.filter(retrieved);

        assertEquals(2, result.getContextChunks().size());
        assertEquals(23L, result.getContextChunks().get(0).getChunkId());
        assertEquals(22L, result.getContextChunks().get(1).getChunkId());

        assertTrue(result.getDecisions().get(23L).isUsedInPrompt());
        assertTrue(result.getDecisions().get(22L).isUsedInPrompt());
        assertFalse(result.getDecisions().get(21L).isUsedInPrompt());
        assertEquals(ContextFilterReason.SCORE_TOO_LOW, result.getDecisions().get(21L).getFilterReason());
    }

    @Test
    void keepsTop1WhenAllFiltered() {
        List<RetrievedChunkResponse> retrieved = List.of(
                chunk(1L, "a.md", 0.1),
                chunk(2L, "b.md", 0.05)
        );

        ContextFilterResult result = filter.filter(retrieved);

        assertEquals(1, result.getContextChunks().size());
        assertEquals(1L, result.getContextChunks().get(0).getChunkId());
    }

    private static RetrievedChunkResponse chunk(Long id, String name, double score) {
        return RetrievedChunkResponse.builder()
                .chunkId(id)
                .documentId(id)
                .documentName(name)
                .chunkIndex(0)
                .score(score)
                .content("content-" + name)
                .build();
    }
}

package com.guan.rag.module.evaluation.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EvaluationServiceTest {

    @Test
    void isPass_whenTop1ContainsExpected() {
        assertTrue(EvaluationService.isPass("02-api-spec.md", "02-api-spec.md"));
        assertTrue(EvaluationService.isPass("02-api-spec.md", "kb/02-api-spec.md"));
    }

    @Test
    void isPass_whenMissingOrMismatch() {
        assertFalse(EvaluationService.isPass("02-api-spec.md", "03-troubleshooting.md"));
        assertFalse(EvaluationService.isPass("02-api-spec.md", null));
        assertFalse(EvaluationService.isPass("02-api-spec.md", ""));
    }
}

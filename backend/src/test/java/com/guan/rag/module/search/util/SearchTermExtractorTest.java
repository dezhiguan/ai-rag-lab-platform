package com.guan.rag.module.search.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchTermExtractorTest {

    @Test
    void extractFromContent_apiSpecChunk() {
        String content = """
                ## POST /api/sms/send-code
                ## POST /api/sms/verify-code
                | SMS_429 | 发送过于频繁 |
                | SMS_400 | 手机号格式错误 |
                """;
        List<String> terms = SearchTermExtractor.extractFromContent(content);
        assertTrue(terms.contains("SMS_429"));
        assertTrue(terms.contains("SMS_400"));
        assertTrue(terms.contains("/api/sms/send-code"));
        assertTrue(terms.contains("/api/sms/verify-code"));
        assertTrue(terms.contains("send-code"));
        assertTrue(terms.contains("verify-code"));
    }

    @Test
    void extractFromQuery_examples() {
        assertEquals(List.of("SMS_429"), SearchTermExtractor.extractFromQuery("SMS_429 是什么意思？"));
        assertEquals(List.of("send-code"), SearchTermExtractor.extractFromQuery("send-code 接口路径是什么？"));
        List<String> pathQuery = SearchTermExtractor.extractFromQuery("/api/sms/send-code 是什么接口？");
        assertTrue(pathQuery.contains("/api/sms/send-code"));
        assertTrue(pathQuery.contains("send-code"));
    }

    @Test
    void matchedTerms_findsSubstringsInContent() {
        List<String> extracted = List.of("SMS_429", "send-code");
        String content = "| SMS_429 | 发送过于频繁 | POST /api/sms/send-code";
        List<String> matched = SearchTermExtractor.matchedTerms(extracted, content);
        assertTrue(matched.contains("SMS_429"));
        assertTrue(matched.contains("send-code"));
    }
}

package com.guan.rag.module.embedding;

import com.guan.rag.config.RagProperties;
import com.guan.rag.module.embedding.provider.MockEmbeddingProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * V2 验收：Mock Embedding 对样例文档的 Top1 排序是否符合预期。
 */
class V2RetrievalRankingTest {

    private MockEmbeddingProvider provider;

    @BeforeEach
    void setUp() {
        RagProperties properties = new RagProperties();
        properties.getEmbedding().setDimension(384);
        provider = new MockEmbeddingProvider(properties);
    }

    @Test
    void acceptanceQueriesRankExpectedDocument() throws IOException {
        String guideline = readSample("01-project-guideline.md");
        String apiSpec = readSample("02-api-spec.md");
        String troubleshooting = readSample("03-troubleshooting.md");

        assertTop1("短信验证码发不出去怎么排查？", Map.of(
                "01-project-guideline.md", embedChunk("01-project-guideline.md", guideline),
                "02-api-spec.md", embedChunk("02-api-spec.md", apiSpec),
                "03-troubleshooting.md", embedChunk("03-troubleshooting.md", troubleshooting)
        ), "03-troubleshooting.md");

        assertTop1("SMS_429 是什么意思？", Map.of(
                "01-project-guideline.md", embedChunk("01-project-guideline.md", guideline),
                "02-api-spec.md", embedChunk("02-api-spec.md", apiSpec),
                "03-troubleshooting.md", embedChunk("03-troubleshooting.md", troubleshooting)
        ), "02-api-spec.md");

        assertTop1("send-code 接口路径是什么？", Map.of(
                "01-project-guideline.md", embedChunk("01-project-guideline.md", guideline),
                "02-api-spec.md", embedChunk("02-api-spec.md", apiSpec),
                "03-troubleshooting.md", embedChunk("03-troubleshooting.md", troubleshooting)
        ), "02-api-spec.md");

        assertTop1("这个项目为什么后续会使用 PgVector？", Map.of(
                "01-project-guideline.md", embedChunk("01-project-guideline.md", guideline),
                "02-api-spec.md", embedChunk("02-api-spec.md", apiSpec),
                "03-troubleshooting.md", embedChunk("03-troubleshooting.md", troubleshooting)
        ), "01-project-guideline.md");
    }

    private void assertTop1(String query, Map<String, float[]> docVectors, String expectedDoc) {
        float[] queryVector = provider.embed(query);
        String topDoc = docVectors.entrySet().stream()
                .max(Comparator.comparingDouble(e -> cosine(queryVector, e.getValue())))
                .map(Map.Entry::getKey)
                .orElseThrow();
        assertEquals(expectedDoc, topDoc, "query=" + query);
    }

    private float[] embedChunk(String fileName, String content) {
        StringBuilder sb = new StringBuilder(fileName).append('\n');
        int newline = content.indexOf('\n');
        if (newline > 0) {
            String title = content.substring(0, newline).trim();
            if (!title.isEmpty()) {
                sb.append(title).append('\n').append(title).append('\n');
            }
        }
        sb.append(content);
        return provider.embed(sb.toString());
    }

    private String readSample(String name) throws IOException {
        ClassPathResource resource = new ClassPathResource("sample-data/" + name);
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    private static float cosine(float[] a, float[] b) {
        double dot = 0;
        double normA = 0;
        double normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) {
            return 0;
        }
        return (float) (dot / (Math.sqrt(normA) * Math.sqrt(normB)));
    }
}

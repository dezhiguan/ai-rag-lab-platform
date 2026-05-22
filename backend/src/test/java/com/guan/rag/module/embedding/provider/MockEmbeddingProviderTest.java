package com.guan.rag.module.embedding.provider;

import com.guan.rag.config.RagProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MockEmbeddingProviderTest {

    private MockEmbeddingProvider provider;

    @BeforeEach
    void setUp() {
        RagProperties properties = new RagProperties();
        properties.getEmbedding().setDimension(384);
        provider = new MockEmbeddingProvider(properties);
    }

    @Test
    void sameTextProducesIdenticalVector() {
        String text = "短信验证码发不出去怎么排查？";
        float[] first = provider.embed(text);
        float[] second = provider.embed(text);
        assertArrayEquals(first, second, 1e-6f);
    }

    @Test
    void relatedTextsHaveHigherSimilarityThanUnrelated() {
        String query = "短信验证码发不出去怎么排查？";
        String troubleshooting = """
                当用户反馈「收不到验证码」时，按以下步骤排查：
                检查手机号格式，确认未触发 SMS_429，检查 Redis 缓存。
                """;
        String unrelated = "这个项目为什么后续会使用 PgVector 存储向量。";

        float[] q = provider.embed(query);
        float[] related = provider.embed(troubleshooting);
        float[] unrelatedVec = provider.embed(unrelated);

        assertTrue(cosine(q, related) > cosine(q, unrelatedVec),
                "query 应与排查文档更相似, related=" + cosine(q, related)
                        + ", unrelated=" + cosine(q, unrelatedVec));
    }

    @Test
    void sendCodeQueryMatchesApiPathContent() {
        String query = "send-code 接口路径是什么？";
        String apiSpec = "POST /api/sms/send-code 发送短信验证码";
        String unrelated = "检查 Redis 缓存与短信账户余额";

        float[] q = provider.embed(query);
        assertTrue(cosine(q, provider.embed(apiSpec)) > cosine(q, provider.embed(unrelated)));
    }

    @Test
    void sms429QuerySharesNgramsWithApiSpecTable() {
        String query = "SMS_429 是什么意思？";
        String apiSpec = "| SMS_429 | 发送过于频繁 |";
        String unrelated = "PgVector 用于存储向量 embedding 维度";

        float[] q = provider.embed(query);
        assertTrue(cosine(q, provider.embed(apiSpec)) > cosine(q, provider.embed(unrelated)));
    }

    @Test
    void dimensionIs384() {
        assertEquals(384, provider.embed("test").length);
        assertEquals(384, provider.dimension());
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

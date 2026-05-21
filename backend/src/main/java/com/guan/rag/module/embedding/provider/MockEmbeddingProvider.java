package com.guan.rag.module.embedding.provider;

import com.guan.rag.config.RagProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Random;

@Component
@ConditionalOnProperty(prefix = "rag.embedding", name = "provider", havingValue = "mock", matchIfMissing = true)
@RequiredArgsConstructor
public class MockEmbeddingProvider implements EmbeddingProvider {

    private final RagProperties ragProperties;

    @Override
    public float[] embed(String text) {
        int dimension = dimension();
        float[] vector = new float[dimension];
        long seed = stableSeed(text == null ? "" : text);
        Random random = new Random(seed);
        for (int i = 0; i < dimension; i++) {
            vector[i] = random.nextFloat() * 2 - 1;
        }
        normalize(vector);
        return vector;
    }

    @Override
    public String model() {
        return ragProperties.getEmbedding().getModel();
    }

    @Override
    public int dimension() {
        return ragProperties.getEmbedding().getDimension();
    }

    private long stableSeed(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            long seed = 0;
            for (int i = 0; i < 8; i++) {
                seed = (seed << 8) | (hash[i] & 0xffL);
            }
            return seed;
        } catch (Exception e) {
            return text.hashCode();
        }
    }

    private void normalize(float[] vector) {
        double sum = 0;
        for (float v : vector) {
            sum += v * v;
        }
        if (sum == 0) {
            return;
        }
        float norm = (float) Math.sqrt(sum);
        for (int i = 0; i < vector.length; i++) {
            vector[i] /= norm;
        }
    }
}

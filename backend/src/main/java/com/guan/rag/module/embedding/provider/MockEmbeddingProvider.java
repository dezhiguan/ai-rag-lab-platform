package com.guan.rag.module.embedding.provider;

import com.guan.rag.config.RagProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 确定性字符 n-gram 哈希向量（Mock）。
 * 同文本向量一致；共享 n-gram 的文本具有更高余弦相似度；支持中文。
 */
@Component
@ConditionalOnProperty(prefix = "rag.embedding", name = "provider", havingValue = "mock", matchIfMissing = true)
@RequiredArgsConstructor
public class MockEmbeddingProvider implements EmbeddingProvider {

    private static final int BIGRAM_SIZE = 2;
    private static final int TRIGRAM_SIZE = 3;
    private static final float TRIGRAM_WEIGHT = 1.2f;

    private final RagProperties ragProperties;

    @Override
    public float[] embed(String text) {
        int dimension = dimension();
        float[] vector = new float[dimension];
        String normalized = normalize(text);
        if (normalized.isEmpty()) {
            return vector;
        }

        accumulateNgrams(vector, normalized, BIGRAM_SIZE, 1.0f);
        accumulateNgrams(vector, normalized, TRIGRAM_SIZE, TRIGRAM_WEIGHT);
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

    private void accumulateNgrams(float[] vector, String text, int n, float weight) {
        if (text.length() < n) {
            for (int i = 0; i < text.length(); i++) {
                addFeature(vector, String.valueOf(text.charAt(i)), weight * 0.8f);
            }
            return;
        }
        for (int i = 0; i <= text.length() - n; i++) {
            addFeature(vector, text.substring(i, i + n), weight);
        }
    }

    /**
     * 特征哈希到固定维度，使用双哈希降低碰撞。
     */
    private void addFeature(float[] vector, String feature, float weight) {
        int h1 = stableHash(feature, 0);
        int h2 = stableHash(feature, 1);
        int index1 = Math.floorMod(h1, vector.length);
        int index2 = Math.floorMod(h2, vector.length);
        float sign1 = Math.floorMod(h1, 2) == 0 ? 1.0f : -1.0f;
        float sign2 = Math.floorMod(h2, 2) == 0 ? 1.0f : -1.0f;
        vector[index1] += sign1 * weight;
        vector[index2] += sign2 * weight * 0.5f;
    }

    private int stableHash(String feature, int seed) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update((seed + ":" + feature).getBytes(StandardCharsets.UTF_8));
            byte[] hash = digest.digest();
            int value = 0;
            for (int i = 0; i < 4; i++) {
                value = (value << 8) | (hash[i] & 0xff);
            }
            return value;
        } catch (NoSuchAlgorithmException e) {
            return feature.hashCode() ^ (seed * 31);
        }
    }

    /**
     * 归一化：小写 ASCII、保留中文等字符、合并空白。
     */
    private String normalize(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(text.length());
        boolean lastWasSpace = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isWhitespace(c)) {
                if (!lastWasSpace && !sb.isEmpty()) {
                    sb.append(' ');
                    lastWasSpace = true;
                }
            } else {
                if (c < 128) {
                    sb.append(Character.toLowerCase(c));
                } else {
                    sb.append(c);
                }
                lastWasSpace = false;
            }
        }
        return sb.toString().trim();
    }

    private void normalize(float[] vector) {
        double sum = 0;
        for (float v : vector) {
            sum += (double) v * v;
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

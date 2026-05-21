package com.guan.rag.module.embedding.provider;

public interface EmbeddingProvider {

    float[] embed(String text);

    String model();

    int dimension();
}

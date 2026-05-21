package com.guan.rag.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "rag")
public class RagProperties {

    private Storage storage = new Storage();
    private Document document = new Document();
    private Embedding embedding = new Embedding();
    private Chat chat = new Chat();

    @Data
    public static class Storage {
        private String path = "./data/uploads";
    }

    @Data
    public static class Document {
        private int chunkSize = 800;
        private int chunkOverlap = 100;
    }

    @Data
    public static class Embedding {
        private String provider = "mock";
        private String model = "mock-embedding";
        private int dimension = 384;
        private String apiKey = "";
        private String baseUrl = "https://api.openai.com/v1";
    }

    @Data
    public static class Chat {
        private String provider = "mock";
        private String model = "mock-chat";
        private String apiKey = "";
        private String baseUrl = "https://api.openai.com/v1";
    }
}

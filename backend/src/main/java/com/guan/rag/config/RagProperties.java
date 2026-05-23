package com.guan.rag.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "rag")
public class RagProperties {

    private App app = new App();
    private Storage storage = new Storage();
    private Document document = new Document();
    private Embedding embedding = new Embedding();
    private Chat chat = new Chat();
    private Context context = new Context();
    private Elasticsearch elasticsearch = new Elasticsearch();

    @Data
    public static class App {
        private String name = "ai-rag-lab-platform";
        private String version = "V8";
    }

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

    @Data
    public static class Context {
        /** 进入 Prompt 的最大 Chunk 数 */
        private int maxChunks = 2;
        /** 低于该分数不进入 Prompt */
        private double minScore = 0.45;
        /** 与 Top1 分数差距超过该值不进入 Prompt */
        private double maxScoreGap = 0.35;
    }

    @Data
    public static class Elasticsearch {
        private String hosts = "http://localhost:9200";
        private String username = "";
        private String password = "";
        private String index = "rag_document_chunk";
    }
}

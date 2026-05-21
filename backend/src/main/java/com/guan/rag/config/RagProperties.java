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

    @Data
    public static class Storage {
        private String path = "./data/uploads";
    }

    @Data
    public static class Document {
        private int chunkSize = 800;
        private int chunkOverlap = 100;
    }
}

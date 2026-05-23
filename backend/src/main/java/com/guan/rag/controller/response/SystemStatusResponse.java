package com.guan.rag.controller.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SystemStatusResponse {

    private BackendStatusResponse backend;
    private DependencyStatusResponse postgresql;
    private ElasticsearchStatusResponse elasticsearch;
    private ModelProviderStatusResponse modelProvider;
    private List<FeatureStatusResponse> features;

    @Data
    @Builder
    public static class BackendStatusResponse {
        private String status;
        private String appName;
        private String version;
        private String serverTime;
        private String message;
    }

    @Data
    @Builder
    public static class DependencyStatusResponse {
        private String name;
        private String status;
        private String message;
    }

    @Data
    @Builder
    public static class ElasticsearchStatusResponse {
        private String status;
        private String indexName;
        private String message;
    }

    @Data
    @Builder
    public static class ModelProviderStatusResponse {
        private String embeddingProvider;
        private String embeddingModel;
        private String chatProvider;
        private String chatModel;
    }

    @Data
    @Builder
    public static class FeatureStatusResponse {
        private String key;
        private String label;
        private String status;
    }
}

package com.guan.rag.common.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guan.rag.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI-compatible HTTP 客户端（DashScope Embedding / DeepSeek Chat）。
 */
@Component
@RequiredArgsConstructor
public class OpenAiCompatibleClient {

    private final ObjectMapper objectMapper;

    public float[] createEmbedding(String baseUrl, String apiKey, String model, int dimension, String input) {
        requireApiKey(apiKey, "Embedding");
        String url = joinPath(baseUrl, "/embeddings");
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("input", input);
        if (dimension > 0) {
            body.put("dimensions", dimension);
        }

        try {
            String responseBody = restClient(apiKey)
                    .post()
                    .uri(url)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return parseEmbeddingResponse(responseBody, dimension);
        } catch (RestClientResponseException e) {
            throw new BusinessException("Embedding API 调用失败: " + extractErrorMessage(e));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("Embedding API 调用失败: " + e.getMessage());
        }
    }

    public ChatCompletionResult createChatCompletion(String baseUrl, String apiKey, String model, String prompt) {
        requireApiKey(apiKey, "Chat");
        String url = joinPath(baseUrl, "/chat/completions");
        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "temperature", 0.3
        );

        try {
            String responseBody = restClient(apiKey)
                    .post()
                    .uri(url)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return parseChatResponse(responseBody);
        } catch (RestClientResponseException e) {
            throw new BusinessException("Chat API 调用失败: " + extractErrorMessage(e));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("Chat API 调用失败: " + e.getMessage());
        }
    }

    private RestClient restClient(String apiKey) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(15));
        factory.setReadTimeout(Duration.ofSeconds(120));
        return RestClient.builder()
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    private float[] parseEmbeddingResponse(String responseBody, int expectedDimension) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode data = root.path("data");
        if (!data.isArray() || data.isEmpty()) {
            throw new BusinessException("Embedding API 返回为空");
        }
        JsonNode embeddingNode = data.get(0).path("embedding");
        if (!embeddingNode.isArray()) {
            throw new BusinessException("Embedding API 返回格式异常");
        }
        float[] vector = new float[embeddingNode.size()];
        for (int i = 0; i < embeddingNode.size(); i++) {
            vector[i] = (float) embeddingNode.get(i).asDouble();
        }
        if (expectedDimension > 0 && vector.length != expectedDimension) {
            throw new BusinessException(
                    "Embedding 返回维度为 " + vector.length + "，与配置 rag.embedding.dimension="
                            + expectedDimension + " 不一致，请调整配置后重建向量");
        }
        return vector;
    }

    private ChatCompletionResult parseChatResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode content = root.path("choices").path(0).path("message").path("content");
        if (content.isMissingNode() || content.asText().isBlank()) {
            throw new BusinessException("Chat API 返回内容为空");
        }
        JsonNode usage = root.path("usage");
        int promptTokens = usage.path("prompt_tokens").asInt(0);
        int completionTokens = usage.path("completion_tokens").asInt(0);
        return new ChatCompletionResult(content.asText(), promptTokens, completionTokens);
    }

    private String extractErrorMessage(RestClientResponseException e) {
        String body = e.getResponseBodyAsString();
        if (body == null || body.isBlank()) {
            return e.getStatusCode() + " " + e.getStatusText();
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode message = root.path("error").path("message");
            if (!message.isMissingNode()) {
                return message.asText();
            }
        } catch (Exception ignored) {
            // use raw body
        }
        return body.length() > 300 ? body.substring(0, 300) + "..." : body;
    }

    private void requireApiKey(String apiKey, String type) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new BusinessException(type + " API Key 未配置，请在环境变量或 application-dev.yml 中设置");
        }
    }

    private String joinPath(String baseUrl, String path) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new BusinessException("API base-url 未配置");
        }
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        if (base.endsWith("/v1") && path.startsWith("/v1/")) {
            path = path.substring(3);
        }
        return path.startsWith("/") ? base + path : base + "/" + path;
    }

    public record ChatCompletionResult(String content, int promptTokens, int completionTokens) {
    }
}

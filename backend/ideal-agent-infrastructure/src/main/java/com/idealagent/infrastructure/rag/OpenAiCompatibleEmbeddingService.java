package com.idealagent.infrastructure.rag;

import com.idealagent.domain.rag.service.IEmbeddingService;
import com.idealagent.domain.rag.service.RagException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class OpenAiCompatibleEmbeddingService implements IEmbeddingService {
    private static final String DEFAULT_EMBEDDINGS_PATH = "/v1/embeddings";
    private static final Pattern EMBEDDING_PATTERN = Pattern.compile("\\\"embedding\\\"\\s*:\\s*\\[(.*?)]", Pattern.DOTALL);

    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final Integer dimensions;
    private final String encodingFormat;
    private final String embeddingsPath;
    private final EmbeddingTransport transport;

    @Autowired
    public OpenAiCompatibleEmbeddingService(
            @Value("${ideal-agent.embedding.base-url:}") String baseUrl,
            @Value("${ideal-agent.embedding.api-key:}") String apiKey,
            @Value("${ideal-agent.embedding.model:text-embedding-v4}") String model,
            @Value("${ideal-agent.embedding.dimensions:1024}") Integer dimensions,
            @Value("${ideal-agent.embedding.encoding-format:float}") String encodingFormat,
            @Value("${ideal-agent.embedding.embeddings-path:/v1/embeddings}") String embeddingsPath) {
        this(baseUrl, apiKey, model, dimensions, encodingFormat, embeddingsPath, new JavaHttpEmbeddingTransport());
    }

    OpenAiCompatibleEmbeddingService(String baseUrl, String apiKey, String model, Integer dimensions, String encodingFormat, String embeddingsPath, EmbeddingTransport transport) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.dimensions = dimensions;
        this.encodingFormat = encodingFormat;
        this.embeddingsPath = embeddingsPath;
        this.transport = transport;
    }

    @Override
    public float[] embed(String text) {
        validateConfig();
        OpenAiResponse response = transport.post(embeddingsUrl(), headers(), requestBody(text));
        if (response == null) {
            throw new RagException("Embedding 调用失败：响应为空");
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RagException("Embedding 调用失败：" + response.body());
        }
        return parseEmbedding(response.body());
    }

    private void validateConfig() {
        if (!StringUtils.hasText(baseUrl) || !StringUtils.hasText(apiKey) || !StringUtils.hasText(model)
                || dimensions == null || dimensions <= 0) {
            throw new RagException("Embedding 服务配置不完整，请检查 ideal-agent.embedding 配置");
        }
    }

    private Map<String, String> headers() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Bearer " + apiKey);
        headers.put("Content-Type", "application/json");
        return headers;
    }

    private String requestBody(String text) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\"model\":\"").append(jsonEscape(model)).append("\"");
        builder.append(",\"input\":\"").append(jsonEscape(text == null ? "" : text)).append("\"");
        builder.append(",\"dimensions\":").append(dimensions);
        if (StringUtils.hasText(encodingFormat)) {
            builder.append(",\"encoding_format\":\"").append(jsonEscape(encodingFormat)).append("\"");
        }
        return builder.append('}').toString();
    }

    private float[] parseEmbedding(String body) {
        Matcher matcher = EMBEDDING_PATTERN.matcher(body == null ? "" : body);
        if (!matcher.find()) {
            throw new RagException("Embedding 服务返回向量为空");
        }
        String vectorText = matcher.group(1).trim();
        if (vectorText.isEmpty()) {
            throw new RagException("Embedding 服务返回向量为空");
        }
        String[] parts = vectorText.split(",");
        float[] embedding = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                embedding[i] = Float.parseFloat(parts[i].trim());
            } catch (NumberFormatException e) {
                throw new RagException("Embedding 服务返回向量格式错误", e);
            }
        }
        return embedding;
    }

    private String embeddingsUrl() {
        String normalizedBase = baseUrl.trim();
        while (normalizedBase.endsWith("/")) {
            normalizedBase = normalizedBase.substring(0, normalizedBase.length() - 1);
        }

        String normalizedPath = StringUtils.hasText(embeddingsPath) ? embeddingsPath.trim() : DEFAULT_EMBEDDINGS_PATH;
        if (normalizedPath.startsWith("http://") || normalizedPath.startsWith("https://")) {
            return normalizedPath;
        }
        if (!normalizedPath.startsWith("/")) {
            normalizedPath = "/" + normalizedPath;
        }
        if (normalizedBase.endsWith("/v1") && DEFAULT_EMBEDDINGS_PATH.equals(normalizedPath)) {
            return normalizedBase + "/embeddings";
        }
        return normalizedBase + normalizedPath;
    }

    private String jsonEscape(String value) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '\\' -> builder.append("\\\\");
                case '"' -> builder.append("\\\"");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> builder.append(ch);
            }
        }
        return builder.toString();
    }

    record OpenAiResponse(int statusCode, String body) {
    }

    interface EmbeddingTransport {
        OpenAiResponse post(String url, Map<String, String> headers, String body);
    }

    private static class JavaHttpEmbeddingTransport implements EmbeddingTransport {
        private final HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        @Override
        public OpenAiResponse post(String url, Map<String, String> headers, String body) {
            try {
                HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(url))
                        .timeout(Duration.ofSeconds(60))
                        .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
                headers.forEach(request::header);
                HttpResponse<String> response = httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                return new OpenAiResponse(response.statusCode(), response.body());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RagException("Embedding 调用被中断");
            } catch (IOException | IllegalArgumentException e) {
                throw new RagException("Embedding 调用失败：" + e.getMessage());
            }
        }
    }
}

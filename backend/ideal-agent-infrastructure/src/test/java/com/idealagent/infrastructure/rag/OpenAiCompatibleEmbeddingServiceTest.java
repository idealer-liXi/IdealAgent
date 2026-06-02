package com.idealagent.infrastructure.rag;

import com.idealagent.domain.rag.service.RagException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenAiCompatibleEmbeddingServiceTest {
    private RecordingTransport transport;

    @BeforeEach
    void setUp() {
        transport = new RecordingTransport();
    }

    @Test
    void embedCallsOpenAiCompatibleEndpointUsingMiniAgentStyleConfig() {
        OpenAiCompatibleEmbeddingService service = new OpenAiCompatibleEmbeddingService(
                "https://api.example.com", "sk-test", "text-embedding-v4", 1024, "float", "/v1/embeddings", transport);
        transport.response = new OpenAiCompatibleEmbeddingService.OpenAiResponse(200,
                "{\"data\":[{\"embedding\":[0.1,-0.2,3.0E-4]}]}");

        float[] embedding = service.embed("IdealAgent RAG");

        assertThat(embedding).containsExactly(0.1f, -0.2f, 3.0E-4f);
        assertThat(transport.url).isEqualTo("https://api.example.com/v1/embeddings");
        assertThat(transport.headers)
                .containsEntry("Authorization", "Bearer sk-test")
                .containsEntry("Content-Type", "application/json");
        assertThat(transport.body).contains("\"model\":\"text-embedding-v4\"");
        assertThat(transport.body).contains("\"input\":\"IdealAgent RAG\"");
        assertThat(transport.body).contains("\"dimensions\":1024");
        assertThat(transport.body).contains("\"encoding_format\":\"float\"");
    }

    @Test
    void embedDoesNotDuplicateV1WhenBaseUrlAlreadyContainsVersion() {
        OpenAiCompatibleEmbeddingService service = new OpenAiCompatibleEmbeddingService(
                "https://api.example.com/v1", "sk-test", "text-embedding-v4", 1024, "float", "/v1/embeddings", transport);
        transport.response = new OpenAiCompatibleEmbeddingService.OpenAiResponse(200,
                "{\"data\":[{\"embedding\":[1.0]}]}");

        service.embed("text");

        assertThat(transport.url).isEqualTo("https://api.example.com/v1/embeddings");
    }

    @Test
    void embedRejectsMissingRequiredConfig() {
        OpenAiCompatibleEmbeddingService service = new OpenAiCompatibleEmbeddingService(
                "", "sk-test", "text-embedding-v4", 1024, "float", "/v1/embeddings", transport);

        assertThatThrownBy(() -> service.embed("text"))
                .isInstanceOf(RagException.class)
                .hasMessage("Embedding 服务配置不完整，请检查 ideal-agent.embedding 配置");
    }

    @Test
    void embedRejectsRemoteFailure() {
        OpenAiCompatibleEmbeddingService service = new OpenAiCompatibleEmbeddingService(
                "https://api.example.com", "sk-test", "text-embedding-v4", 1024, "float", "/v1/embeddings", transport);
        transport.response = new OpenAiCompatibleEmbeddingService.OpenAiResponse(401, "invalid key");

        assertThatThrownBy(() -> service.embed("text"))
                .isInstanceOf(RagException.class)
                .hasMessage("Embedding 调用失败：invalid key");
    }

    private static class RecordingTransport implements OpenAiCompatibleEmbeddingService.EmbeddingTransport {
        private String url;
        private Map<String, String> headers;
        private String body;
        private OpenAiCompatibleEmbeddingService.OpenAiResponse response;

        @Override
        public OpenAiCompatibleEmbeddingService.OpenAiResponse post(String url, Map<String, String> headers, String body) {
            this.url = url;
            this.headers = new HashMap<>(headers);
            this.body = body;
            return response;
        }
    }
}

package com.idealagent.infrastructure.chat;

import com.idealagent.domain.chat.model.entity.ChatMessage;
import com.idealagent.domain.chat.service.ChatException;
import com.idealagent.domain.config.model.entity.AiConfigRecord;
import com.idealagent.domain.config.repository.IAiConfigRepository;
import com.idealagent.domain.config.service.ConfigKind;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenAiCompatibleChatClientTest {
    private FakeAiConfigRepository repository;
    private RecordingTransport transport;
    private OpenAiCompatibleChatClient chatClient;

    @BeforeEach
    void setUp() {
        repository = new FakeAiConfigRepository();
        transport = new RecordingTransport();
        chatClient = new OpenAiCompatibleChatClient(repository, transport);
    }

    @Test
    void completeCallsOpenAiCompatibleEndpointUsingClientModelAndApiConfig() {
        repository.records.add(api("api_deepseek", "https://api.deepseek.com", "sk-test"));
        repository.records.add(model("model_deepseek_v4_flash", "deepseek-v4-flash", "api_deepseek"));
        repository.records.add(client("client_deepseek_v4_flash", "model_deepseek_v4_flash"));
        transport.response = new OpenAiCompatibleChatClient.OpenAiResponse(200,
                "{\"choices\":[{\"message\":{\"content\":\"真实模型回复\"}}]}");

        String answer = chatClient.complete("client_deepseek_v4_flash", List.of(
                message("system", "Use Chinese."),
                message("user", "你好")));

        assertThat(answer).isEqualTo("真实模型回复");
        assertThat(transport.url).isEqualTo("https://api.deepseek.com/v1/chat/completions");
        assertThat(transport.headers)
                .containsEntry("Authorization", "Bearer sk-test")
                .containsEntry("Content-Type", "application/json");
        assertThat(transport.body).contains("\"model\":\"deepseek-v4-flash\"");
        assertThat(transport.body).contains("\"role\":\"system\"", "\"role\":\"user\"", "\"content\":\"你好\"");
    }

    @Test
    void completeReturnsLocalEchoWithoutCallingRemoteWhenClientIsLocalEcho() {
        String answer = chatClient.complete("local_echo", List.of(message("user", "测试")));

        assertThat(answer).isEqualTo("IdealAgent local chat client [local_echo] received: 测试");
        assertThat(transport.url).isNull();
    }

    @Test
    void streamParsesOpenAiCompatibleDeltaEvents() {
        repository.records.add(api("api_deepseek", "https://api.deepseek.com", "sk-test"));
        repository.records.add(model("model_deepseek_v4_flash", "deepseek-v4-flash", "api_deepseek"));
        repository.records.add(client("client_deepseek_v4_flash", "model_deepseek_v4_flash"));
        transport.streamLines = List.of(
                "data: {\"choices\":[{\"delta\":{\"content\":\"你\"}}]}",
                "data: {\"choices\":[{\"delta\":{\"content\":\"好\"}}]}",
                "data: [DONE]");
        List<String> deltas = new ArrayList<>();

        chatClient.stream("client_deepseek_v4_flash", List.of(message("user", "你好")), deltas::add);

        assertThat(deltas).containsExactly("你", "好");
        assertThat(transport.body).contains("\"stream\":true");
    }

    @Test
    void completeRejectsMissingClientConfiguration() {
        assertThatThrownBy(() -> chatClient.complete("client_missing", List.of(message("user", "你好"))))
                .isInstanceOf(ChatException.class)
                .hasMessage("Client 不存在或未启用");
    }

    @Test
    void springCanInstantiateOpenAiCompatibleChatClientBean() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.registerBean(IAiConfigRepository.class, FakeAiConfigRepository::new);
        context.register(OpenAiCompatibleChatClient.class);
        context.refresh();

        assertThat(context.getBean(OpenAiCompatibleChatClient.class)).isNotNull();

        context.close();
    }

    private ChatMessage message(String role, String content) {
        ChatMessage message = new ChatMessage();
        message.setRole(role);
        message.setContent(content);
        return message;
    }

    private AiConfigRecord api(String id, String baseUrl, String apiKey) {
        AiConfigRecord record = record(ConfigKind.API, id, "DeepSeek API", "openai", 1);
        record.setContent(baseUrl);
        record.setSecret(apiKey);
        return record;
    }

    private AiConfigRecord model(String id, String modelName, String apiId) {
        AiConfigRecord record = record(ConfigKind.MODEL, id, modelName, "chat", 1);
        record.setRefId(apiId);
        return record;
    }

    private AiConfigRecord client(String id, String modelId) {
        AiConfigRecord record = record(ConfigKind.CLIENT, id, "DeepSeek Chat", "chat", 1);
        record.setContent("assistant");
        record.setRefId(modelId);
        return record;
    }

    private AiConfigRecord record(ConfigKind kind, String id, String name, String type, Integer status) {
        AiConfigRecord record = new AiConfigRecord();
        record.setConfigId(id);
        record.setName(name);
        record.setType(type);
        record.setStatus(status);
        record.setOwnerType(kind.name());
        return record;
    }

    private static class RecordingTransport implements OpenAiCompatibleChatClient.OpenAiTransport {
        private String url;
        private Map<String, String> headers;
        private String body;
        private OpenAiCompatibleChatClient.OpenAiResponse response;
        private List<String> streamLines = List.of();

        @Override
        public OpenAiCompatibleChatClient.OpenAiResponse post(String url, Map<String, String> headers, String body) {
            this.url = url;
            this.headers = new HashMap<>(headers);
            this.body = body;
            return response;
        }

        @Override
        public void stream(String url, Map<String, String> headers, String body, java.util.function.Consumer<String> onLine) {
            this.url = url;
            this.headers = new HashMap<>(headers);
            this.body = body;
            streamLines.forEach(onLine);
        }
    }

    private static class FakeAiConfigRepository implements IAiConfigRepository {
        private final List<AiConfigRecord> records = new ArrayList<>();

        @Override
        public AiConfigRecord save(ConfigKind kind, AiConfigRecord record) {
            records.add(record);
            return record;
        }

        @Override
        public List<AiConfigRecord> list(ConfigKind kind) {
            return records.stream().filter(record -> record.getOwnerType().equals(kind.name())).toList();
        }

        @Override
        public AiConfigRecord update(ConfigKind kind, AiConfigRecord record) {
            records.removeIf(item -> item.getConfigId().equals(record.getConfigId()));
            records.add(record);
            return record;
        }

        @Override
        public void updateStatus(ConfigKind kind, String configId, Integer status) {
            records.stream().filter(item -> item.getConfigId().equals(configId)).forEach(item -> item.setStatus(status));
        }

        @Override
        public void delete(ConfigKind kind, String configId) {
            records.removeIf(item -> item.getConfigId().equals(configId));
        }
    }
}

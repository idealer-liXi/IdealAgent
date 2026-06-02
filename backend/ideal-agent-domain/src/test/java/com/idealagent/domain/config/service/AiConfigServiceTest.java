package com.idealagent.domain.ai.service.config;

import com.idealagent.domain.ai.model.dto.AiConfigRecordDTO;
import com.idealagent.domain.ai.model.entity.AiConfigRecord;
import com.idealagent.domain.ai.model.enumeration.ConfigKind;
import com.idealagent.domain.ai.model.vo.AiConfigRecordVO;
import com.idealagent.domain.ai.repository.IAiConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiConfigServiceTest {

    private FakeAiConfigRepository repository;
    private AiConfigService service;

    @BeforeEach
    void setUp() {
        repository = new FakeAiConfigRepository();
        service = new AiConfigService(repository);
    }

    @Test
    void createStoresApiConfiguration() {
        AiConfigRecordVO created = service.create(ConfigKind.API, new AiConfigRecordDTO(
                "api_openai", "OpenAI", "openai", "https://api.openai.com", "sk-test", null, 1, 0L));

        assertThat(created.configId()).isEqualTo("api_openai");
        assertThat(created.name()).isEqualTo("OpenAI");
        assertThat(repository.list(ConfigKind.API)).hasSize(1);
    }

    @Test
    void listReturnsRecordsByKind() {
        service.create(ConfigKind.API, new AiConfigRecordDTO("api_openai", "OpenAI", "openai", null, null, null, 1, 0L));
        service.create(ConfigKind.MODEL, new AiConfigRecordDTO("model_gpt", "GPT", "chat", null, null, "api_openai", 1, 0L));

        List<AiConfigRecordVO> apis = service.list(ConfigKind.API);

        assertThat(apis).extracting(AiConfigRecordVO::configId).containsExactly("api_openai");
    }

    @Test
    void createRejectsBlankId() {
        assertThatThrownBy(() -> service.create(ConfigKind.API, new AiConfigRecordDTO(
                "", "OpenAI", "openai", null, null, null, 1, 0L)))
                .isInstanceOf(AiConfigException.class)
                .hasMessage("配置ID不能为空");
    }

    @Test
    void updateUsesPathConfigIdAndReturnsUpdatedRecord() {
        service.create(ConfigKind.API, new AiConfigRecordDTO("api_openai", "OpenAI", "openai", "https://api.openai.com", "sk-old", null, 1, 0L));

        AiConfigRecordVO updated = service.update(ConfigKind.API, "api_openai", new AiConfigRecordDTO(
                "ignored", "DeepSeek", "openai", "https://api.deepseek.com", "sk-new", null, 1, 0L));

        assertThat(updated.configId()).isEqualTo("api_openai");
        assertThat(updated.name()).isEqualTo("DeepSeek");
        assertThat(updated.content()).isEqualTo("https://api.deepseek.com");
    }

    @Test
    void updateStatusChangesExistingRecordStatus() {
        service.create(ConfigKind.CLIENT, new AiConfigRecordDTO("client_chat", "Chat", "chat", "assistant", null, "model_chat", 1, 0L));

        service.updateStatus(ConfigKind.CLIENT, "client_chat", 0);

        assertThat(repository.list(ConfigKind.CLIENT).get(0).getStatus()).isZero();
    }

    @Test
    void deleteRemovesExistingRecord() {
        service.create(ConfigKind.MODEL, new AiConfigRecordDTO("model_chat", "deepseek-v4-flash", "chat", null, null, "api_ds", 1, 0L));

        service.delete(ConfigKind.MODEL, "model_chat");

        assertThat(repository.list(ConfigKind.MODEL)).isEmpty();
    }

    @Test
    void deleteRejectsApiReferencedByModel() {
        service.create(ConfigKind.API, new AiConfigRecordDTO("api_ds", "DeepSeek", "openai", "https://api.deepseek.com", "sk", null, 1, 0L));
        service.create(ConfigKind.MODEL, new AiConfigRecordDTO("model_chat", "deepseek-v4-flash", "chat", null, null, "api_ds", 1, 0L));

        assertThatThrownBy(() -> service.delete(ConfigKind.API, "api_ds"))
                .isInstanceOf(AiConfigException.class)
                .hasMessage("API 已被 Model 引用，不能删除");
    }

    @Test
    void deleteRejectsModelReferencedByClient() {
        service.create(ConfigKind.MODEL, new AiConfigRecordDTO("model_chat", "deepseek-v4-flash", "chat", null, null, "api_ds", 1, 0L));
        service.create(ConfigKind.CLIENT, new AiConfigRecordDTO("client_chat", "Chat", "chat", "assistant", "deepseek-v4-flash", "model_chat", 1, 0L));

        assertThatThrownBy(() -> service.delete(ConfigKind.MODEL, "model_chat"))
                .isInstanceOf(AiConfigException.class)
                .hasMessage("Model 已被 Client 引用，不能删除");
    }

    @Test
    void deleteRejectsClientReferencedByBindingOwner() {
        service.create(ConfigKind.CLIENT, new AiConfigRecordDTO("client_chat", "Chat", "chat", "assistant", "deepseek-v4-flash", "model_chat", 1, 0L));
        service.create(ConfigKind.CONFIG, new AiConfigRecordDTO("config_client_prompt", null, "client", "client_chat", "prompt", "prompt_system", 1, 0L, "client", "prompt"));

        assertThatThrownBy(() -> service.delete(ConfigKind.CLIENT, "client_chat"))
                .isInstanceOf(AiConfigException.class)
                .hasMessage("Client 已被 Binding 引用，不能删除");
    }

    @Test
    void deleteRejectsBindingTargetsReferencedByBinding() {
        service.create(ConfigKind.PROMPT, new AiConfigRecordDTO("prompt_system", "System", "system", "hello", null, null, 1, 0L));
        service.create(ConfigKind.ADVISOR, new AiConfigRecordDTO("advisor_memory", "Memory", "memory", "{}", null, null, 1, 0L));
        service.create(ConfigKind.MCP, new AiConfigRecordDTO("mcp_weather", "Weather", "stdio", "{}", null, null, 1, 0L));
        service.create(ConfigKind.CONFIG, new AiConfigRecordDTO("config_prompt", null, "client", "client_chat", "prompt", "prompt_system", 1, 0L, "client", "prompt"));
        service.create(ConfigKind.CONFIG, new AiConfigRecordDTO("config_advisor", null, "client", "client_chat", "advisor", "advisor_memory", 1, 0L, "client", "advisor"));
        service.create(ConfigKind.CONFIG, new AiConfigRecordDTO("config_mcp", null, "client", "client_chat", "mcp", "mcp_weather", 1, 0L, "client", "mcp"));

        assertThatThrownBy(() -> service.delete(ConfigKind.PROMPT, "prompt_system"))
                .isInstanceOf(AiConfigException.class)
                .hasMessage("Prompt 已被 Binding 引用，不能删除");
        assertThatThrownBy(() -> service.delete(ConfigKind.ADVISOR, "advisor_memory"))
                .isInstanceOf(AiConfigException.class)
                .hasMessage("Advisor 已被 Binding 引用，不能删除");
        assertThatThrownBy(() -> service.delete(ConfigKind.MCP, "mcp_weather"))
                .isInstanceOf(AiConfigException.class)
                .hasMessage("MCP 已被 Binding 引用，不能删除");
    }

    private static class FakeAiConfigRepository implements IAiConfigRepository {
        private final Map<ConfigKind, List<AiConfigRecord>> records = new EnumMap<>(ConfigKind.class);

        @Override
        public AiConfigRecord save(ConfigKind kind, AiConfigRecord record) {
            records.computeIfAbsent(kind, ignored -> new ArrayList<>()).add(record);
            return record;
        }

        @Override
        public List<AiConfigRecord> list(ConfigKind kind) {
            return records.getOrDefault(kind, List.of());
        }

        @Override
        public AiConfigRecord update(ConfigKind kind, AiConfigRecord record) {
            List<AiConfigRecord> values = records.computeIfAbsent(kind, ignored -> new ArrayList<>());
            values.removeIf(item -> item.getConfigId().equals(record.getConfigId()));
            values.add(record);
            return record;
        }

        @Override
        public void updateStatus(ConfigKind kind, String configId, Integer status) {
            records.getOrDefault(kind, List.of()).stream()
                    .filter(item -> item.getConfigId().equals(configId))
                    .findFirst()
                    .ifPresent(item -> item.setStatus(status));
        }

        @Override
        public void delete(ConfigKind kind, String configId) {
            records.getOrDefault(kind, List.of()).removeIf(item -> item.getConfigId().equals(configId));
        }
    }
}

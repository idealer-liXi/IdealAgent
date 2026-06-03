package com.idealagent.domain.ai.service.armory;

import com.idealagent.domain.ai.model.entity.AiConfigRecord;
import com.idealagent.domain.ai.model.enumeration.ConfigKind;
import com.idealagent.domain.ai.repository.IAiConfigRepository;
import com.idealagent.domain.ai.service.chat.ChatException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChatClientArmoryTest {
    private FakeAiConfigRepository repository;
    private ChatClientArmory armory;

    @BeforeEach
    void setUp() {
        repository = new FakeAiConfigRepository();
        armory = new ChatClientArmory(repository);
    }

    @Test
    void resolveBuildsAndCachesChatClientFromClientModelApiChain() {
        repository.records.add(record(ConfigKind.API, "api_default", "OpenAI", "https://api.example.com", "sk-test", null, 1));
        repository.records.add(record(ConfigKind.MODEL, "model_default", "gpt-4o-mini", null, null, "api_default", 1));
        repository.records.add(record(ConfigKind.CLIENT, "client_default", "Default Client", null, null, "model_default", 1));

        ChatClient first = armory.resolve("client_default");
        ChatClient second = armory.resolve("client_default");

        assertThat(first).isNotNull();
        assertThat(second).isSameAs(first);
    }

    @Test
    void resolveRejectsDisabledClient() {
        repository.records.add(record(ConfigKind.CLIENT, "client_disabled", "Disabled", null, null, "model_default", 0));

        assertThatThrownBy(() -> armory.resolve("client_disabled"))
                .isInstanceOf(ChatException.class)
                .hasMessage("Client 不可用");
    }

    @Test
    void resolveRejectsModelWithoutApiBinding() {
        repository.records.add(record(ConfigKind.MODEL, "model_default", "gpt-4o-mini", null, null, null, 1));
        repository.records.add(record(ConfigKind.CLIENT, "client_default", "Default Client", null, null, "model_default", 1));

        assertThatThrownBy(() -> armory.resolve("client_default"))
                .isInstanceOf(ChatException.class)
                .hasMessage("Model 未绑定 API");
    }

    private AiConfigRecord record(ConfigKind kind, String id, String name, String content, String secret, String refId, Integer status) {
        AiConfigRecord record = new AiConfigRecord();
        record.setConfigId(id);
        record.setName(name);
        record.setContent(content);
        record.setSecret(secret);
        record.setRefId(refId);
        record.setStatus(status);
        record.setType(kind.name());
        return record;
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
            return records.stream().filter(record -> kind.name().equals(record.getType())).toList();
        }

        @Override
        public AiConfigRecord find(ConfigKind kind, String configId) {
            return list(kind).stream()
                    .filter(record -> configId.equals(record.getConfigId()))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public AiConfigRecord update(ConfigKind kind, AiConfigRecord record) {
            return record;
        }

        @Override
        public void updateStatus(ConfigKind kind, String configId, Integer status) {
        }

        @Override
        public void delete(ConfigKind kind, String configId) {
        }
    }
}

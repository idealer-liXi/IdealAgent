package com.idealagent.domain.config.service;

import com.idealagent.domain.config.model.dto.AiConfigRecordDTO;
import com.idealagent.domain.config.model.entity.AiConfigRecord;
import com.idealagent.domain.config.model.vo.AiConfigRecordVO;
import com.idealagent.domain.config.repository.IAiConfigRepository;
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
    }
}

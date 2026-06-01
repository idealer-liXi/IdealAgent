package com.idealagent.infrastructure.repository;

import com.idealagent.domain.config.model.entity.AiConfigRecord;
import com.idealagent.domain.config.service.ConfigKind;
import com.idealagent.infrastructure.persistent.dao.IAiConfigDao;
import com.idealagent.infrastructure.persistent.po.AiConfigData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AiConfigRepositoryTest {
    private AiConfigRepository repository;

    @BeforeEach
    void setUp() {
        repository = new AiConfigRepository(new FakeAiConfigDao());
    }

    @Test
    void saveAndListApiConfiguration() {
        AiConfigRecord record = new AiConfigRecord();
        record.setConfigId("api_openai");
        record.setName("OpenAI");
        record.setType("openai");
        record.setContent("https://api.openai.com");
        record.setSecret("sk-test");
        record.setStatus(1);
        record.setOwnerId(0L);

        repository.save(ConfigKind.API, record);

        assertThat(repository.list(ConfigKind.API))
                .extracting(AiConfigRecord::getConfigId)
                .containsExactly("api_openai");
    }

    private static class FakeAiConfigDao implements IAiConfigDao {
        private final List<AiConfigData> apis = new ArrayList<>();

        @Override
        public int insertApi(AiConfigData record) {
            apis.add(record);
            return 1;
        }

        @Override
        public List<AiConfigData> listApis() {
            return apis;
        }

        @Override
        public int insertModel(AiConfigData record) {
            return 1;
        }

        @Override
        public List<AiConfigData> listModels() {
            return List.of();
        }

        @Override
        public int insertClient(AiConfigData record) {
            return 1;
        }

        @Override
        public List<AiConfigData> listClients() {
            return List.of();
        }

        @Override
        public int insertPrompt(AiConfigData record) {
            return 1;
        }

        @Override
        public List<AiConfigData> listPrompts() {
            return List.of();
        }

        @Override
        public int insertAdvisor(AiConfigData record) {
            return 1;
        }

        @Override
        public List<AiConfigData> listAdvisors() {
            return List.of();
        }

        @Override
        public int insertMcp(AiConfigData record) {
            return 1;
        }

        @Override
        public List<AiConfigData> listMcps() {
            return List.of();
        }

        @Override
        public int insertConfig(AiConfigData record) {
            return 1;
        }

        @Override
        public List<AiConfigData> listConfigs() {
            return List.of();
        }
    }
}

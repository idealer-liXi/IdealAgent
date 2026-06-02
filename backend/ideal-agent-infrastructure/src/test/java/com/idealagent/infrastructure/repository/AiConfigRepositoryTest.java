package com.idealagent.infrastructure.repository;

import com.idealagent.domain.ai.model.entity.AiConfigRecord;
import com.idealagent.domain.ai.model.enumeration.ConfigKind;
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

    @Test
    void updateStatusAndDeleteApiConfiguration() {
        AiConfigRecord record = new AiConfigRecord();
        record.setConfigId("api_deepseek");
        record.setName("DeepSeek");
        record.setType("openai");
        record.setContent("https://api.deepseek.com");
        record.setSecret("sk-test");
        record.setStatus(1);
        record.setOwnerId(0L);
        repository.save(ConfigKind.API, record);

        record.setName("DeepSeek Updated");
        repository.update(ConfigKind.API, record);
        repository.updateStatus(ConfigKind.API, "api_deepseek", 0);

        assertThat(repository.list(ConfigKind.API).get(0).getName()).isEqualTo("DeepSeek Updated");
        assertThat(repository.list(ConfigKind.API).get(0).getStatus()).isZero();

        repository.delete(ConfigKind.API, "api_deepseek");

        assertThat(repository.list(ConfigKind.API)).isEmpty();
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
        public int updateApi(AiConfigData record) {
            apis.removeIf(item -> item.getConfigId().equals(record.getConfigId()));
            apis.add(record);
            return 1;
        }

        @Override
        public int updateApiStatus(String configId, Integer status) {
            apis.stream().filter(item -> item.getConfigId().equals(configId)).forEach(item -> item.setStatus(status));
            return 1;
        }

        @Override
        public int deleteApi(String configId) {
            apis.removeIf(item -> item.getConfigId().equals(configId));
            return 1;
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
        public int updateModel(AiConfigData record) {
            return 1;
        }

        @Override
        public int updateModelStatus(String configId, Integer status) {
            return 1;
        }

        @Override
        public int deleteModel(String configId) {
            return 1;
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
        public int updateClient(AiConfigData record) {
            return 1;
        }

        @Override
        public int updateClientStatus(String configId, Integer status) {
            return 1;
        }

        @Override
        public int deleteClient(String configId) {
            return 1;
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
        public int updatePrompt(AiConfigData record) {
            return 1;
        }

        @Override
        public int updatePromptStatus(String configId, Integer status) {
            return 1;
        }

        @Override
        public int deletePrompt(String configId) {
            return 1;
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
        public int updateAdvisor(AiConfigData record) {
            return 1;
        }

        @Override
        public int updateAdvisorStatus(String configId, Integer status) {
            return 1;
        }

        @Override
        public int deleteAdvisor(String configId) {
            return 1;
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
        public int updateMcp(AiConfigData record) {
            return 1;
        }

        @Override
        public int updateMcpStatus(String configId, Integer status) {
            return 1;
        }

        @Override
        public int deleteMcp(String configId) {
            return 1;
        }

        @Override
        public int insertConfig(AiConfigData record) {
            return 1;
        }

        @Override
        public List<AiConfigData> listConfigs() {
            return List.of();
        }

        @Override
        public int updateConfig(AiConfigData record) {
            return 1;
        }

        @Override
        public int updateConfigStatus(String configId, Integer status) {
            return 1;
        }

        @Override
        public int deleteConfig(String configId) {
            return 1;
        }
    }
}

package com.idealagent.domain.ai.service.augment;

import com.idealagent.domain.ai.model.entity.AiConfigRecord;
import com.idealagent.domain.ai.model.enumeration.ConfigKind;
import com.idealagent.domain.ai.repository.IAiConfigRepository;
import com.idealagent.domain.ai.service.armory.mcp.IMcpClientArmory;
import com.idealagent.domain.ai.service.mcp.McpException;
import io.modelcontextprotocol.client.McpSyncClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class McpToolServiceTest {
    private FakeConfigRepository repository;
    private RecordingMcpClientArmory armory;
    private McpToolService service;

    @BeforeEach
    void setUp() {
        repository = new FakeConfigRepository();
        armory = new RecordingMcpClientArmory();
        service = new McpToolService(repository, armory);
    }

    @Test
    void augmentMcpToolBuildsClientsForEnabledClientBindings() {
        repository.add(ConfigKind.CONFIG, binding("bind_1", "client_chat", "mcp_weather", 1));
        repository.add(ConfigKind.MCP, mcp("mcp_weather", 1));

        McpToolHandle handle = service.augmentMcpTool(7L, "client_chat");

        assertThat(handle.toolCallbackProvider()).isNotNull();
        assertThat(armory.builtIds).containsExactly("mcp_weather");
        assertThat(armory.lastUserId).isEqualTo(7L);
    }

    @Test
    void augmentMcpToolIgnoresDisabledBindingsAndDisabledMcpRecords() {
        repository.add(ConfigKind.CONFIG, binding("bind_disabled", "client_chat", "mcp_a", 0));
        repository.add(ConfigKind.CONFIG, binding("bind_enabled", "client_chat", "mcp_b", 1));
        repository.add(ConfigKind.MCP, mcp("mcp_a", 1));
        repository.add(ConfigKind.MCP, mcp("mcp_b", 0));

        service.augmentMcpTool(7L, "client_chat");

        assertThat(armory.builtIds).isEmpty();
    }

    @Test
    void augmentMcpToolFailsWhenEnabledBindingReferencesMissingMcp() {
        repository.add(ConfigKind.CONFIG, binding("bind_1", "client_chat", "mcp_missing", 1));

        assertThatThrownBy(() -> service.augmentMcpTool(7L, "client_chat"))
                .isInstanceOf(McpException.class)
                .hasMessage("MCP 配置不存在");
    }

    private AiConfigRecord binding(String id, String clientId, String mcpId, Integer status) {
        AiConfigRecord record = new AiConfigRecord();
        record.setConfigId(id);
        record.setOwnerType("client");
        record.setContent(clientId);
        record.setConfigType("mcp");
        record.setRefId(mcpId);
        record.setStatus(status);
        return record;
    }

    private AiConfigRecord mcp(String id, Integer status) {
        AiConfigRecord record = new AiConfigRecord();
        record.setConfigId(id);
        record.setType("stdio");
        record.setContent("{\"command\":\"node\"}");
        record.setStatus(status);
        return record;
    }

    private static class FakeConfigRepository implements IAiConfigRepository {
        private final Map<ConfigKind, List<AiConfigRecord>> records = new EnumMap<>(ConfigKind.class);

        void add(ConfigKind kind, AiConfigRecord record) {
            records.computeIfAbsent(kind, ignored -> new ArrayList<>()).add(record);
        }

        @Override
        public AiConfigRecord save(ConfigKind kind, AiConfigRecord record) {
            add(kind, record);
            return record;
        }

        @Override
        public List<AiConfigRecord> list(ConfigKind kind) {
            return records.getOrDefault(kind, List.of());
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

    private static class RecordingMcpClientArmory implements IMcpClientArmory {
        private final List<String> builtIds = new ArrayList<>();
        private Long lastUserId;

        @Override
        public McpSyncClient build(AiConfigRecord mcpRecord, Long userId) {
            builtIds.add(mcpRecord.getConfigId());
            lastUserId = userId;
            return mock(McpSyncClient.class);
        }
    }
}

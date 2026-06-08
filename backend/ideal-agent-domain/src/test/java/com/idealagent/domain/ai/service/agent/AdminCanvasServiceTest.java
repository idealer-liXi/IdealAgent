package com.idealagent.domain.ai.service.agent;

import com.idealagent.domain.ai.model.dto.AiConfigRecordDTO;
import com.idealagent.domain.ai.model.dto.CanvasNodeDTO;
import com.idealagent.domain.ai.model.dto.CanvasRelationDTO;
import com.idealagent.domain.ai.model.dto.CanvasSaveDTO;
import com.idealagent.domain.ai.model.entity.AiConfigRecord;
import com.idealagent.domain.ai.model.enumeration.ConfigKind;
import com.idealagent.domain.ai.model.vo.AgentManageVO;
import com.idealagent.domain.ai.model.vo.FlowManageVO;
import com.idealagent.domain.ai.repository.IAiConfigRepository;
import com.idealagent.domain.ai.repository.IAgentManagementRepository;
import com.idealagent.domain.ai.service.config.AiConfigService;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

import java.util.List;

class AdminCanvasServiceTest {
    private final IAgentManagementRepository repository = mock(IAgentManagementRepository.class);
    private final IAiConfigRepository configRepository = mock(IAiConfigRepository.class);
    private final AgentManagementService agentManagementService = mock(AgentManagementService.class);
    private final AiConfigService aiConfigService = mock(AiConfigService.class);
    private final AdminCanvasService service = new AdminCanvasService(repository, configRepository, agentManagementService, aiConfigService);

    @Test
    void rejectsUnsupportedRelation() {
        assertThatThrownBy(() -> service.saveRelation(new CanvasRelationDTO("api", "prompt", "api_x", "prompt_x", "agent_x", null, null, null, null)))
                .isInstanceOf(AgentManagementException.class)
                .hasMessageContaining("连接规则不允许");
    }

    @Test
    void savesClientPromptRelationThroughConfigService() {
        CanvasRelationDTO relation = new CanvasRelationDTO("client", "prompt", "client_x", "prompt_x", "agent_x", "prompt", null, null, null);

        service.saveRelation(relation);

        ArgumentCaptor<AiConfigRecordDTO> captor = ArgumentCaptor.forClass(AiConfigRecordDTO.class);
        verify(aiConfigService).create(eq(ConfigKind.CONFIG), captor.capture());
        assertThat(captor.getValue().configId()).isEqualTo("config_client_x_prompt_prompt_x");
        assertThat(captor.getValue().content()).isEqualTo("client_x");
        assertThat(captor.getValue().ownerType()).isEqualTo("client");
        assertThat(captor.getValue().configType()).isEqualTo("prompt");
        assertThat(captor.getValue().refId()).isEqualTo("prompt_x");
        assertThat(captor.getValue().status()).isEqualTo(1);
    }

    @Test
    void savesClientModelRelationThroughConfigServiceUpdate() {
        AiConfigRecord client = config("client_x", "work");
        client.setName("Client X");
        client.setContent("planner");
        client.setSecret("old-model-name");
        client.setOwnerId(10L);
        when(configRepository.find(ConfigKind.CLIENT, "client_x")).thenReturn(client);

        service.saveRelation(new CanvasRelationDTO("client", "model", "client_x", "model_x", "agent_x", "model", null, null, null));

        ArgumentCaptor<AiConfigRecordDTO> captor = ArgumentCaptor.forClass(AiConfigRecordDTO.class);
        verify(aiConfigService).update(eq(ConfigKind.CLIENT), eq("client_x"), captor.capture());
        assertThat(captor.getValue().configId()).isEqualTo("client_x");
        assertThat(captor.getValue().name()).isEqualTo("Client X");
        assertThat(captor.getValue().type()).isEqualTo("work");
        assertThat(captor.getValue().content()).isEqualTo("planner");
        assertThat(captor.getValue().secret()).isEqualTo("old-model-name");
        assertThat(captor.getValue().refId()).isEqualTo("model_x");
        assertThat(captor.getValue().ownerId()).isEqualTo(10L);
    }

    @Test
    void savesModelApiRelationThroughConfigServiceUpdate() {
        AiConfigRecord model = config("model_x", "model");
        model.setName("Model X");
        model.setOwnerId(11L);
        when(configRepository.find(ConfigKind.MODEL, "model_x")).thenReturn(model);

        service.saveRelation(new CanvasRelationDTO("model", "api", "model_x", "api_x", "agent_x", "api", null, null, null));

        ArgumentCaptor<AiConfigRecordDTO> captor = ArgumentCaptor.forClass(AiConfigRecordDTO.class);
        verify(aiConfigService).update(eq(ConfigKind.MODEL), eq("model_x"), captor.capture());
        assertThat(captor.getValue().configId()).isEqualTo("model_x");
        assertThat(captor.getValue().name()).isEqualTo("Model X");
        assertThat(captor.getValue().refId()).isEqualTo("api_x");
        assertThat(captor.getValue().ownerId()).isEqualTo(11L);
    }

    @Test
    void savesFlowRelationThroughAgentManagementService() {
        service.saveRelation(new CanvasRelationDTO("agent", "client", "agent_step", "client_planner", "agent_step", null, "planner", "Plan", 2));

        verify(agentManagementService).createFlow(new com.idealagent.domain.ai.model.dto.FlowManageDTO(null, null, "agent_step", "client_planner", "planner", "Plan", 2));
    }

    @Test
    void rejectsChatClientRagAdvisorRelation() {
        when(configRepository.find(ConfigKind.CLIENT, "client_chat")).thenReturn(config("client_chat", "chat"));
        when(configRepository.find(ConfigKind.ADVISOR, "advisor_rag")).thenReturn(config("advisor_rag", "Rag"));

        assertThatThrownBy(() -> service.saveRelation(new CanvasRelationDTO("client", "advisor", "client_chat", "advisor_rag", "agent_x", "advisor", null, null, null)))
                .isInstanceOf(AgentManagementException.class)
                .hasMessage("Chat Client 不允许绑定 RAG Advisor");
    }

    @Test
    void rejectsChatClientMcpRelation() {
        when(configRepository.find(ConfigKind.CLIENT, "client_chat")).thenReturn(config("client_chat", "chat"));

        assertThatThrownBy(() -> service.saveRelation(new CanvasRelationDTO("client", "mcp", "client_chat", "mcp_weather", "agent_x", "mcp", null, null, null)))
                .isInstanceOf(AgentManagementException.class)
                .hasMessage("Chat Client 不允许绑定 MCP");
    }

    @Test
    void rejectsFlowRelationRoleThatDoesNotMatchAgentStrategy() {
        when(agentManagementService.createFlow(any())).thenThrow(new AgentManagementException("Flow 角色不匹配 Agent 类型"));

        assertThatThrownBy(() -> service.saveRelation(new CanvasRelationDTO("agent", "client", "agent_step", "client_bad", "agent_step", null, "analyzer", "Prompt", 1)))
                .isInstanceOf(AgentManagementException.class)
                .hasMessageContaining("Flow 角色不匹配");
    }

    @Test
    void rejectsFlowRelationSequenceOutsideAgentStrategy() {
        when(agentManagementService.createFlow(any())).thenThrow(new AgentManagementException("Flow 顺序不匹配 Agent 策略"));

        assertThatThrownBy(() -> service.saveRelation(new CanvasRelationDTO("agent", "client", "agent_step", "client_runner", "agent_step", null, "runner", "Prompt", 5)))
                .isInstanceOf(AgentManagementException.class)
                .hasMessageContaining("Flow 顺序不匹配");
    }

    @Test
    void deletesFlowRelationWithoutRequiringFlowRolePromptOrSeq() {
        CanvasRelationDTO relation = new CanvasRelationDTO("agent", "client", "agent_step", "client_planner", "agent_step", null, null, null, null);

        service.deleteRelation(relation);

        verify(agentManagementService).deleteFlow("agent_step", "client_planner");
    }

    @Test
    void deletesClientConfigRelationThroughConfigService() {
        service.deleteRelation(new CanvasRelationDTO("client", "prompt", "client_x", "prompt_x", "agent_x", "prompt", null, null, null));

        verify(aiConfigService).delete(ConfigKind.CONFIG, "config_client_x_prompt_prompt_x");
    }

    @Test
    void savesCanvasNodeThroughConfigServiceUpdateWhenNodeExists() {
        when(configRepository.find(ConfigKind.CLIENT, "client_x")).thenReturn(config("client_x", "work"));

        service.saveNode(new CanvasNodeDTO("client", "client_x", "agent_x", Map.of(
                "configId", "client_x",
                "name", "Client X",
                "type", "work",
                "content", "planner",
                "refId", "model_x",
                "status", 1)));

        verify(aiConfigService).update(eq(ConfigKind.CLIENT), eq("client_x"), any(AiConfigRecordDTO.class));
    }

    @Test
    void savesCanvasNodeThroughConfigServiceCreateWhenNodeDoesNotExist() {
        when(configRepository.find(ConfigKind.MCP, "mcp_x")).thenReturn(null);

        service.saveNode(new CanvasNodeDTO("mcp", "mcp_x", "agent_x", Map.of(
                "configId", "mcp_x",
                "name", "MCP X",
                "type", "stdio",
                "content", "{}",
                "status", 1)));

        verify(aiConfigService).create(eq(ConfigKind.MCP), any(AiConfigRecordDTO.class));
    }

    @Test
    void savesCanvasBatchAndValidatesCompleteStrategyFlow() {
        when(repository.findAgent("agent_step")).thenReturn(new AgentManageVO("agent_step", "Step", "step", "desc", "model_x", "", 1));
        when(repository.listFlows("agent_step")).thenReturn(List.of(
                new FlowManageVO("agent_step", "client_inspector", "inspector", "Inspect", 1),
                new FlowManageVO("agent_step", "client_planner", "planner", "Plan", 2),
                new FlowManageVO("agent_step", "client_runner", "runner", "Run", 3),
                new FlowManageVO("agent_step", "client_replier", "replier", "Reply", 4)));

        service.saveCanvas("agent_step", new CanvasSaveDTO(
                List.of(),
                List.of(new CanvasRelationDTO("client", "prompt", "client_planner", "prompt_x", "agent_step", "prompt", null, null, null)),
                List.of(new CanvasRelationDTO("client", "advisor", "client_planner", "advisor_old", "agent_step", "advisor", null, null, null))));

        verify(aiConfigService).delete(ConfigKind.CONFIG, "config_client_planner_advisor_advisor_old");
        verify(aiConfigService).create(eq(ConfigKind.CONFIG), any(AiConfigRecordDTO.class));
    }

    @Test
    void rejectsCanvasBatchWhenStrategyFlowIsIncomplete() {
        when(repository.findAgent("agent_step")).thenReturn(new AgentManageVO("agent_step", "Step", "step", "desc", "model_x", "", 1));
        when(repository.listFlows("agent_step")).thenReturn(List.of(
                new FlowManageVO("agent_step", "client_inspector", "inspector", "Inspect", 1)));

        assertThatThrownBy(() -> service.saveCanvas("agent_step", new CanvasSaveDTO(
                List.of(),
                List.of(new CanvasRelationDTO("client", "prompt", "client_inspector", "prompt_x", "agent_step", "prompt", null, null, null)),
                List.of())))
                .isInstanceOf(AgentManagementException.class)
                .hasMessageContaining("Agent Flow 未完整配置");
    }

    private AiConfigRecord config(String id, String type) {
        AiConfigRecord record = new AiConfigRecord();
        record.setConfigId(id);
        record.setType(type);
        record.setStatus(1);
        return record;
    }
}

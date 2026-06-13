package com.idealagent.domain.ai.service.workshop;

import com.idealagent.domain.ai.model.dto.AgentManageDTO;
import com.idealagent.domain.ai.model.dto.AiConfigRecordDTO;
import com.idealagent.domain.ai.model.dto.FlowManageDTO;
import com.idealagent.domain.ai.model.dto.WorkshopAgentCreateDTO;
import com.idealagent.domain.ai.model.entity.AiConfigRecord;
import com.idealagent.domain.ai.model.enumeration.ConfigKind;
import com.idealagent.domain.ai.model.vo.AgentManageVO;
import com.idealagent.domain.ai.model.vo.CanvasGraphVO;
import com.idealagent.domain.ai.model.vo.FlowManageVO;
import com.idealagent.domain.ai.model.vo.FlowOptionsVO;
import com.idealagent.domain.ai.repository.IAiConfigRepository;
import com.idealagent.domain.ai.repository.IAgentManagementRepository;
import com.idealagent.domain.ai.service.agent.AgentManagementException;
import com.idealagent.domain.ai.service.agent.AgentManagementService;
import com.idealagent.domain.ai.service.agent.StrategyPromptTemplateService;
import com.idealagent.domain.ai.service.config.AiConfigService;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkshopServiceTest {
    private final FakeAgentRepository agentRepository = new FakeAgentRepository();
    private final FakeConfigRepository configRepository = new FakeConfigRepository();
    private final AiConfigService aiConfigService = new AiConfigService(configRepository);
    private final AgentManagementService agentManagementService = new AgentManagementService(agentRepository, configRepository, aiConfigService, new StrategyPromptTemplateService(new DefaultResourceLoader()));
    private final WorkshopService workshopService = new WorkshopService(agentManagementService, aiConfigService);

    @Test
    void createsStepAgentAndBindsSelectedMcpToStepToolRoles() {
        AgentManageVO agent = workshopService.createAgent(new WorkshopAgentCreateDTO(
                "News Agent", "查询新闻并发送邮件", "step", "model_default", List.of("mcp_mail", "mcp_mail", "")));

        assertThat(agent.agentType()).isEqualTo("step");
        List<String> toolClientIds = agentRepository.savedFlows.stream()
                .filter(flow -> List.of("inspector", "planner", "runner").contains(flow.clientRole()))
                .map(FlowManageVO::clientId)
                .toList();
        assertThat(toolClientIds).hasSize(3);
        assertThat(configRepository.list(ConfigKind.CONFIG))
                .filteredOn(record -> "mcp".equals(record.getConfigType()))
                .hasSize(3)
                .allSatisfy(record -> assertThat(record.getRefId()).isEqualTo("mcp_mail"))
                .extracting(AiConfigRecord::getContent)
                .containsExactlyInAnyOrderElementsOf(toolClientIds);
    }

    @Test
    void createsLoopAgentAndBindsSelectedMcpToLoopToolRoles() {
        workshopService.createAgent(new WorkshopAgentCreateDTO(
                "Loop Agent", "反复优化输出", "loop", "model_default", List.of("mcp_mail")));

        List<String> toolClientIds = agentRepository.savedFlows.stream()
                .filter(flow -> List.of("analyzer", "performer", "supervisor").contains(flow.clientRole()))
                .map(FlowManageVO::clientId)
                .toList();
        assertThat(configRepository.list(ConfigKind.CONFIG))
                .filteredOn(record -> "mcp".equals(record.getConfigType()))
                .hasSize(3)
                .extracting(AiConfigRecord::getContent)
                .containsExactlyInAnyOrderElementsOf(toolClientIds);
    }

    @Test
    void createsReactAgentAndBindsSelectedMcpToReactToolRoles() {
        workshopService.createAgent(new WorkshopAgentCreateDTO(
                "React Agent", "观察思考执行", "react", "model_default", List.of("mcp_mail")));

        List<String> toolClientIds = agentRepository.savedFlows.stream()
                .filter(flow -> List.of("observer", "reasoner", "actor").contains(flow.clientRole()))
                .map(FlowManageVO::clientId)
                .toList();
        assertThat(configRepository.list(ConfigKind.CONFIG))
                .filteredOn(record -> "mcp".equals(record.getConfigType()))
                .hasSize(3)
                .extracting(AiConfigRecord::getContent)
                .containsExactlyInAnyOrderElementsOf(toolClientIds);
    }

    @Test
    void rejectsUnavailableModel() {
        assertThatThrownBy(() -> workshopService.createAgent(new WorkshopAgentCreateDTO(
                "Bad Model", "desc", "step", "model_missing", List.of("mcp_mail"))))
                .isInstanceOf(AgentManagementException.class)
                .hasMessage("模型不存在或未启用");
    }

    @Test
    void rejectsUnavailableMcp() {
        assertThatThrownBy(() -> workshopService.createAgent(new WorkshopAgentCreateDTO(
                "Bad MCP", "desc", "step", "model_default", List.of("mcp_missing"))))
                .isInstanceOf(AgentManagementException.class)
                .hasMessage("MCP 工具不存在或未启用");
    }

    private static class FakeAgentRepository implements IAgentManagementRepository {
        private final List<AgentManageVO> agents = new ArrayList<>();
        private final List<FlowManageVO> savedFlows = new ArrayList<>();

        @Override
        public List<AgentManageVO> listAgents() {
            return agents;
        }

        @Override
        public AgentManageVO findAgent(String agentId) {
            return agents.stream().filter(agent -> agent.agentId().equals(agentId)).findFirst().orElse(null);
        }

        @Override
        public AgentManageVO saveAgent(AgentManageDTO request) {
            AgentManageVO agent = new AgentManageVO(request.agentId(), request.agentName(), request.agentType(), request.agentDesc(), request.modelId(), request.templateId(), request.status());
            agents.add(agent);
            return agent;
        }

        @Override
        public AgentManageVO updateAgent(String agentId, AgentManageDTO request) {
            return null;
        }

        @Override
        public void updateAgentStatus(String agentId, Integer status) {
        }

        @Override
        public void deleteAgent(String agentId) {
        }

        @Override
        public List<FlowManageVO> listFlows(String agentId) {
            return savedFlows.stream().filter(flow -> flow.agentId().equals(agentId)).toList();
        }

        @Override
        public FlowManageVO findFlow(String agentId, String clientId) {
            return null;
        }

        @Override
        public FlowManageVO saveFlow(FlowManageDTO request) {
            FlowManageVO flow = new FlowManageVO(request.agentId(), request.clientId(), request.clientRole(), request.userPrompt(), request.flowSeq());
            savedFlows.add(flow);
            return flow;
        }

        @Override
        public FlowManageVO updateFlow(String originAgentId, String originClientId, FlowManageDTO request) {
            return null;
        }

        @Override
        public void deleteFlow(String agentId, String clientId) {
        }

        @Override
        public FlowOptionsVO options() {
            return new FlowOptionsVO(List.of(), List.of(), List.of());
        }

        @Override
        public CanvasGraphVO canvasGraph(String agentId) {
            return null;
        }
    }

    private static class FakeConfigRepository implements IAiConfigRepository {
        private final Map<ConfigKind, List<AiConfigRecord>> records = new EnumMap<>(ConfigKind.class);

        FakeConfigRepository() {
            for (ConfigKind kind : ConfigKind.values()) {
                records.put(kind, new ArrayList<>());
            }
            AiConfigRecord model = new AiConfigRecord();
            model.setConfigId("model_default");
            model.setName("Default Model");
            model.setType("model");
            model.setStatus(1);
            records.get(ConfigKind.MODEL).add(model);

            AiConfigRecord mcp = new AiConfigRecord();
            mcp.setConfigId("mcp_mail");
            mcp.setName("Mail");
            mcp.setType("sse");
            mcp.setStatus(1);
            records.get(ConfigKind.MCP).add(mcp);
        }

        @Override
        public AiConfigRecord save(ConfigKind kind, AiConfigRecord record) {
            records.get(kind).add(record);
            return record;
        }

        @Override
        public List<AiConfigRecord> list(ConfigKind kind) {
            return records.get(kind);
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

        @Override
        public AiConfigRecord find(ConfigKind kind, String configId) {
            return records.get(kind).stream().filter(record -> configId.equals(record.getConfigId())).findFirst().orElse(null);
        }
    }
}

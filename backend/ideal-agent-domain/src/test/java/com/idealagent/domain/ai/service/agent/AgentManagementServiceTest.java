package com.idealagent.domain.ai.service.agent;

import com.idealagent.domain.ai.model.dto.AgentManageDTO;
import com.idealagent.domain.ai.model.dto.FlowManageDTO;
import com.idealagent.domain.ai.model.entity.AiConfigRecord;
import com.idealagent.domain.ai.model.enumeration.ConfigKind;
import com.idealagent.domain.ai.model.vo.AgentManageVO;
import com.idealagent.domain.ai.model.vo.CanvasGraphVO;
import com.idealagent.domain.ai.model.vo.FlowManageVO;
import com.idealagent.domain.ai.model.vo.FlowOptionsVO;
import com.idealagent.domain.ai.repository.IAiConfigRepository;
import com.idealagent.domain.ai.repository.IAgentManagementRepository;
import com.idealagent.domain.ai.service.config.AiConfigService;
import org.springframework.core.io.DefaultResourceLoader;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentManagementServiceTest {
    private final FakeRepository repository = new FakeRepository();
    private final FakeConfigRepository configRepository = new FakeConfigRepository();
    private final AgentManagementService service = new AgentManagementService(repository, configRepository, new AiConfigService(configRepository), new StrategyPromptTemplateService(new DefaultResourceLoader()));

    @Test
    void createsAgentWithSupportedType() {
        AgentManageVO agent = service.createAgent(new AgentManageDTO(
                "agent_custom_step", "Custom Step", "step", "desc", "model_default", "", 1));

        assertThat(agent.agentId()).isEqualTo("agent_custom_step");
        assertThat(agent.agentType()).isEqualTo("step");
        assertThat(repository.savedAgents).hasSize(1);
    }

    @Test
    void createsAgentWithGeneratedIdWhenAgentIdIsBlank() {
        AgentManageVO agent = service.createAgent(new AgentManageDTO(
                "", "Custom Step", "step", "desc", "model_default", "", 1));

        assertThat(agent.agentId()).startsWith("agent_");
        assertThat(agent.agentId()).hasSize(14);
        assertThat(agent.agentType()).isEqualTo("step");
        assertThat(repository.savedAgents).hasSize(1);
    }

    @Test
    void createsMiniAgentWorkspaceStyleRuntimeConfigForStrategyRoles() {
        AgentManageVO agent = service.createAgent(new AgentManageDTO(
                "agent_custom_step", "Custom Step", "step", "desc", "model_default", "", 1));

        assertThat(agent.agentId()).isEqualTo("agent_custom_step");
        assertThat(configRepository.list(ConfigKind.CLIENT)).hasSize(4)
                .extracting(AiConfigRecord::getContent)
                .containsExactlyInAnyOrder("inspector", "planner", "runner", "replier");
        assertThat(configRepository.list(ConfigKind.CLIENT)).allSatisfy(client -> {
            assertThat(client.getType()).isEqualTo("work");
            assertThat(client.getRefId()).isEqualTo("model_default");
            assertThat(client.getSecret()).isEqualTo("Default Model");
        });
        assertThat(configRepository.list(ConfigKind.PROMPT)).hasSize(4)
                .extracting(AiConfigRecord::getName)
                .containsExactlyInAnyOrder("inspector_prompt", "planner_prompt", "runner_prompt", "replier_prompt");
        assertThat(configRepository.list(ConfigKind.CONFIG))
                .filteredOn(config -> "prompt".equals(config.getConfigType()))
                .hasSize(4);
        assertThat(configRepository.list(ConfigKind.CONFIG))
                .filteredOn(config -> "advisor".equals(config.getConfigType()))
                .hasSize(4)
                .allSatisfy(config -> assertThat(config.getRefId()).isEqualTo("advisor_memory_default"));
        assertThat(repository.savedFlows).hasSize(4)
                .extracting(FlowManageVO::clientRole)
                .containsExactly("inspector", "planner", "runner", "replier");
        assertThat(repository.savedFlows).extracting(FlowManageVO::flowSeq).containsExactly(1, 2, 3, 4);
        assertThat(repository.savedFlows).allSatisfy(flow -> assertThat(flow.userPrompt()).isNotBlank());
    }

    @Test
    void createsRuntimeConfigFromStrategyPromptTemplatesWhenAvailable() {
        service.createAgent(new AgentManageDTO(
                "agent_template_step", "Template Step", "step", "desc", "model_default", "", 1));

        AiConfigRecord inspectorPrompt = configRepository.list(ConfigKind.PROMPT).stream()
                .filter(prompt -> "inspector_prompt".equals(prompt.getName()))
                .findFirst()
                .orElseThrow();
        assertThat(inspectorPrompt.getContent()).isEqualTo("Template system prompt for Inspector: %s");

        FlowManageVO inspectorFlow = repository.savedFlows.stream()
                .filter(flow -> "inspector".equals(flow.clientRole()))
                .findFirst()
                .orElseThrow();
        assertThat(inspectorFlow.userPrompt()).isEqualTo("Template user prompt for Inspector: 当前角色只处理 inspector 职责；围绕 Agent 目标「desc」输出必要信息；可做边界是完成本角色职责内的分析、决策或交付，禁止越权替代其他流程、调用未配置工具或编造结果。 / demand %s");

        AiConfigRecord plannerPrompt = configRepository.list(ConfigKind.PROMPT).stream()
                .filter(prompt -> "planner_prompt".equals(prompt.getName()))
                .findFirst()
                .orElseThrow();
        assertThat(plannerPrompt.getContent()).contains("You are the planner role for agent Template Step");

        FlowManageVO plannerFlow = repository.savedFlows.stream()
                .filter(flow -> "planner".equals(flow.clientRole()))
                .findFirst()
                .orElseThrow();
        assertThat(plannerFlow.userPrompt()).contains("Execute the planner step for agent goal: desc");
    }

    @Test
    void rejectsAgentCreateWithoutModelBecauseRuntimeClientsNeedModel() {
        assertThatThrownBy(() -> service.createAgent(new AgentManageDTO(
                "agent_no_model", "No Model", "step", "desc", "", "", 1)))
                .isInstanceOf(AgentManagementException.class)
                .hasMessage("Model ID不能为空");
    }

    @Test
    void rejectsUnsupportedAgentType() {
        assertThatThrownBy(() -> service.createAgent(new AgentManageDTO(
                "agent_bad", "Bad", "graph", "desc", "", "", 1)))
                .isInstanceOf(AgentManagementException.class)
                .hasMessageContaining("Agent 类型不支持");
    }

    @Test
    void createsMiniAgentStyleFlow() {
        repository.agents.add(new AgentManageVO("agent_custom_step", "Custom Step", "step", "desc", "", "", 1));

        FlowManageVO flow = service.createFlow(new FlowManageDTO(
                null, null, "agent_custom_step", "client_planner", "planner", "Plan %s", 2));

        assertThat(flow.agentId()).isEqualTo("agent_custom_step");
        assertThat(flow.clientId()).isEqualTo("client_planner");
        assertThat(flow.clientRole()).isEqualTo("planner");
        assertThat(flow.userPrompt()).isEqualTo("Plan %s");
        assertThat(flow.flowSeq()).isEqualTo(2);
        assertThat(repository.savedFlows).hasSize(1);
    }

    @Test
    void rejectsFlowRoleThatDoesNotMatchAgentType() {
        repository.agents.add(new AgentManageVO("agent_custom_step", "Custom Step", "step", "desc", "", "", 1));

        assertThatThrownBy(() -> service.createFlow(new FlowManageDTO(
                null, null, "agent_custom_step", "client_summarizer", "summarizer", "Summarize", 4)))
                .isInstanceOf(AgentManagementException.class)
                .hasMessageContaining("Flow 角色不匹配");
    }

    @Test
    void rejectsFlowSequenceThatDoesNotMatchAgentStrategy() {
        repository.agents.add(new AgentManageVO("agent_custom_step", "Custom Step", "step", "desc", "", "", 1));

        assertThatThrownBy(() -> service.createFlow(new FlowManageDTO(
                null, null, "agent_custom_step", "client_runner", "runner", "Run", 2)))
                .isInstanceOf(AgentManagementException.class)
                .hasMessageContaining("Flow 顺序不匹配");
    }

    private static class FakeRepository implements IAgentManagementRepository {
        private final List<AgentManageVO> agents = new ArrayList<>();
        private final List<AgentManageVO> savedAgents = new ArrayList<>();
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
            AgentManageVO vo = new AgentManageVO(request.agentId(), request.agentName(), request.agentType(), request.agentDesc(), request.modelId(), request.templateId(), request.status());
            savedAgents.add(vo);
            agents.add(vo);
            return vo;
        }

        @Override
        public AgentManageVO updateAgent(String agentId, AgentManageDTO request) {
            return new AgentManageVO(agentId, request.agentName(), request.agentType(), request.agentDesc(), request.modelId(), request.templateId(), request.status());
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
            return savedFlows.stream().filter(flow -> flow.agentId().equals(agentId) && flow.clientId().equals(clientId)).findFirst().orElse(null);
        }

        @Override
        public FlowManageVO saveFlow(FlowManageDTO request) {
            FlowManageVO vo = new FlowManageVO(request.agentId(), request.clientId(), request.clientRole(), request.userPrompt(), request.flowSeq());
            savedFlows.add(vo);
            return vo;
        }

        @Override
        public FlowManageVO updateFlow(String originAgentId, String originClientId, FlowManageDTO request) {
            return new FlowManageVO(request.agentId(), request.clientId(), request.clientRole(), request.userPrompt(), request.flowSeq());
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
            List<AiConfigRecord> list = records.get(kind);
            list.removeIf(item -> item.getConfigId().equals(record.getConfigId()));
            list.add(record);
            return record;
        }

        @Override
        public void updateStatus(ConfigKind kind, String configId, Integer status) {
        }

        @Override
        public void delete(ConfigKind kind, String configId) {
            records.get(kind).removeIf(item -> item.getConfigId().equals(configId));
        }
    }
}

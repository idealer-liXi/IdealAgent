package com.idealagent.domain.ai.service.agent;

import com.idealagent.domain.ai.model.dto.AgentManageDTO;
import com.idealagent.domain.ai.model.dto.FlowManageDTO;
import com.idealagent.domain.ai.model.vo.AgentManageVO;
import com.idealagent.domain.ai.model.vo.FlowManageVO;
import com.idealagent.domain.ai.model.vo.FlowOptionsVO;
import com.idealagent.domain.ai.repository.IAgentManagementRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentManagementServiceTest {
    private final FakeRepository repository = new FakeRepository();
    private final AgentManagementService service = new AgentManagementService(repository);

    @Test
    void createsAgentWithSupportedType() {
        AgentManageVO agent = service.createAgent(new AgentManageDTO(
                "agent_custom_step", "Custom Step", "step", "desc", "", "", 1));

        assertThat(agent.agentId()).isEqualTo("agent_custom_step");
        assertThat(agent.agentType()).isEqualTo("step");
        assertThat(repository.savedAgents).hasSize(1);
    }

    @Test
    void rejectsUnsupportedAgentType() {
        assertThatThrownBy(() -> service.createAgent(new AgentManageDTO(
                "agent_bad", "Bad", "graph", "desc", "", "", 1)))
                .isInstanceOf(AgentManagementException.class)
                .hasMessageContaining("Agent 类型不支持");
    }

    @Test
    void createsFlowAndPromptBindingForCompatibleRole() {
        repository.agents.add(new AgentManageVO("agent_custom_step", "Custom Step", "step", "desc", "", "", 1));

        FlowManageVO flow = service.createFlow(new FlowManageDTO(
                "flow_custom_planner", "agent_custom_step", "client_default_chat", "planner", 2, "prompt_step_planner", 1));

        assertThat(flow.flowId()).isEqualTo("flow_custom_planner");
        assertThat(flow.promptId()).isEqualTo("prompt_step_planner");
        assertThat(repository.savedFlows).hasSize(1);
    }

    @Test
    void rejectsFlowRoleThatDoesNotMatchAgentType() {
        repository.agents.add(new AgentManageVO("agent_custom_step", "Custom Step", "step", "desc", "", "", 1));

        assertThatThrownBy(() -> service.createFlow(new FlowManageDTO(
                "flow_bad", "agent_custom_step", "client_default_chat", "summarizer", 1, "prompt_loop", 1)))
                .isInstanceOf(AgentManagementException.class)
                .hasMessageContaining("Flow 角色不匹配");
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
        public FlowManageVO findFlow(String flowId) {
            return savedFlows.stream().filter(flow -> flow.flowId().equals(flowId)).findFirst().orElse(null);
        }

        @Override
        public FlowManageVO saveFlow(FlowManageDTO request) {
            FlowManageVO vo = new FlowManageVO(request.flowId(), request.agentId(), request.clientId(), request.roleType(), request.sortOrder(), request.promptId(), "prompt", request.status());
            savedFlows.add(vo);
            return vo;
        }

        @Override
        public FlowManageVO updateFlow(String flowId, FlowManageDTO request) {
            return new FlowManageVO(flowId, request.agentId(), request.clientId(), request.roleType(), request.sortOrder(), request.promptId(), "prompt", request.status());
        }

        @Override
        public void updateFlowStatus(String flowId, Integer status) {
        }

        @Override
        public void deleteFlow(String flowId) {
        }

        @Override
        public FlowOptionsVO options() {
            return new FlowOptionsVO(List.of(), List.of(), List.of());
        }
    }
}

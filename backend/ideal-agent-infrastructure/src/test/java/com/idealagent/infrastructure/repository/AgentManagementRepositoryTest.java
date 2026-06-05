package com.idealagent.infrastructure.repository;

import com.idealagent.domain.ai.model.dto.AgentManageDTO;
import com.idealagent.domain.ai.model.dto.FlowManageDTO;
import com.idealagent.domain.ai.model.vo.AgentManageVO;
import com.idealagent.domain.ai.model.vo.FlowManageVO;
import com.idealagent.infrastructure.persistent.dao.IAiAgentDao;
import com.idealagent.infrastructure.persistent.dao.IAiConfigDao;
import com.idealagent.infrastructure.persistent.dao.IAiFlowDao;
import com.idealagent.infrastructure.persistent.po.AiAgent;
import com.idealagent.infrastructure.persistent.po.AiConfigData;
import com.idealagent.infrastructure.persistent.po.AiFlow;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentManagementRepositoryTest {
    private final IAiAgentDao agentDao = mock(IAiAgentDao.class);
    private final IAiFlowDao flowDao = mock(IAiFlowDao.class);
    private final IAiConfigDao configDao = mock(IAiConfigDao.class);
    private final AgentManagementRepository repository = new AgentManagementRepository(agentDao, flowDao, configDao);

    @Test
    void savesAgentAndMapsResponse() {
        AgentManageVO agent = repository.saveAgent(new AgentManageDTO("agent_custom_step", "Custom Step", "step", "desc", "model", "template", 1));

        assertThat(agent.agentId()).isEqualTo("agent_custom_step");
        assertThat(agent.agentName()).isEqualTo("Custom Step");
        verify(agentDao).insert(any(AiAgent.class));
    }

    @Test
    void savesFlowAndPromptBinding() {
        FlowManageVO flow = repository.saveFlow(new FlowManageDTO("flow_custom_planner", "agent_custom_step", "client_default_chat", "planner", 2, "prompt_step_planner", 1));

        assertThat(flow.flowId()).isEqualTo("flow_custom_planner");
        assertThat(flow.promptId()).isEqualTo("prompt_step_planner");
        verify(flowDao).insert(any(AiFlow.class));
        var captor = forClass(AiConfigData.class);
        verify(configDao).deleteConfigByOwner("flow_custom_planner", "flow", "prompt");
        verify(configDao).insertConfig(captor.capture());
        assertThat(captor.getValue().getConfigId()).isEqualTo("config_flow_custom_planner_prompt");
        assertThat(captor.getValue().getOwnerType()).isEqualTo("flow");
        assertThat(captor.getValue().getConfigType()).isEqualTo("prompt");
        assertThat(captor.getValue().getRefId()).isEqualTo("prompt_step_planner");
    }

    @Test
    void listsFlowsWithPromptBinding() {
        AiFlow po = new AiFlow();
        po.setFlowId("flow_custom_planner");
        po.setAgentId("agent_custom_step");
        po.setClientId("client_default_chat");
        po.setRoleType("planner");
        po.setSortOrder(2);
        po.setFlowStatus(1);
        po.setPromptId("prompt_step_planner");
        po.setPromptContent("Plan prompt");
        when(flowDao.listByAgentId("agent_custom_step")).thenReturn(List.of(po));

        List<FlowManageVO> flows = repository.listFlows("agent_custom_step");

        assertThat(flows).hasSize(1);
        assertThat(flows.get(0).promptId()).isEqualTo("prompt_step_planner");
        assertThat(flows.get(0).promptContent()).isEqualTo("Plan prompt");
    }
}

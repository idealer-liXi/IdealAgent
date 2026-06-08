package com.idealagent.infrastructure.repository;

import com.idealagent.domain.ai.model.entity.WorkAgent;
import com.idealagent.domain.ai.model.vo.AiFlowVO;
import com.idealagent.infrastructure.persistent.dao.IAiAgentDao;
import com.idealagent.infrastructure.persistent.dao.IAiFlowDao;
import com.idealagent.infrastructure.persistent.po.AiAgent;
import com.idealagent.infrastructure.persistent.po.AiFlow;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WorkAgentRepositoryTest {
    private final IAiAgentDao agentDao = mock(IAiAgentDao.class);
    private final IAiFlowDao flowDao = mock(IAiFlowDao.class);
    private final WorkAgentRepository repository = new WorkAgentRepository(agentDao, flowDao);

    @Test
    void findsAgentAndExecuteType() {
        AiAgent po = new AiAgent();
        po.setAgentId("agent_default_step");
        po.setAgentType("step");
        po.setAgentDesc("默认任务执行智能体");
        po.setAgentStatus(1);
        when(agentDao.queryByAgentId("agent_default_step")).thenReturn(po);

        WorkAgent agent = repository.findAgent("agent_default_step");

        assertThat(agent.getAgentType()).isEqualTo("step");
        assertThat(repository.findExecuteType("agent_default_step")).isEqualTo("step");
    }

    @Test
    void listsFlowMapByRoleWithUserPrompt() {
        AiFlow flow = new AiFlow();
        flow.setAgentId("agent_default_step");
        flow.setClientId("client_default_chat");
        flow.setClientRole("planner");
        flow.setFlowSeq(2);
        flow.setUserPrompt("plan %s %s");
        when(flowDao.listEnabledByAgentId("agent_default_step")).thenReturn(List.of(flow));

        Map<String, AiFlowVO> flowMap = repository.listFlowMap("agent_default_step");

        assertThat(flowMap).containsOnlyKeys("planner");
        assertThat(flowMap.get("planner").getUserPrompt()).isEqualTo("plan %s %s");
        assertThat(flowMap.get("planner").getFlowSeq()).isEqualTo(2);
    }

    @Test
    void listsEnabledWorkAgentsForFrontendSelection() {
        AiAgent step = new AiAgent();
        step.setAgentId("agent_default_step");
        step.setAgentName("默认 Step Agent");
        step.setAgentType("step");
        step.setAgentDesc("Step desc");
        step.setAgentStatus(1);
        AiAgent loop = new AiAgent();
        loop.setAgentId("agent_default_loop");
        loop.setAgentName("默认 Loop Agent");
        loop.setAgentType("loop");
        loop.setAgentDesc("Loop desc");
        loop.setAgentStatus(1);
        when(agentDao.listEnabledWorkAgents()).thenReturn(List.of(step, loop));

        List<WorkAgent> agents = repository.listEnabledAgents();

        assertThat(agents).extracting(WorkAgent::getAgentId).containsExactly("agent_default_step", "agent_default_loop");
        assertThat(agents).extracting(WorkAgent::getAgentName).containsExactly("默认 Step Agent", "默认 Loop Agent");
    }
}

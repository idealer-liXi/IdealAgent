package com.idealagent.domain.ai.repository;

import com.idealagent.domain.ai.model.dto.AgentManageDTO;
import com.idealagent.domain.ai.model.dto.FlowManageDTO;
import com.idealagent.domain.ai.model.vo.AgentManageVO;
import com.idealagent.domain.ai.model.vo.FlowManageVO;
import com.idealagent.domain.ai.model.vo.FlowOptionsVO;

import java.util.List;

public interface IAgentManagementRepository {
    List<AgentManageVO> listAgents();

    AgentManageVO findAgent(String agentId);

    AgentManageVO saveAgent(AgentManageDTO request);

    AgentManageVO updateAgent(String agentId, AgentManageDTO request);

    void updateAgentStatus(String agentId, Integer status);

    void deleteAgent(String agentId);

    List<FlowManageVO> listFlows(String agentId);

    FlowManageVO findFlow(String flowId);

    FlowManageVO saveFlow(FlowManageDTO request);

    FlowManageVO updateFlow(String flowId, FlowManageDTO request);

    void updateFlowStatus(String flowId, Integer status);

    void deleteFlow(String flowId);

    FlowOptionsVO options();
}

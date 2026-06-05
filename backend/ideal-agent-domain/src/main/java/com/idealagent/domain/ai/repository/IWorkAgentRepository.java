package com.idealagent.domain.ai.repository;

import com.idealagent.domain.ai.model.entity.WorkAgent;
import com.idealagent.domain.ai.model.vo.AiFlowVO;

import java.util.List;
import java.util.Map;

public interface IWorkAgentRepository {
    WorkAgent findAgent(String agentId);

    List<WorkAgent> listEnabledAgents();

    String findExecuteType(String agentId);

    Map<String, AiFlowVO> listFlowMap(String agentId);
}

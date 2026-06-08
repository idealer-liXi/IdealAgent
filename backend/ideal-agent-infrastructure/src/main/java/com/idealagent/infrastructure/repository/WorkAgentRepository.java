package com.idealagent.infrastructure.repository;

import com.idealagent.domain.ai.model.entity.WorkAgent;
import com.idealagent.domain.ai.model.vo.AiFlowVO;
import com.idealagent.domain.ai.repository.IWorkAgentRepository;
import com.idealagent.infrastructure.persistent.dao.IAiAgentDao;
import com.idealagent.infrastructure.persistent.dao.IAiFlowDao;
import com.idealagent.infrastructure.persistent.po.AiAgent;
import com.idealagent.infrastructure.persistent.po.AiFlow;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class WorkAgentRepository implements IWorkAgentRepository {
    private final IAiAgentDao agentDao;
    private final IAiFlowDao flowDao;

    public WorkAgentRepository(IAiAgentDao agentDao, IAiFlowDao flowDao) {
        this.agentDao = agentDao;
        this.flowDao = flowDao;
    }

    @Override
    public WorkAgent findAgent(String agentId) {
        return toAgent(agentDao.queryByAgentId(agentId));
    }

    @Override
    public List<WorkAgent> listEnabledAgents() {
        return agentDao.listEnabledWorkAgents().stream().map(this::toAgent).toList();
    }

    @Override
    public String findExecuteType(String agentId) {
        WorkAgent agent = findAgent(agentId);
        return agent == null ? null : agent.getAgentType();
    }

    @Override
    public Map<String, AiFlowVO> listFlowMap(String agentId) {
        Map<String, AiFlowVO> flowMap = new LinkedHashMap<>();
        for (AiFlow flow : flowDao.listEnabledByAgentId(agentId)) {
            AiFlowVO vo = new AiFlowVO();
            vo.setAgentId(flow.getAgentId());
            vo.setClientId(flow.getClientId());
            vo.setClientRole(flow.getClientRole());
            vo.setUserPrompt(flow.getUserPrompt());
            vo.setFlowSeq(flow.getFlowSeq());
            flowMap.put(flow.getClientRole(), vo);
        }
        return flowMap;
    }

    private WorkAgent toAgent(AiAgent po) {
        if (po == null) {
            return null;
        }
        WorkAgent agent = new WorkAgent();
        agent.setAgentId(po.getAgentId());
        agent.setAgentName(po.getAgentName());
        agent.setAgentType(po.getAgentType());
        agent.setAgentDesc(po.getAgentDesc());
        agent.setStatus(po.getAgentStatus());
        return agent;
    }
}

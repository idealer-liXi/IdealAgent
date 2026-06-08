package com.idealagent.infrastructure.repository;

import com.idealagent.domain.ai.model.dto.AgentManageDTO;
import com.idealagent.domain.ai.model.dto.FlowManageDTO;
import com.idealagent.domain.ai.model.vo.AgentManageVO;
import com.idealagent.domain.ai.model.vo.AiConfigRecordVO;
import com.idealagent.domain.ai.model.vo.CanvasGraphVO;
import com.idealagent.domain.ai.model.vo.FlowManageVO;
import com.idealagent.domain.ai.model.vo.FlowOptionsVO;
import com.idealagent.domain.ai.repository.IAgentManagementRepository;
import com.idealagent.infrastructure.persistent.dao.IAiAgentDao;
import com.idealagent.infrastructure.persistent.dao.IAiConfigDao;
import com.idealagent.infrastructure.persistent.dao.IAiFlowDao;
import com.idealagent.infrastructure.persistent.po.AiAgent;
import com.idealagent.infrastructure.persistent.po.AiConfigData;
import com.idealagent.infrastructure.persistent.po.AiFlow;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class AgentManagementRepository implements IAgentManagementRepository {
    private final IAiAgentDao agentDao;
    private final IAiFlowDao flowDao;
    private final IAiConfigDao configDao;

    public AgentManagementRepository(IAiAgentDao agentDao, IAiFlowDao flowDao, IAiConfigDao configDao) {
        this.agentDao = agentDao;
        this.flowDao = flowDao;
        this.configDao = configDao;
    }

    @Override
    public List<AgentManageVO> listAgents() {
        return agentDao.listAll().stream().map(this::toAgentVo).toList();
    }

    @Override
    public AgentManageVO findAgent(String agentId) {
        return toAgentVo(agentDao.queryByAgentId(agentId));
    }

    @Override
    public AgentManageVO saveAgent(AgentManageDTO request) {
        agentDao.insert(toAgentPo(request));
        return toAgentVo(toAgentPo(request));
    }

    @Override
    public AgentManageVO updateAgent(String agentId, AgentManageDTO request) {
        agentDao.update(toAgentPo(request));
        return toAgentVo(toAgentPo(request));
    }

    @Override
    public void updateAgentStatus(String agentId, Integer status) {
        agentDao.updateStatus(agentId, status);
    }

    @Override
    public void deleteAgent(String agentId) {
        agentDao.delete(agentId);
    }

    @Override
    public List<FlowManageVO> listFlows(String agentId) {
        return flowDao.listByAgentId(agentId).stream().map(this::toFlowVo).toList();
    }

    @Override
    public FlowManageVO findFlow(String agentId, String clientId) {
        return toFlowVo(flowDao.queryByAgentIdAndClientId(agentId, clientId));
    }

    @Override
    public FlowManageVO saveFlow(FlowManageDTO request) {
        flowDao.insert(toFlowPo(request));
        return toFlowVo(toFlowPo(request));
    }

    @Override
    public FlowManageVO updateFlow(String originAgentId, String originClientId, FlowManageDTO request) {
        flowDao.update(originAgentId, originClientId, toFlowPo(request));
        return toFlowVo(toFlowPo(request));
    }

    @Override
    public void deleteFlow(String agentId, String clientId) {
        flowDao.deleteByAgentIdAndClientId(agentId, clientId);
    }

    @Override
    public FlowOptionsVO options() {
        return new FlowOptionsVO(
                configDao.listClients().stream().map(this::toConfigVo).toList(),
                configDao.listPrompts().stream().map(this::toConfigVo).toList(),
                configDao.listMcps().stream().map(this::toConfigVo).toList());
    }

    @Override
    public CanvasGraphVO canvasGraph(String agentId) {
        return new CanvasGraphVO(
                findAgent(agentId),
                listFlows(agentId),
                configDao.listClients().stream().map(this::toCanvasMap).toList(),
                configDao.listModels().stream().map(this::toCanvasMap).toList(),
                configDao.listApis().stream().map(this::toCanvasMap).toList(),
                configDao.listPrompts().stream().map(this::toCanvasMap).toList(),
                configDao.listAdvisors().stream().map(this::toCanvasMap).toList(),
                configDao.listMcps().stream().map(this::toCanvasMap).toList(),
                configDao.listConfigs().stream().map(this::toCanvasMap).toList());
    }

    private AiAgent toAgentPo(AgentManageDTO request) {
        AiAgent agent = new AiAgent();
        agent.setAgentId(request.agentId());
        agent.setAgentName(request.agentName());
        agent.setAgentType(request.agentType());
        agent.setAgentDesc(request.agentDesc());
        agent.setModelId(request.modelId());
        agent.setTemplateId(request.templateId());
        agent.setAgentStatus(request.status());
        agent.setAgentFrom(0L);
        return agent;
    }

    private AgentManageVO toAgentVo(AiAgent agent) {
        if (agent == null) {
            return null;
        }
        return new AgentManageVO(agent.getAgentId(), agent.getAgentName(), agent.getAgentType(), agent.getAgentDesc(), agent.getModelId(), agent.getTemplateId(), agent.getAgentStatus());
    }

    private AiFlow toFlowPo(FlowManageDTO request) {
        AiFlow flow = new AiFlow();
        flow.setAgentId(request.agentId());
        flow.setClientId(request.clientId());
        flow.setClientRole(request.clientRole());
        flow.setUserPrompt(request.userPrompt());
        flow.setFlowSeq(request.flowSeq());
        return flow;
    }

    private FlowManageVO toFlowVo(AiFlow flow) {
        if (flow == null) {
            return null;
        }
        return new FlowManageVO(flow.getAgentId(), flow.getClientId(), flow.getClientRole(), flow.getUserPrompt(), flow.getFlowSeq());
    }

    private AiConfigRecordVO toConfigVo(AiConfigData record) {
        return new AiConfigRecordVO(record.getConfigId(), record.getName(), record.getType(), record.getContent(), record.getSecret(), record.getRefId(), record.getStatus(), record.getOwnerId(), record.getOwnerType(), record.getConfigType(), record.getCreateTime(), record.getUpdateTime());
    }

    private Map<String, Object> toCanvasMap(AiConfigData record) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("configId", record.getConfigId());
        map.put("name", record.getName());
        map.put("type", record.getType());
        map.put("content", record.getContent());
        map.put("secret", record.getSecret());
        map.put("refId", record.getRefId());
        map.put("status", record.getStatus());
        map.put("ownerId", record.getOwnerId());
        map.put("ownerType", record.getOwnerType());
        map.put("configType", record.getConfigType());
        return map;
    }

}

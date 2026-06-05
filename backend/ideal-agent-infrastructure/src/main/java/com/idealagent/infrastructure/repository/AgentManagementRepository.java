package com.idealagent.infrastructure.repository;

import com.idealagent.domain.ai.model.dto.AgentManageDTO;
import com.idealagent.domain.ai.model.dto.FlowManageDTO;
import com.idealagent.domain.ai.model.vo.AgentManageVO;
import com.idealagent.domain.ai.model.vo.AiConfigRecordVO;
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
import org.springframework.util.StringUtils;

import java.util.List;

@Repository
public class AgentManagementRepository implements IAgentManagementRepository {
    private static final String FLOW_OWNER = "flow";
    private static final String PROMPT_CONFIG = "prompt";
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
    public FlowManageVO findFlow(String flowId) {
        return toFlowVo(flowDao.queryByFlowId(flowId));
    }

    @Override
    public FlowManageVO saveFlow(FlowManageDTO request) {
        flowDao.insert(toFlowPo(request));
        savePromptBinding(request.flowId(), request.promptId());
        return toFlowVo(toFlowPo(request));
    }

    @Override
    public FlowManageVO updateFlow(String flowId, FlowManageDTO request) {
        flowDao.update(toFlowPo(request));
        savePromptBinding(flowId, request.promptId());
        return toFlowVo(toFlowPo(request));
    }

    @Override
    public void updateFlowStatus(String flowId, Integer status) {
        flowDao.updateStatus(flowId, status);
    }

    @Override
    public void deleteFlow(String flowId) {
        configDao.deleteConfigByOwner(flowId, FLOW_OWNER, PROMPT_CONFIG);
        flowDao.delete(flowId);
    }

    @Override
    public FlowOptionsVO options() {
        return new FlowOptionsVO(
                configDao.listClients().stream().map(this::toConfigVo).toList(),
                configDao.listPrompts().stream().map(this::toConfigVo).toList(),
                configDao.listMcps().stream().map(this::toConfigVo).toList());
    }

    private void savePromptBinding(String flowId, String promptId) {
        configDao.deleteConfigByOwner(flowId, FLOW_OWNER, PROMPT_CONFIG);
        if (!StringUtils.hasText(promptId)) {
            return;
        }
        AiConfigData binding = new AiConfigData();
        binding.setConfigId("config_" + flowId + "_prompt");
        binding.setContent(flowId);
        binding.setOwnerType(FLOW_OWNER);
        binding.setConfigType(PROMPT_CONFIG);
        binding.setRefId(promptId);
        binding.setStatus(1);
        configDao.insertConfig(binding);
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
        flow.setFlowId(request.flowId());
        flow.setAgentId(request.agentId());
        flow.setClientId(request.clientId());
        flow.setRoleType(request.roleType());
        flow.setSortOrder(request.sortOrder());
        flow.setFlowStatus(request.status());
        flow.setPromptId(request.promptId());
        return flow;
    }

    private FlowManageVO toFlowVo(AiFlow flow) {
        if (flow == null) {
            return null;
        }
        return new FlowManageVO(flow.getFlowId(), flow.getAgentId(), flow.getClientId(), flow.getRoleType(), flow.getSortOrder(), flow.getPromptId(), flow.getPromptContent(), flow.getFlowStatus());
    }

    private AiConfigRecordVO toConfigVo(AiConfigData record) {
        return new AiConfigRecordVO(record.getConfigId(), record.getName(), record.getType(), record.getContent(), record.getSecret(), record.getRefId(), record.getStatus(), record.getOwnerId(), record.getOwnerType(), record.getConfigType(), record.getCreateTime(), record.getUpdateTime());
    }
}

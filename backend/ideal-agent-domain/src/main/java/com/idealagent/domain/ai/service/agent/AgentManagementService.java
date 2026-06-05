package com.idealagent.domain.ai.service.agent;

import com.idealagent.domain.ai.model.dto.AgentManageDTO;
import com.idealagent.domain.ai.model.dto.FlowManageDTO;
import com.idealagent.domain.ai.model.vo.AgentManageVO;
import com.idealagent.domain.ai.model.vo.FlowManageVO;
import com.idealagent.domain.ai.model.vo.FlowOptionsVO;
import com.idealagent.domain.ai.repository.IAgentManagementRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AgentManagementService {
    private static final Set<String> AGENT_TYPES = Set.of("step", "loop", "react");
    private static final Map<String, Set<String>> ROLE_MAP = Map.of(
            "step", Set.of("inspector", "planner", "runner", "replier"),
            "loop", Set.of("analyzer", "performer", "supervisor", "summarizer"),
            "react", Set.of("observer", "reasoner", "actor", "evaluator"));
    private final IAgentManagementRepository repository;

    public AgentManagementService(IAgentManagementRepository repository) {
        this.repository = repository;
    }

    public List<AgentManageVO> listAgents() {
        return repository.listAgents();
    }

    public AgentManageVO createAgent(AgentManageDTO request) {
        validateAgent(request.agentId(), request.agentName(), request.agentType(), request.status());
        return repository.saveAgent(normalizeAgent(request));
    }

    public AgentManageVO updateAgent(String agentId, AgentManageDTO request) {
        validateId(agentId, "Agent ID不能为空");
        validateAgent(agentId, request.agentName(), request.agentType(), request.status());
        return repository.updateAgent(agentId, normalizeAgent(new AgentManageDTO(agentId, request.agentName(), request.agentType(), request.agentDesc(), request.modelId(), request.templateId(), request.status())));
    }

    public void updateAgentStatus(String agentId, Integer status) {
        validateId(agentId, "Agent ID不能为空");
        validateStatus(status);
        repository.updateAgentStatus(agentId, status);
    }

    public void deleteAgent(String agentId) {
        validateId(agentId, "Agent ID不能为空");
        if (!repository.listFlows(agentId).isEmpty()) {
            throw new AgentManagementException("Agent 存在 Flow，不能删除");
        }
        repository.deleteAgent(agentId);
    }

    public List<FlowManageVO> listFlows(String agentId) {
        validateId(agentId, "Agent ID不能为空");
        return repository.listFlows(agentId);
    }

    public FlowManageVO createFlow(FlowManageDTO request) {
        validateFlow(request.flowId(), request);
        return repository.saveFlow(normalizeFlow(request));
    }

    public FlowManageVO updateFlow(String flowId, FlowManageDTO request) {
        validateId(flowId, "Flow ID不能为空");
        validateFlow(flowId, request);
        return repository.updateFlow(flowId, normalizeFlow(new FlowManageDTO(flowId, request.agentId(), request.clientId(), request.roleType(), request.sortOrder(), request.promptId(), request.status())));
    }

    public void updateFlowStatus(String flowId, Integer status) {
        validateId(flowId, "Flow ID不能为空");
        validateStatus(status);
        repository.updateFlowStatus(flowId, status);
    }

    public void deleteFlow(String flowId) {
        validateId(flowId, "Flow ID不能为空");
        repository.deleteFlow(flowId);
    }

    public FlowOptionsVO options() {
        return repository.options();
    }

    private void validateAgent(String agentId, String agentName, String agentType, Integer status) {
        validateId(agentId, "Agent ID不能为空");
        if (!StringUtils.hasText(agentName)) {
            throw new AgentManagementException("Agent 名称不能为空");
        }
        if (!AGENT_TYPES.contains(normalize(agentType))) {
            throw new AgentManagementException("Agent 类型不支持");
        }
        validateStatus(status == null ? 1 : status);
    }

    private void validateFlow(String flowId, FlowManageDTO request) {
        validateId(flowId, "Flow ID不能为空");
        validateId(request.agentId(), "Agent ID不能为空");
        validateId(request.clientId(), "Client ID不能为空");
        if (request.sortOrder() == null || request.sortOrder() < 1) {
            throw new AgentManagementException("Flow 顺序不正确");
        }
        validateStatus(request.status() == null ? 1 : request.status());
        AgentManageVO agent = repository.findAgent(request.agentId());
        if (agent == null) {
            throw new AgentManagementException("Agent 不存在");
        }
        String roleType = normalize(request.roleType());
        Set<String> allowedRoles = ROLE_MAP.get(normalize(agent.agentType()));
        if (allowedRoles == null || !allowedRoles.contains(roleType)) {
            throw new AgentManagementException("Flow 角色不匹配 Agent 类型");
        }
    }

    private AgentManageDTO normalizeAgent(AgentManageDTO request) {
        return new AgentManageDTO(trim(request.agentId()), trim(request.agentName()), normalize(request.agentType()), trim(request.agentDesc()), trim(request.modelId()), trim(request.templateId()), request.status() == null ? 1 : request.status());
    }

    private FlowManageDTO normalizeFlow(FlowManageDTO request) {
        return new FlowManageDTO(trim(request.flowId()), trim(request.agentId()), trim(request.clientId()), normalize(request.roleType()), request.sortOrder(), trim(request.promptId()), request.status() == null ? 1 : request.status());
    }

    private void validateId(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new AgentManagementException(message);
        }
    }

    private void validateStatus(Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new AgentManagementException("状态不正确");
        }
    }

    private String normalize(String value) {
        return trim(value).toLowerCase();
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}

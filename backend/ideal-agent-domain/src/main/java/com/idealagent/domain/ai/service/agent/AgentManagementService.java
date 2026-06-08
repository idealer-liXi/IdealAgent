package com.idealagent.domain.ai.service.agent;

import com.idealagent.domain.ai.model.dto.AgentManageDTO;
import com.idealagent.domain.ai.model.dto.AiConfigRecordDTO;
import com.idealagent.domain.ai.model.dto.FlowManageDTO;
import com.idealagent.domain.ai.model.entity.AiConfigRecord;
import com.idealagent.domain.ai.model.enumeration.ConfigKind;
import com.idealagent.domain.ai.model.vo.AgentManageVO;
import com.idealagent.domain.ai.model.vo.FlowManageVO;
import com.idealagent.domain.ai.model.vo.FlowOptionsVO;
import com.idealagent.domain.ai.repository.IAiConfigRepository;
import com.idealagent.domain.ai.repository.IAgentManagementRepository;
import com.idealagent.domain.ai.service.config.AiConfigService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;

@Service
public class AgentManagementService {
    private static final String DEFAULT_ADVISOR_ID = "advisor_memory_default";
    private static final Set<String> AGENT_TYPES = Set.of("step", "loop", "react");
    private static final Map<String, Set<String>> ROLE_MAP = Map.of(
            "step", Set.of("inspector", "planner", "runner", "replier"),
            "loop", Set.of("analyzer", "performer", "supervisor", "summarizer"),
            "react", Set.of("observer", "reasoner", "actor", "evaluator"));
    private static final Map<String, List<String>> ROLE_ORDER = Map.of(
            "step", List.of("inspector", "planner", "runner", "replier"),
            "loop", List.of("analyzer", "performer", "supervisor", "summarizer"),
            "react", List.of("observer", "reasoner", "actor", "evaluator"));
    private final IAgentManagementRepository repository;
    private final IAiConfigRepository configRepository;
    private final AiConfigService aiConfigService;
    private final StrategyPromptTemplateService templateService;

    public AgentManagementService(IAgentManagementRepository repository, IAiConfigRepository configRepository, AiConfigService aiConfigService, StrategyPromptTemplateService templateService) {
        this.repository = repository;
        this.configRepository = configRepository;
        this.aiConfigService = aiConfigService;
        this.templateService = templateService;
    }

    public List<AgentManageVO> listAgents() {
        return repository.listAgents();
    }

    @Transactional(rollbackFor = Exception.class)
    public AgentManageVO createAgent(AgentManageDTO request) {
        AgentManageDTO normalized = normalizeAgent(new AgentManageDTO(
                StringUtils.hasText(request.agentId()) ? request.agentId() : generateAgentId(),
                request.agentName(), request.agentType(), request.agentDesc(), request.modelId(), request.templateId(), request.status()));
        validateAgent(normalized.agentId(), normalized.agentName(), normalized.agentType(), normalized.status());
        validateId(normalized.modelId(), "Model ID不能为空");
        AgentManageVO agent = repository.saveAgent(normalized);
        createStrategyRuntimeConfig(agent);
        return agent;
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
        FlowManageDTO normalized = normalizeFlow(request);
        validateFlow(normalized);
        return repository.saveFlow(normalized);
    }

    public FlowManageVO updateFlow(FlowManageDTO request) {
        String originAgentId = StringUtils.hasText(request.originAgentId()) ? request.originAgentId().trim() : trim(request.agentId());
        String originClientId = StringUtils.hasText(request.originClientId()) ? request.originClientId().trim() : trim(request.clientId());
        validateId(originAgentId, "Origin Agent ID不能为空");
        validateId(originClientId, "Origin Client ID不能为空");
        FlowManageDTO normalized = normalizeFlow(request);
        validateFlow(normalized);
        return repository.updateFlow(originAgentId, originClientId, normalized);
    }

    public FlowManageVO updateFlow(String ignoredFlowId, FlowManageDTO request) {
        return updateFlow(request);
    }

    public void deleteFlow(String agentId, String clientId) {
        validateId(agentId, "Agent ID不能为空");
        validateId(clientId, "Client ID不能为空");
        repository.deleteFlow(agentId.trim(), clientId.trim());
    }

    public FlowOptionsVO options() {
        return repository.options();
    }

    private void createStrategyRuntimeConfig(AgentManageVO agent) {
        List<String> roles = ROLE_ORDER.get(normalize(agent.agentType()));
        if (roles == null) {
            return;
        }
        String modelName = modelName(agent.modelId());
        for (int index = 0; index < roles.size(); index++) {
            String role = roles.get(index);
            int seq = index + 1;
            String clientId = id("client_");
            String promptId = id("prompt_");
            aiConfigService.create(ConfigKind.CLIENT, new AiConfigRecordDTO(
                    clientId,
                    agent.agentName() + "-" + role,
                    "work",
                    role,
                    modelName,
                    agent.modelId(),
                    1,
                    0L,
                    null,
                    null));
            aiConfigService.create(ConfigKind.PROMPT, new AiConfigRecordDTO(
                    promptId,
                    role + "_prompt",
                    "system",
                    defaultSystemPrompt(agent, role),
                    null,
                    null,
                    1,
                    0L,
                    null,
                    null));
            aiConfigService.create(ConfigKind.CONFIG, binding(clientId, "prompt", promptId));
            aiConfigService.create(ConfigKind.CONFIG, binding(clientId, "advisor", DEFAULT_ADVISOR_ID));
            createFlow(new FlowManageDTO(null, null, agent.agentId(), clientId, role, defaultUserPrompt(agent, role), seq));
        }
    }

    private AiConfigRecordDTO binding(String clientId, String configType, String refId) {
        return new AiConfigRecordDTO("config_" + clientId + "_" + configType + "_" + refId, "", null, clientId, null, refId, 1, 0L, "client", configType);
    }

    private String modelName(String modelId) {
        AiConfigRecord model = configRepository.find(ConfigKind.MODEL, modelId);
        if (model != null && StringUtils.hasText(model.getName())) {
            return model.getName();
        }
        return modelId;
    }

    private String defaultSystemPrompt(AgentManageVO agent, String role) {
        String template = templateService.systemPrompt(agent.agentType(), role);
        if (StringUtils.hasText(template)) {
            return template;
        }
        return "You are the " + role + " role for agent " + agent.agentName() + ". Agent goal: " + trim(agent.agentDesc());
    }

    private String defaultUserPrompt(AgentManageVO agent, String role) {
        String template = templateService.userPrompt(agent.agentType(), role);
        if (StringUtils.hasText(template)) {
            return template.replaceFirst("%s", Matcher.quoteReplacement(roleConstraint(agent, role)));
        }
        return "Execute the " + role + " step for agent goal: " + trim(agent.agentDesc());
    }

    private String roleConstraint(AgentManageVO agent, String role) {
        return "当前角色只处理 " + role + " 职责；围绕 Agent 目标「" + trim(agent.agentDesc()) + "」输出必要信息；可做边界是完成本角色职责内的分析、决策或交付，禁止越权替代其他流程、调用未配置工具或编造结果。";
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

    private void validateFlow(FlowManageDTO request) {
        validateId(request.agentId(), "Agent ID不能为空");
        validateId(request.clientId(), "Client ID不能为空");
        validateId(request.clientRole(), "Client Role不能为空");
        if (!StringUtils.hasText(request.userPrompt())) {
            throw new AgentManagementException("Flow Prompt不能为空");
        }
        if (request.flowSeq() == null || request.flowSeq() < 1) {
            throw new AgentManagementException("Flow 顺序不正确");
        }
        AgentManageVO agent = repository.findAgent(request.agentId());
        if (agent == null) {
            throw new AgentManagementException("Agent 不存在");
        }
        String roleType = normalize(request.clientRole());
        String agentType = normalize(agent.agentType());
        Set<String> allowedRoles = ROLE_MAP.get(agentType);
        if (allowedRoles == null || !allowedRoles.contains(roleType)) {
            throw new AgentManagementException("Flow 角色不匹配 Agent 类型");
        }
        List<String> orderedRoles = ROLE_ORDER.get(agentType);
        if (request.flowSeq() > orderedRoles.size() || !orderedRoles.get(request.flowSeq() - 1).equals(roleType)) {
            throw new AgentManagementException("Flow 顺序不匹配 Agent 策略");
        }
    }

    private AgentManageDTO normalizeAgent(AgentManageDTO request) {
        return new AgentManageDTO(trim(request.agentId()), trim(request.agentName()), normalize(request.agentType()), trim(request.agentDesc()), trim(request.modelId()), trim(request.templateId()), request.status() == null ? 1 : request.status());
    }

    private FlowManageDTO normalizeFlow(FlowManageDTO request) {
        return new FlowManageDTO(trim(request.originAgentId()), trim(request.originClientId()), trim(request.agentId()), trim(request.clientId()), normalize(request.clientRole()), trim(request.userPrompt()), request.flowSeq());
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

    private String generateAgentId() {
        return "agent_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private String id(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}

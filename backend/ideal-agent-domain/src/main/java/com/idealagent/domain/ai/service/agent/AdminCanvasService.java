package com.idealagent.domain.ai.service.agent;

import com.idealagent.domain.ai.model.dto.AiConfigRecordDTO;
import com.idealagent.domain.ai.model.dto.CanvasNodeDTO;
import com.idealagent.domain.ai.model.dto.CanvasRelationDTO;
import com.idealagent.domain.ai.model.dto.CanvasSaveDTO;
import com.idealagent.domain.ai.model.dto.FlowManageDTO;
import com.idealagent.domain.ai.model.entity.AiConfigRecord;
import com.idealagent.domain.ai.model.enumeration.ConfigKind;
import com.idealagent.domain.ai.model.vo.AgentManageVO;
import com.idealagent.domain.ai.model.vo.CanvasGraphVO;
import com.idealagent.domain.ai.model.vo.FlowManageVO;
import com.idealagent.domain.ai.repository.IAiConfigRepository;
import com.idealagent.domain.ai.repository.IAgentManagementRepository;
import com.idealagent.domain.ai.service.config.AiConfigService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AdminCanvasService {
    private static final Set<String> ALLOWED_EDGES = Set.of(
            "agent->client",
            "client->client",
            "client->model",
            "model->api",
            "client->prompt",
            "client->advisor",
            "client->mcp");
    private static final Map<String, List<String>> ROLE_ORDER = Map.of(
            "step", List.of("inspector", "planner", "runner", "replier"),
            "loop", List.of("analyzer", "performer", "supervisor", "summarizer"),
            "react", List.of("observer", "reasoner", "actor", "evaluator"));

    private final IAgentManagementRepository repository;
    private final IAiConfigRepository configRepository;
    private final AgentManagementService agentManagementService;
    private final AiConfigService aiConfigService;

    public AdminCanvasService(IAgentManagementRepository repository, IAiConfigRepository configRepository, AgentManagementService agentManagementService, AiConfigService aiConfigService) {
        this.repository = repository;
        this.configRepository = configRepository;
        this.agentManagementService = agentManagementService;
        this.aiConfigService = aiConfigService;
    }

    public CanvasGraphVO graph(String agentId) {
        validateId(agentId, "Agent ID不能为空");
        return repository.canvasGraph(agentId.trim());
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveCanvas(String agentId, CanvasSaveDTO request) {
        validateId(agentId, "Agent ID不能为空");
        CanvasSaveDTO normalized = request == null ? new CanvasSaveDTO(List.of(), List.of(), List.of()) : request;
        for (CanvasRelationDTO relation : safeList(normalized.deletedRelations())) {
            deleteRelation(relationWithAgent(agentId, relation));
        }
        for (CanvasNodeDTO node : safeList(normalized.nodes())) {
            saveNode(node);
        }
        for (CanvasRelationDTO relation : safeList(normalized.relations())) {
            saveRelation(relationWithAgent(agentId, relation));
        }
    }

    public void saveNode(CanvasNodeDTO request) {
        validateId(request.nodeType(), "Node Type不能为空");
        ConfigKind kind = ConfigKind.from(request.nodeType());
        AiConfigRecordDTO dto = nodeDto(request);
        if (configRepository.find(kind, dto.configId()) == null) {
            aiConfigService.create(kind, dto);
        } else {
            aiConfigService.update(kind, dto.configId(), dto);
        }
    }

    public void saveRelation(CanvasRelationDTO request) {
        validateRelation(request, true);
        String edge = edge(request);
        if ("agent->client".equals(edge) || "client->client".equals(edge)) {
            agentManagementService.createFlow(new FlowManageDTO(null, null, request.agentId(), request.targetId(), request.clientRole(), request.userPrompt(), request.flowSeq()));
            return;
        }
        if ("client->model".equals(edge)) {
            updateRef(ConfigKind.CLIENT, request.sourceId(), request.targetId());
            return;
        }
        if ("model->api".equals(edge)) {
            updateRef(ConfigKind.MODEL, request.sourceId(), request.targetId());
            return;
        }
        if (edge.startsWith("client->")) {
            saveClientBinding(request);
        }
    }

    public void deleteRelation(CanvasRelationDTO request) {
        validateRelation(request, false);
        String edge = edge(request);
        if ("agent->client".equals(edge) || "client->client".equals(edge)) {
            agentManagementService.deleteFlow(request.agentId(), request.targetId());
            return;
        }
        if ("client->model".equals(edge)) {
            updateRef(ConfigKind.CLIENT, request.sourceId(), "");
            return;
        }
        if ("model->api".equals(edge)) {
            updateRef(ConfigKind.MODEL, request.sourceId(), "");
            return;
        }
        if (edge.startsWith("client->")) {
            aiConfigService.delete(ConfigKind.CONFIG, bindingId(request.sourceId(), request.targetType(), request.targetId()));
        }
    }

    private void validateRelation(CanvasRelationDTO request, boolean requireFlowDetails) {
        validateId(request.sourceType(), "Source Type不能为空");
        validateId(request.targetType(), "Target Type不能为空");
        validateId(request.sourceId(), "Source ID不能为空");
        validateId(request.targetId(), "Target ID不能为空");
        String edge = edge(request);
        if (!ALLOWED_EDGES.contains(edge)) {
            throw new AgentManagementException("连接规则不允许");
        }
        if ("agent->client".equals(edge) || "client->client".equals(edge)) {
            if (requireFlowDetails) {
                validateId(request.agentId(), "Agent ID不能为空");
            } else {
                validateId(request.agentId(), "Agent ID不能为空");
            }
        }
        if ("client->advisor".equals(edge) || "client->mcp".equals(edge)) {
            validateChatClientRuntimeBinding(request, edge);
        }
    }

    private void validateChatClientRuntimeBinding(CanvasRelationDTO request, String edge) {
        AiConfigRecord client = configRepository.find(ConfigKind.CLIENT, request.sourceId().trim());
        if (client == null || !"chat".equalsIgnoreCase(client.getType())) {
            return;
        }
        if ("client->mcp".equals(edge)) {
            throw new AgentManagementException("Chat Client 不允许绑定 MCP");
        }
        AiConfigRecord advisor = configRepository.find(ConfigKind.ADVISOR, request.targetId().trim());
        if (advisor != null && "rag".equalsIgnoreCase(advisor.getType())) {
            throw new AgentManagementException("Chat Client 不允许绑定 RAG Advisor");
        }
    }

    private void validateFlowRelation(CanvasRelationDTO request) {
        validateId(request.agentId(), "Agent ID不能为空");
        validateId(request.clientRole(), "Client Role不能为空");
        validateId(request.userPrompt(), "Flow Prompt不能为空");
        if (request.flowSeq() == null || request.flowSeq() < 1) {
            throw new AgentManagementException("Flow 顺序不正确");
        }
        AgentManageVO agent = repository.findAgent(request.agentId().trim());
        if (agent == null) {
            throw new AgentManagementException("Agent 不存在");
        }
        List<String> roles = ROLE_ORDER.get(normalize(agent.agentType()));
        String role = normalize(request.clientRole());
        if (roles == null || !roles.contains(role)) {
            throw new AgentManagementException("Flow 角色不匹配 Agent 类型");
        }
        if (request.flowSeq() > roles.size() || !roles.get(request.flowSeq() - 1).equals(role)) {
            throw new AgentManagementException("Flow 顺序不匹配 Agent 策略");
        }
    }

    private CanvasRelationDTO relationWithAgent(String agentId, CanvasRelationDTO relation) {
        if (relation == null) {
            return new CanvasRelationDTO(null, null, null, null, agentId, null, null, null, null);
        }
        return new CanvasRelationDTO(relation.sourceType(), relation.targetType(), relation.sourceId(), relation.targetId(), agentId, relation.configType(), relation.clientRole(), relation.userPrompt(), relation.flowSeq());
    }

    private <T> List<T> safeList(List<T> value) {
        return value == null ? List.of() : value;
    }

    private void validateId(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new AgentManagementException(message);
        }
    }

    private void saveClientBinding(CanvasRelationDTO request) {
        AiConfigRecordDTO dto = new AiConfigRecordDTO(
                bindingId(request.sourceId(), request.targetType(), request.targetId()),
                "",
                null,
                request.sourceId(),
                null,
                request.targetId(),
                1,
                0L,
                "client",
                normalize(request.targetType()));
        if (configRepository.find(ConfigKind.CONFIG, dto.configId()) == null) {
            aiConfigService.create(ConfigKind.CONFIG, dto);
        } else {
            aiConfigService.update(ConfigKind.CONFIG, dto.configId(), dto);
        }
    }

    private void updateRef(ConfigKind kind, String sourceId, String refId) {
        AiConfigRecord record = configRepository.find(kind, sourceId.trim());
        if (record == null) {
            throw new AgentManagementException("配置不存在");
        }
        aiConfigService.update(kind, sourceId.trim(), configDto(record, refId));
    }

    private AiConfigRecordDTO nodeDto(CanvasNodeDTO request) {
        Map<String, Object> payload = request.payload() == null ? Map.of() : request.payload();
        String id = string(payload.get("configId"), request.id());
        return new AiConfigRecordDTO(
                id,
                string(payload.get("name"), id),
                string(payload.get("type"), normalize(request.nodeType())),
                string(payload.get("content"), ""),
                string(payload.get("secret"), ""),
                string(payload.get("refId"), ""),
                integer(payload.get("status"), 1),
                longValue(payload.get("ownerId"), 0L),
                string(payload.get("ownerType"), null),
                string(payload.get("configType"), null));
    }

    private AiConfigRecordDTO configDto(AiConfigRecord record, String refId) {
        return new AiConfigRecordDTO(
                record.getConfigId(),
                record.getName(),
                record.getType(),
                record.getContent(),
                record.getSecret(),
                refId,
                record.getStatus(),
                record.getOwnerId(),
                record.getOwnerType(),
                record.getConfigType());
    }

    private String bindingId(String clientId, String configType, String refId) {
        return "config_" + clientId.trim() + "_" + normalize(configType) + "_" + refId.trim();
    }

    private String edge(CanvasRelationDTO request) {
        return normalize(request.sourceType()) + "->" + normalize(request.targetType());
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private String string(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String stringValue = value.toString();
        return stringValue.isBlank() ? fallback : stringValue;
    }

    private Integer integer(Object value, Integer fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String string && !string.isBlank()) {
            return Integer.parseInt(string);
        }
        return fallback;
    }

    private Long longValue(Object value, Long fallback) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String string && !string.isBlank()) {
            return Long.parseLong(string);
        }
        return fallback;
    }
}

package com.idealagent.domain.ai.service.workshop;

import com.idealagent.domain.ai.model.dto.AgentManageDTO;
import com.idealagent.domain.ai.model.dto.AiConfigRecordDTO;
import com.idealagent.domain.ai.model.dto.WorkshopAgentCreateDTO;
import com.idealagent.domain.ai.model.enumeration.ConfigKind;
import com.idealagent.domain.ai.model.vo.AgentManageVO;
import com.idealagent.domain.ai.model.vo.AiConfigRecordVO;
import com.idealagent.domain.ai.model.vo.FlowManageVO;
import com.idealagent.domain.ai.service.agent.AgentManagementException;
import com.idealagent.domain.ai.service.agent.AgentManagementService;
import com.idealagent.domain.ai.service.config.AiConfigService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Service
public class WorkshopService {
    private static final Map<String, List<String>> MCP_BINDING_ROLES = Map.of(
            "step", List.of("inspector", "planner", "runner"),
            "loop", List.of("analyzer", "performer", "supervisor"),
            "react", List.of("observer", "reasoner", "actor"));

    private final AgentManagementService agentManagementService;
    private final AiConfigService aiConfigService;

    public WorkshopService(AgentManagementService agentManagementService, AiConfigService aiConfigService) {
        this.agentManagementService = agentManagementService;
        this.aiConfigService = aiConfigService;
    }

    @Transactional(rollbackFor = Exception.class)
    public AgentManageVO createAgent(WorkshopAgentCreateDTO request) {
        validateModel(request.modelId());
        validateMcps(request.mcpIdList());
        AgentManageVO agent = agentManagementService.createAgent(new AgentManageDTO(
                "",
                trim(request.agentName()),
                trim(request.strategy()),
                trim(request.agentDesc()),
                trim(request.modelId()),
                "",
                1));
        bindSelectedMcps(agent, request.mcpIdList());
        return agent;
    }

    private void bindSelectedMcps(AgentManageVO agent, List<String> mcpIds) {
        if (mcpIds == null || mcpIds.isEmpty()) {
            return;
        }
        List<String> targetRoles = MCP_BINDING_ROLES.get(trim(agent.agentType()).toLowerCase());
        if (targetRoles == null || targetRoles.isEmpty()) {
            return;
        }
        List<String> normalizedMcpIds = mcpIds.stream()
                .map(this::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        agentManagementService.listFlows(agent.agentId()).stream()
                .filter(flow -> targetRoles.stream().anyMatch(role -> role.equalsIgnoreCase(flow.clientRole())))
                .forEach(flow -> normalizedMcpIds.forEach(mcpId -> aiConfigService.create(ConfigKind.CONFIG, binding(flow.clientId(), mcpId))));
    }

    private void validateModel(String modelId) {
        String normalized = trim(modelId);
        boolean available = aiConfigService.list(ConfigKind.MODEL).stream()
                .anyMatch(model -> normalized.equals(model.configId()) && enabled(model));
        if (!available) {
            throw new AgentManagementException("模型不存在或未启用");
        }
    }

    private void validateMcps(List<String> mcpIds) {
        if (mcpIds == null || mcpIds.isEmpty()) {
            return;
        }
        List<String> availableIds = aiConfigService.list(ConfigKind.MCP).stream()
                .filter(this::enabled)
                .map(AiConfigRecordVO::configId)
                .toList();
        boolean unavailable = mcpIds.stream()
                .map(this::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .anyMatch(mcpId -> !availableIds.contains(mcpId));
        if (unavailable) {
            throw new AgentManagementException("MCP 工具不存在或未启用");
        }
    }

    private boolean enabled(AiConfigRecordVO record) {
        return Integer.valueOf(1).equals(record.status());
    }

    private AiConfigRecordDTO binding(String clientId, String mcpId) {
        return new AiConfigRecordDTO("config_" + clientId + "_mcp_" + mcpId, "", null, clientId, null, mcpId, 1, 0L, "client", "mcp");
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}

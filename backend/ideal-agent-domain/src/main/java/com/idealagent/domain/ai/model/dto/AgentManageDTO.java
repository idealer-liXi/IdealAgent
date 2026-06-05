package com.idealagent.domain.ai.model.dto;

public record AgentManageDTO(
        String agentId,
        String agentName,
        String agentType,
        String agentDesc,
        String modelId,
        String templateId,
        Integer status) {
}

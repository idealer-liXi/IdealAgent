package com.idealagent.domain.ai.model.dto;

public record FlowManageDTO(
        String flowId,
        String agentId,
        String clientId,
        String roleType,
        Integer sortOrder,
        String promptId,
        Integer status) {
}

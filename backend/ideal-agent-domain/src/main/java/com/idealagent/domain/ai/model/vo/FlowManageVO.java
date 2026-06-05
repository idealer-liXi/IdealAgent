package com.idealagent.domain.ai.model.vo;

public record FlowManageVO(
        String flowId,
        String agentId,
        String clientId,
        String roleType,
        Integer sortOrder,
        String promptId,
        String promptContent,
        Integer status) {
}

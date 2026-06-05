package com.idealagent.domain.ai.model.vo;

public record AgentManageVO(
        String agentId,
        String agentName,
        String agentType,
        String agentDesc,
        String modelId,
        String templateId,
        Integer status) {
}

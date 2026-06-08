package com.idealagent.domain.ai.model.dto;

public record FlowManageDTO(
        String originAgentId,
        String originClientId,
        String agentId,
        String clientId,
        String clientRole,
        String userPrompt,
        Integer flowSeq) {
}

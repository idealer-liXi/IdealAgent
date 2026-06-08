package com.idealagent.domain.ai.model.dto;

public record CanvasRelationDTO(
        String sourceType,
        String targetType,
        String sourceId,
        String targetId,
        String agentId,
        String configType,
        String clientRole,
        String userPrompt,
        Integer flowSeq) {
}

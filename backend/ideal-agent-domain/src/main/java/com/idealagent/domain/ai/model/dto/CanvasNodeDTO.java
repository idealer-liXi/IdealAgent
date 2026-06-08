package com.idealagent.domain.ai.model.dto;

import java.util.Map;

public record CanvasNodeDTO(
        String nodeType,
        String id,
        String agentId,
        Map<String, Object> payload) {
}

package com.idealagent.domain.ai.model.dto;

public record WorkRequestDTO(
        String agentId,
        String agentDesc,
        String userMessage,
        String sessionId,
        Integer maxRound,
        Integer maxRetry,
        Integer maxPace) {
}

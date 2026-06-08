package com.idealagent.domain.ai.model.dto;

import java.util.List;

public record ChatRequestDTO(String sessionId, String clientId, String content, String ragTag, List<String> mcpIdList) {
    public ChatRequestDTO(String sessionId, String clientId, String content) {
        this(sessionId, clientId, content, null, List.of());
    }

    public ChatRequestDTO(String sessionId, String clientId, String content, String ragTag) {
        this(sessionId, clientId, content, ragTag, List.of());
    }
}

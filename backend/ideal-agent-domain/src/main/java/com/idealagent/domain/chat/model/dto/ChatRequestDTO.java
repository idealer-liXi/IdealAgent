package com.idealagent.domain.chat.model.dto;

public record ChatRequestDTO(String sessionId, String clientId, String content, String ragTag) {
    public ChatRequestDTO(String sessionId, String clientId, String content) {
        this(sessionId, clientId, content, null);
    }
}

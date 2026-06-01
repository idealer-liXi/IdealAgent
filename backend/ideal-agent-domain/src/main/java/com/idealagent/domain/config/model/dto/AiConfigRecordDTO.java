package com.idealagent.domain.config.model.dto;

public record AiConfigRecordDTO(
        String configId,
        String name,
        String type,
        String content,
        String secret,
        String refId,
        Integer status,
        Long ownerId,
        String ownerType,
        String configType) {

    public AiConfigRecordDTO(String configId, String name, String type, String content, String secret, String refId, Integer status, Long ownerId) {
        this(configId, name, type, content, secret, refId, status, ownerId, null, null);
    }
}

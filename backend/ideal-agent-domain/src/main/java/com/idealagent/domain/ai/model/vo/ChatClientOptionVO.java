package com.idealagent.domain.ai.model.vo;

public record ChatClientOptionVO(
        String clientId,
        String clientName,
        String clientType,
        String modelId,
        String modelName,
        Integer status) {
}

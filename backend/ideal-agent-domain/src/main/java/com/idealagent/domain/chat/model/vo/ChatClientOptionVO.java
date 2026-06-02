package com.idealagent.domain.chat.model.vo;

public record ChatClientOptionVO(
        String clientId,
        String clientName,
        String clientType,
        String modelId,
        String modelName,
        Integer status) {
}

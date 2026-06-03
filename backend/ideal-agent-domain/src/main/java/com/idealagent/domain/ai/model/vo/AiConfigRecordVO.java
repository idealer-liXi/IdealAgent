package com.idealagent.domain.ai.model.vo;

import java.time.LocalDateTime;

public record AiConfigRecordVO(
        String configId,
        String name,
        String type,
        String content,
        String secret,
        String refId,
        Integer status,
        Long ownerId,
        String ownerType,
        String configType,
        LocalDateTime createTime,
        LocalDateTime updateTime) {
}

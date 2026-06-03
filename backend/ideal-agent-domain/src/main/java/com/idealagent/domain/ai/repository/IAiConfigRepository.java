package com.idealagent.domain.ai.repository;

import com.idealagent.domain.ai.model.entity.AiConfigRecord;
import com.idealagent.domain.ai.model.enumeration.ConfigKind;

import java.util.List;

public interface IAiConfigRepository {
    AiConfigRecord save(ConfigKind kind, AiConfigRecord record);

    List<AiConfigRecord> list(ConfigKind kind);

    default AiConfigRecord find(ConfigKind kind, String configId) {
        if (configId == null || configId.isBlank()) {
            return null;
        }
        return list(kind).stream()
                .filter(record -> configId.equals(record.getConfigId()))
                .findFirst()
                .orElse(null);
    }

    AiConfigRecord update(ConfigKind kind, AiConfigRecord record);

    void updateStatus(ConfigKind kind, String configId, Integer status);

    void delete(ConfigKind kind, String configId);
}

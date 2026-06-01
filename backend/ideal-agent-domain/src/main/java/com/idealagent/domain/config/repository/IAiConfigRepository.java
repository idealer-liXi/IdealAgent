package com.idealagent.domain.config.repository;

import com.idealagent.domain.config.model.entity.AiConfigRecord;
import com.idealagent.domain.config.service.ConfigKind;

import java.util.List;

public interface IAiConfigRepository {
    AiConfigRecord save(ConfigKind kind, AiConfigRecord record);

    List<AiConfigRecord> list(ConfigKind kind);
}

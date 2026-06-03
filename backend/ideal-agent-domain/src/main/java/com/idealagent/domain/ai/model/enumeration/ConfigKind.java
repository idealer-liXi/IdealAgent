package com.idealagent.domain.ai.model.enumeration;

import com.idealagent.domain.ai.service.config.AiConfigException;

public enum ConfigKind {
    API,
    MODEL,
    CLIENT,
    PROMPT,
    ADVISOR,
    MCP,
    CONFIG;

    public static ConfigKind from(String value) {
        for (ConfigKind kind : values()) {
            if (kind.name().equalsIgnoreCase(value)) {
                return kind;
            }
        }
        throw new AiConfigException("配置类型不存在");
    }
}
